<%
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
%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="../_res/inc/header.jsp"/>

    <script src="../_res/js/Duo-Web-v2.min.js"></script>
    <script type="text/javascript">

        //break if loaded in frame
        if(top != self) top.location.replace(location);

        $(document).ready(function() {
            Duo.init({
                'host': '<s:property value='duoAPIEndpoint'/>',
                'sig_request': '<s:property value='signedRequest'/>',
                'post_action': '/admin/duoSubmit.action',
                'post_argument': 'signedResponse'
            });
        });

    </script>
    <title>KeyBox - Login </title>
</head>
<body>
    <div class="navbar navbar-default navbar-fixed-top" role="navigation">
        <div class="container" >

            <div class="navbar-header">
                <div class="navbar-brand" >
                    <div class="nav-img"><img src="<%= request.getContextPath() %>/img/keybox_40x40.png" alt="keybox"/></div>
                    <a href="<%= request.getContextPath() %>/admin/menu.action">KeyBox</a>
                </div>
            </div>
            <!--/.nav-collapse -->
        </div>
    </div>

    <div class="container">
        <p>
            <style>
                #duo_iframe {
                    width: 100%;
                    min-width: 304px;
                    max-width: 620px;
                    height: 330px;
                    border: none;
                }
            </style>
            <iframe id="duo_iframe"></iframe>
        </p>
        <s:form method="POST" id="duo_form">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        </s:form>
    </div>
</body>
</html>
