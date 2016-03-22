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

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * External authentication utility for Google Auth
 */
public class GoogleAuthUtil {

    private static Logger log = LoggerFactory.getLogger(GoogleAuthUtil.class);

    public static final boolean externalAuthEnabled = true;

    /**
     * external auth login method
     *
     * @param auth contains username and password
     * @return auth token if success
     */
    public static String login(final Auth auth) {

        String authToken = null;
        if (externalAuthEnabled && auth != null && StringUtils.isNotEmpty(auth.getUsername()) && StringUtils.isNotEmpty(auth.getOauthToken())) {

            Connection con = null;
            try {

                Payload payload = GoogleIdToken.parse(new JacksonFactory(), auth.getOauthToken()).getPayload();
                log.info("payload: " + payload);
        
                // Print user identifier
                String userId = payload.getSubject();
                log.info("User ID: " + userId);
        
                // Get profile information from payload
                String email = payload.getEmail();
                boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String locale = (String) payload.get("locale");
                String familyName = (String) payload.get("family_name");
                String givenName = (String) payload.get("given_name");
                String aud = (String) payload.get("aud");
                String sub = (String) payload.get("sub");
                String hd = (String) payload.get("hd");

                // check for matching Google Apps domain
                if(hd == null || !hd.equals(AppConfig.getProperty("googleDomain"))) {
                    return(null);
                }
         
                log.info("email: " + email + " | " + "name: " + name + " | " + "sub: " + sub + " | " + "hd: " + hd);
                // Use or store profile information
                // ...

                con = DBUtils.getConn();
                User user = AuthDB.getUserByUID(con, auth.getUsername());

                if (user == null) {
                    user = new User();

                    user.setUserType(User.ADMINISTRATOR);
                    user.setUsername(auth.getUsername());
                        
                    //if it looks like name is returned default it 
                    user.setFirstNm((String) payload.get("given_name"));
                    user.setLastNm((String) payload.get("family_name"));
                    
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

            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            DBUtils.closeConn(con);
        }

        return authToken;
    }

}
