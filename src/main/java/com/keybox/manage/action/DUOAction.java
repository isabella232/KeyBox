/**
 * Copyright 2014 Sean Kavanagh - sean.p.kavanagh6@gmail.com
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
package com.keybox.manage.action;

import com.duosecurity.duoweb.DuoWeb;
import com.keybox.common.util.AppConfig;
import com.keybox.manage.model.User;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DUOAction extends ActionSupport implements ServletRequestAware, ServletResponseAware {

    private static Logger log = LoggerFactory.getLogger(DUOAction.class);

    HttpServletRequest servletRequest;
    HttpServletResponse servletResponse;

    String signedRequest;
    String signedResponse;

    String duoAPIEndpoint;

    @Action(value = "/admin/duo",
            results = {
                    @Result(name = "success", location = "/admin/duo.jsp"),
                    @Result(name = "error", location = "/login.action", type = "redirect")
            }
    )
    public String duo() {

        SecurityContextImpl sci = (SecurityContextImpl) servletRequest.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        if (sci != null) {
            User userDetails = (User) sci.getAuthentication().getDetails();

            String duoIKey = AppConfig.getProperty("duoIKey");
            String duoSKey = AppConfig.getProperty("duoSKey");
            String duoAKey = AppConfig.getProperty("duoAKey");

            signedRequest = DuoWeb.signRequest(duoIKey, duoSKey, duoAKey, userDetails.getUsername());

            duoAPIEndpoint = AppConfig.getProperty("duoAPIEndpoint");
        }
        else {
            return "error";
        }
        
        return SUCCESS;

    }

    @Action(value = "/admin/duoSubmit",
            results = {
                    @Result(name = "success", location = "/admin/menu.action", type = "redirect"),
                    @Result(name = "failed", location = "/login.action", type = "redirect")
            }
    )
    public String duoSubmit() {

        if (signedResponse == null) {
            return "failed";
        }

        String duoIKey = AppConfig.getProperty("duoIKey");
        String duoSKey = AppConfig.getProperty("duoSKey");
        String duoAKey = AppConfig.getProperty("duoAKey");

        String username;
        try {
            username = DuoWeb.verifyResponse(duoIKey, duoSKey, duoAKey, signedResponse);
            if (username.isEmpty()) {
                return "failed";
            }

            // set a value in the session to indicate that they passed duo verification
            SecurityContextImpl sci = (SecurityContextImpl) servletRequest.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
            if (sci != null) {
                User user = (User) sci.getAuthentication().getDetails();
                user.setDuoAuthenticated(true);
            }
            else {
                log.error("Failed because couldn't find user in the session");
                return "failed";
            }
        } catch (Exception ex) {
            log.error("Failed because of: " + ex.getMessage());
            return "failed";
        }

        return SUCCESS;

    }

    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    public void setServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    public String getSignedRequest() {
        return signedRequest;
    }

    public void setSignedRequest(String signedRequest) {
        this.signedRequest = signedRequest;
    }

    public String getSignedResponse() {
        return signedResponse;
    }

    public void setSignedResponse(String signedResponse) {
        this.signedResponse = signedResponse;
    }

    public String getDuoAPIEndpoint() {
        return duoAPIEndpoint;
    }

    public void setDuoAPIEndpoint(String duoAPIEndpoint) {
        this.duoAPIEndpoint = duoAPIEndpoint;
    }
}
