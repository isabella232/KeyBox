/**
 * Copyright 2015 Sean Kavanagh - sean.p.kavanagh6@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.keybox.manage.util;


import com.keybox.common.util.AppConfig;
import com.keybox.manage.db.AuthDB;
import com.keybox.manage.db.UserDB;
import com.keybox.manage.db.ProfileDB;
import com.keybox.manage.db.UserProfileDB;
import com.keybox.manage.model.Profile;
import com.keybox.manage.model.Auth;
import com.keybox.manage.model.User;
import org.apache.commons.lang3.StringUtils;

import java.security.Principal;
import java.sql.Connection;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.ServiceUnavailableException;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;

/**
 * External authentication utility for Azure
 */
public class AzureAuthUtil {

    private static Logger log = LoggerFactory.getLogger(AzureAuthUtil.class);

    public static final boolean externalAuthEnabled = true;

    private final static String AUTHORITY = AppConfig.getProperty("azureAuthority");
    private final static String TENANT =  AppConfig.getProperty("azureTennant");
    private final static String CLIENT_ID =  AppConfig.getProperty("azureClientId");
    
    /**
     * external auth login method
     *
     * @param auth contains username and password
     * @return auth token if success
     */
    public static String login(final Auth auth) {

        String authToken = null;
        if (externalAuthEnabled && auth != null && StringUtils.isNotEmpty(auth.getUsername()) && StringUtils.isNotEmpty(auth.getPassword())) {

            Connection con = null;
            try {
                try {
                    //will throw exception if login fail
                    AuthenticationResult result = getAccessTokenFromUserCredentials(
                            auth.getUsername(), auth.getPassword());

                    log.info("USERINFO " + result.getUserInfo().getUniqueId() + " " + result.getUserInfo().getDisplayableId() + " " + result.getUserInfo().getGivenName() + " " + result.getUserInfo().getFamilyName());
                    

                    con = DBUtils.getConn();
                    User user = AuthDB.getUserByUID(con, auth.getUsername());

                    if (user == null) {
                        user = new User();

                        user.setUserType(User.ADMINISTRATOR);
                        user.setUsername(auth.getUsername());
                        
                        //if it looks like name is returned default it 
                        user.setFirstNm(result.getUserInfo().getGivenName());
                        user.setLastNm(result.getUserInfo().getFamilyName());
                        
                        //set email
                        if(auth.getUsername().contains("@")){
                            user.setEmail(auth.getUsername());
                        }

                        user.setId(UserDB.insertUser(con, user));
                        Profile profile = new Profile();
                        profile.setNm("private_" + auth.getUsername() + "_" + user.getId());
                        profile.setDesc("private profile for " + auth.getUsername());
                        List<Long> profUserList = new ArrayList<Long>();
                        Long profileId = ProfileDB.insertProfile(profile);
                        profUserList.add(user.getId());
                        UserProfileDB.setUsersForProfile(profileId, profUserList);

                    }

                    authToken = UUID.randomUUID().toString();
                    user.setAuthToken(authToken);
                    user.setAuthType(Auth.AUTH_EXTERNAL);
                    //set auth token
                    AuthDB.updateLogin(con, user);


                } catch (ServiceUnavailableException e) {
                    //auth failed return empty
                    log.error(e.toString(), e);
                    authToken = null;
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            DBUtils.closeConn(con);
        }

        return authToken;
    }

    private static AuthenticationResult getAccessTokenFromUserCredentials(
            String username, String password) throws Exception {
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, false, service);
            Future<AuthenticationResult> future = context.acquireToken(
                    "https://graph.windows.net", CLIENT_ID, username, password,
                    null);
            result = future.get();
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException(
                    "authentication result was null");
        }
        return result;
    }
}
