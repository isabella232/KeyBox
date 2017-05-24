/**
 * Copyright 2013 Sean Kavanagh - sean.p.kavanagh6@gmail.com
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
package com.looker.azure.filter;

import com.keybox.common.util.AuthUtil;
import com.keybox.manage.model.Auth;
import com.keybox.manage.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter determines if admin user is authenticated
 */
public class AuthFilter implements Filter {

    private static Logger log = LoggerFactory.getLogger(AuthFilter.class);

    public void init(FilterConfig config) throws ServletException {

    }

    public void destroy() {
    }

    /**
     * doFilter determines if user is an administrator or redirect to login page
     *
     * @param req   task request
     * @param resp  task response
     * @param chain filter chain
     * @throws ServletException
     * @throws IOException
     */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {

        HttpServletRequest servletRequest = (HttpServletRequest) req;
        HttpServletResponse servletResponse = (HttpServletResponse) resp;
        boolean isLoginValid = false;
        boolean isDuoAuthenticated = false;

        try {
            // get the spring security context
            SecurityContextImpl sci = (SecurityContextImpl) servletRequest.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
            if (sci != null) {
                User keyboxUser = (User) sci.getAuthentication().getDetails();

                if (keyboxUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER"))) {
                    isLoginValid = true;
                    AuthUtil.setUserType(servletRequest.getSession(), Auth.MANAGER);
                    AuthUtil.setUserId(servletRequest.getSession(), keyboxUser.getId());
                } else if (servletRequest.getRequestURI().matches("^" + servletRequest.getContextPath().replaceAll("/", "\\\\/") + "\\/admin\\/.*") &&
                        keyboxUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))) {
                    isLoginValid = true;
                    AuthUtil.setUserType(servletRequest.getSession(), Auth.ADMINISTRATOR);
                    AuthUtil.setUserId(servletRequest.getSession(), keyboxUser.getId());
                }

                // check duo authentication
                if (keyboxUser.isDuoAuthenticated()) {
                    isDuoAuthenticated = true;
                }
                else {
                    log.warn("user " + keyboxUser.getUsername() + " was not duo authenticated for request (" + ((HttpServletRequest) req).getRequestURI() + "), sending them back to the duo screen");
                    isDuoAuthenticated = false;
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            isLoginValid = false;
            isDuoAuthenticated = false;
        }

        //if not admin redirect to login page
        if (!isLoginValid) {
            AuthUtil.deleteAllSession(servletRequest.getSession());
            servletResponse.sendRedirect(servletRequest.getContextPath() + "/login.action");
        }
        else if ( (!servletRequest.getServletPath().equals("/admin/duo.action") && !servletRequest.getServletPath().equals("/admin/duoSubmit.action")) && !isDuoAuthenticated) {
            // if we are going anywhere other than duo when we are not duo authenticated, redirect to duo
            servletResponse.sendRedirect(servletRequest.getContextPath() + "/admin/duo.action");
        }
        else{
            chain.doFilter(req, resp);
        }
    }


}
