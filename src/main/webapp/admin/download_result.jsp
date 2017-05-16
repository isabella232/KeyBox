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


    <title>KeyBox - Download &amp; Pull</title>

    <script type="text/javascript">
        $(document).ready(function() {

            $(".submit_btn").button().click(function() {
                $('#pull').submit();
            });

            if ($('.downloadScrollWrapper').height() >= 200) {

                $('.downloadScrollWrapper').addClass('downloadScrollWrapperActive');
                $('.downloadScrollableTable').floatThead({
                    scrollContainer: function ($table) {
                        return $table.closest(".downloadScrollWrapper");
                    }
                });
            }
            $(".downloadScrollableTable tr:even").css("background-color", "#e0e0e0");

            <s:if test="pendingSystemStatus!=null">
            //set scroll
            var container = $('.downloadScrollWrapper'), scrollTo = $('#status_<s:property value="pendingSystemStatus.id"/>');
            container.scrollTop(scrollTo.offset().top - container.offset().top + container.scrollTop() - 55);
            </s:if>
            <s:if test="currentSystemStatus!=null && currentSystemStatus.statusCd=='GENERICFAIL'">
            $("#error_dialog").modal();
            </s:if>
            <s:elseif test="pendingSystemStatus!=null">
            $('#pull').submit();
            </s:elseif>


        });
    </script>
    <style>
        body {
            padding: 10px;
        }
    </style>


</head>
<body style="background: #FFFFFF">

<h4>
    Pulling File to keybox. When you see success, click DOWNLOAD.
</h4>


<s:if test="hostSystemList!= null && !hostSystemList.isEmpty()">
    <div class="downloadScrollWrapper">

    <table class="table-striped downloadScrollableTable" >
        <thead>

        <tr>

            <th>Display Name</th>
            <th>Host</th>
            <th>Status</th>
            <th>Link</th>
        </tr>
        </thead>
        <tbody>

        <s:iterator value="hostSystemList" status="stat">
            <tr>

                <td>
                    <div id="status_<s:property value="id"/>"><s:property
                            value="displayNm"/></div>
                </td>
                <td><s:property value="host"/>:<s:property value="port"/></td>

                <td>
                   <s:if test="statusCd=='INITIAL'">
                    <div class="warning">Transferring</div>
                   </s:if>
                   <s:elseif test="statusCd=='AUTHFAIL'">
                    <div class="warning">Authentication Failed</div>
                   </s:elseif>
                   <s:elseif test="statusCd=='HOSTFAIL'">
                    <div class="error">DNS Lookup Failed</div>
                   </s:elseif>
                   <s:elseif test="statusCd=='KEYAUTHFAIL'">
                    <div class="warning">Passphrase Authentication Failed</div>
                   </s:elseif>
                   <s:elseif test="statusCd=='GENERICFAIL'">
                    <div class="error">Failed</div>
                   </s:elseif>
                   <s:elseif test="statusCd=='SUCCESS'">
                    <div class="success">Success</div>
                   </s:elseif>
                </td>

                <td><a target="_blank" href="/admin/download.action?downloadFileName=<s:property value="downloadFileName"/>_<s:property value="id"/>">DOWNLOAD</a></td>

            </tr>

        </s:iterator>
        </tbody>
    </table>
    </div>
</s:if>
<s:else>
    <p class="error">No systems associated with download</p>
</s:else>

<s:form action="pull" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <s:hidden name="pullFile"/>
</s:form>



<div id="error_dialog" class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">x</button>
                <h4 class="modal-title">System: <s:property value="currentSystemStatus.displayLabel"/></h4>
            </div>
            <div class="modal-body">
                <div class="row">
                    <div class="error">Error: <s:property value="currentSystemStatus.errorMsg"/></div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default submit_btn">OK</button>
            </div>
        </div>
    </div>
</div>

</body>
</html>