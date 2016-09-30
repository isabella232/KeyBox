package com.looker.azure.userdetails;

import com.keybox.manage.db.AuthDB;
import com.keybox.manage.db.ProfileDB;
import com.keybox.manage.db.UserDB;
import com.keybox.manage.db.UserProfileDB;
import com.keybox.manage.model.Auth;
import com.keybox.manage.model.Profile;
import com.keybox.manage.util.DBUtils;
import com.looker.azure.util.SAMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

    // Logger
    private static final Logger LOG = LoggerFactory.getLogger(SAMLUserDetailsServiceImpl.class);

    public Object loadUserBySAML(SAMLCredential credential)
            throws UsernameNotFoundException {

        // The method is supposed to identify local account of user referenced by
        // data in the SAML assertion and return UserDetails object describing the user.

        // grab user info from SAML
        String userEmail = SAMLUtil.getAttribute(credential, SAMLUtil.EMAIL_ATTRIBUTE_NAME);
        String userFirstname = SAMLUtil.getAttribute(credential, SAMLUtil.FIRSTNAME_ATTRIBUTE_NAME);
        String userLastname = SAMLUtil.getAttribute(credential, SAMLUtil.LASTNAME_ATTRIBUTE_NAME);

        LOG.info(userEmail + " is logged in");
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        GrantedAuthority authority;

        String authToken;
        Connection con = null;
        try {
            con = DBUtils.getConn();
            com.keybox.manage.model.User user = AuthDB.getUserByUID(con, userEmail);

            // user doesn't exist, create in db
            if (user == null) {
                user = new com.keybox.manage.model.User();

                user.setUserType(com.keybox.manage.model.User.ADMINISTRATOR);
                user.setUsername(userEmail);

                //if it looks like name is returned default it
                user.setFirstNm(userFirstname);
                user.setLastNm(userLastname);
                user.setEmail(userEmail);

                user.setId(UserDB.insertUser(con, user));
                Profile profile = new Profile();
                profile.setNm("private_" + userEmail + "_" + user.getId());
                profile.setDescr("private profile for " + userEmail);
                List<Long> profUserList = new ArrayList<Long>();
                Long profileId = ProfileDB.insertProfile(profile);
                profUserList.add(user.getId());
                UserProfileDB.setUsersForProfile(profileId, profUserList);
            }

            // Add proper authorities
            if (user.getUserType().equals(com.keybox.manage.model.User.ADMINISTRATOR)) {
                // normal admin
                authority = new SimpleGrantedAuthority("ROLE_ADMINISTRATOR");
            } else if (user.getUserType().equals(com.keybox.manage.model.User.MANAGER)) {
                // super admin
                authority = new SimpleGrantedAuthority("ROLE_MANAGER");
            } else {
                // no authorities case (should never happen)
                authority = new SimpleGrantedAuthority("ROLE_NONE");
            }
            authorities.add(authority);

            authToken = UUID.randomUUID().toString();
            user.setAuthToken(authToken);
            user.setAuthType(Auth.AUTH_EXTERNAL);
            //set auth token
            AuthDB.updateLogin(con, user);

        } catch (Exception e) {
            LOG.error(e.toString(), e);
        }

        DBUtils.closeConn(con);

        return new User(userEmail, "<abc123>", true, true, true, true, authorities);
    }

}
