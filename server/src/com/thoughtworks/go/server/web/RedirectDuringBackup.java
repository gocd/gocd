/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.server.util.ServletRequest;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @understands redirecting all requests to a service unavailable page when the server is being backed up.
 */
public class RedirectDuringBackup {

    static final String BACKUP_IN_PROGRESS = "backupInProgress";
    private static final String REFERER = "Referer";
    private ServletHelper servletHelper;

    public void setServerBackupFlag(HttpServletRequest req) {
        servletHelper = ServletHelper.getServerHelper(new SystemEnvironment().usingJetty9());
        BackupStatusProvider backupStatusProvider = getBackupStatusProvider(req);
        boolean backingUp = backupStatusProvider.isBackingUp();
        req.setAttribute(BACKUP_IN_PROGRESS, String.valueOf(backingUp));

        if (backingUp) {
            req.setAttribute("redirected_from", servletHelper.encodeString(getRedirectUri(req)));
            req.setAttribute("backup_started_at", servletHelper.encodeString(backupStatusProvider.backupRunningSinceISO8601()));
            req.setAttribute("backup_started_by", servletHelper.encodeString(backupStatusProvider.backupStartedBy()));
        }
    }

    private String getRedirectUri(HttpServletRequest req) {
        ServletRequest request = servletHelper.getRequest(req);

        if (isMessagesJson(request) || isMethod(req, "post") || isMethod(req, "put") || isMethod(req, "delete")) {
            return getReferer(req);
        }
        return request.getUriAsString();
    }

    private boolean isMessagesJson(ServletRequest req) {
        return req.getUriPath().equals("/go/server/messages.json");
    }

    private String getReferer(HttpServletRequest req) {
        String referer = req.getHeader(REFERER);
        return referer == null? "" : referer;
    }

    private boolean isMethod(HttpServletRequest req, String method) {
        return req.getMethod().equalsIgnoreCase(method);
    }

    protected BackupStatusProvider getBackupStatusProvider(HttpServletRequest req) {
        return (BackupStatusProvider) WebApplicationContextUtils.getWebApplicationContext(req.getSession().getServletContext()).getBean("backupService");
    }
}
