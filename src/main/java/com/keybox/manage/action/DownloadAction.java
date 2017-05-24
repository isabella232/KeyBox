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
package com.keybox.manage.action;

import com.keybox.common.util.AuthUtil;
import com.keybox.manage.db.SystemStatusDB;
import com.keybox.manage.model.HostSystem;
import com.keybox.manage.model.SchSession;
import com.keybox.manage.util.DBUtils;
import com.keybox.manage.util.SSHUtil;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class DownloadAction extends ActionSupport implements ServletRequestAware, ServletResponseAware {

    private static Logger log = LoggerFactory.getLogger(DownloadAction.class);

    String downloadContentType;
    String downloadFileName;
    List<Long> idList = new ArrayList<Long>();
    String pullFile = "~/full_path_to_file";
    List<HostSystem> hostSystemList;
    HostSystem pendingSystemStatus;
    HostSystem currentSystemStatus;
    HttpServletRequest servletRequest;
    HttpServletResponse servletResponse;
    FileInputStream fileInputStream = null;

    @Action(value = "/admin/setDownload",
            results = {
                    @Result(name = "success", location = "/admin/download.jsp")
            }
    )
    public String setDownload() throws Exception {
        Long userId= AuthUtil.getUserId(servletRequest.getSession());

        SystemStatusDB.setInitialSystemStatus(idList, userId, AuthUtil.getUserType(servletRequest.getSession()));
        return SUCCESS;

    }


    @Action(value = "/admin/pull",
            results = {
                    @Result(name = "input", location = "/admin/download.jsp"),
                    @Result(name = "success", location = "/admin/download_result.jsp"),
                    @Result(name = "error", location = "/admin/struts_error.jsp")
            }
    )
    public String pull() {

        Long userId=AuthUtil.getUserId(servletRequest.getSession());
        Long sessionId=AuthUtil.getSessionId(servletRequest.getSession());
        try {

            //get next pending system
            pendingSystemStatus = SystemStatusDB.getNextPendingSystem(userId);
            if (pendingSystemStatus != null) {
                //get session for system
                SchSession session = null;
                for(Integer instanceId : SecureShellAction.getUserSchSessionMap().get(sessionId).getSchSessionMap().keySet()) {

                    //if host system id matches pending system then download
                    if(pendingSystemStatus.getId().equals(SecureShellAction.getUserSchSessionMap().get(sessionId).getSchSessionMap().get(instanceId).getHostSystem().getId())){
                        session = SecureShellAction.getUserSchSessionMap().get(sessionId).getSchSessionMap().get(instanceId);
                    }
                }

                if(session!=null) {

                    // clean filename paths out
                    downloadFileName = cleanFilenameForDownloadFile(pullFile);

                    //pull download from system, saving with systemId and userId to make it unique for that system/user
                    currentSystemStatus = SSHUtil.pullDownload(pendingSystemStatus, session.getSession(), pullFile,
                            buildFilePath(cleanDownloadFilename(pullFile, pendingSystemStatus.getId()), userId));

                    //update system status
                    SystemStatusDB.updateSystemStatus(currentSystemStatus, userId);

                    pendingSystemStatus = SystemStatusDB.getNextPendingSystem(userId);
                }

            }
            hostSystemList = SystemStatusDB.getAllSystemStatus(userId);


        } catch (Exception e) {
            log.error(e.toString(), e);
            addActionError("Exception occurred during pull to keybox: " + e.getMessage());
            return ERROR;
        }

        return SUCCESS;
    }

    @Action(value = "/admin/download",
            results = {
                    @Result(name = "success",
                            type = "stream",
                            params = { "contentType", "application/octet-stream",
                                    "inputName", "fileInputStream",
                                    "contentDisposition", "attachment;filename=${downloadFileName}" }
                    ),
                    @Result(name = "error", location = "/admin/struts_error.jsp")
            }
    )
    public String download() {

        // https://www.mkyong.com/struts/struts-download-file-from-website-example/
        // http://stackoverflow.com/questions/13973264/download-file-in-struts-2-using-annotation
        Long userId = AuthUtil.getUserId(servletRequest.getSession());
        try {

            // build the file path based on the user downloading
            String filePath = buildFilePath(downloadFileName, userId);

            File fileToDownload = new File(filePath);
            if (!fileToDownload.isFile()) {
                // file requested doesn't exist
                addActionError("File (" + downloadFileName + ") does not exist");
                return ERROR;
            }

            fileInputStream = new FileInputStream(fileToDownload);
        } catch (Exception e) {
            log.error(e.toString(), e);
            addActionError("Exception occurred during download: " + e.getMessage());
            return ERROR;
        }

        return SUCCESS;

    }

    /**
     * Validates all fields for downloading a file
     */
    public void validateDownload() {

        if (pullFile == null || pullFile.trim().equals("")) {
            addFieldError("pullFile", "Required");

        }

    }

    public String getDownloadContentType() {
        return downloadContentType;
    }

    public void setDownloadContentType(String downloadContentType) {
        this.downloadContentType = downloadContentType;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public void setDownloadFileName(String downloadFileName) {
        this.downloadFileName = downloadFileName;
    }

    public String getPullFile() {
        return pullFile;
    }

    public void setPullFile(String pullDir) {
        this.pullFile = pullDir;
    }

    public List<Long> getIdList() {
        return idList;
    }

    public void setIdList(List<Long> idList) {
        this.idList = idList;
    }

    public List<HostSystem> getHostSystemList() {
        return hostSystemList;
    }

    public void setHostSystemList(List<HostSystem> hostSystemList) {
        this.hostSystemList = hostSystemList;
    }



    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    public void setServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    public HostSystem getPendingSystemStatus() {
        return pendingSystemStatus;
    }

    public void setPendingSystemStatus(HostSystem pendingSystemStatus) {
        this.pendingSystemStatus = pendingSystemStatus;
    }

    public HostSystem getCurrentSystemStatus() {
        return currentSystemStatus;
    }

    public void setCurrentSystemStatus(HostSystem currentSystemStatus) {
        this.currentSystemStatus = currentSystemStatus;
    }

    private static String cleanFilenameForDownloadFile(String filePath) {
        return filePath.replaceAll("[^A-Za-z0-9]", "");
    }

    // builds up the full download path, for the given user
    private static String buildFilePath(String filenameWithSystem, long userId) {
        return SSHUtil.DOWNLOAD_PATH + "/" + filenameWithSystem + "_" + userId;
    }

    private static String cleanDownloadFilename(String pullPath, long systemId) {
        return cleanFilenameForDownloadFile(pullPath) + "_" + systemId;
    }

    public FileInputStream getFileInputStream() {
        return fileInputStream;
    }

    public void setFileInputStream(FileInputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }
}
