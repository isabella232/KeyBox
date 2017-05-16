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

    <script type="text/javascript">
        $(document).ready(function() {


            $("#download_btn").button().click(function() {
                $(this).replaceWith('<img src="../img/loading.gif" height="72" width="110"/>');
                $('#pull').submit();
            });
        });

    </script>
    <style>
        body {
            padding: 10px;
        }
    </style>

    <title>KeyBox - Download File</title>
</head>
<body style="background: #FFFFFF">

<s:if test="idList!= null && !idList.isEmpty()">
<s:form action="pull" method="post" enctype="multipart/form-data">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <s:textfield name="pullFile" label="Full path of file to download from host"/>
    <tr>
        <td>&nbsp;</td>
        <td>
            <div id="download_btn" class="btn btn-default download">Download</div>
        </td>
    </tr>
</s:form>
</s:if>
<s:else>
    <p class="error">No systems associated with download</p>
</s:else>

</body>
</html>