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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.UrlEncoded;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @understands redirecting all requests to a service unavailable page when the server is being backed up.
 */
public class RedirectDuringBackup {

    static final String BACKUP_IN_PROGRESS = "backupInProgress";
    private static final String REFERER = "Referer";

    public void setServerBackupFlag(HttpServletRequest req) {
        BackupStatusProvider backupStatusProvider = getBackupStatusProvider(req);
        boolean backingUp = backupStatusProvider.isBackingUp();
        req.setAttribute(BACKUP_IN_PROGRESS, String.valueOf(backingUp));
        if (backingUp) {
            req.setAttribute("redirected_from", UrlEncoded.encodeString(getRedirectUri((Request) req)));
            req.setAttribute("backup_started_at", UrlEncoded.encodeString(backupStatusProvider.backupRunningSinceISO8601()));
            req.setAttribute("backup_started_by", UrlEncoded.encodeString(backupStatusProvider.backupStartedBy()));
        }
    }

    private String getRedirectUri(Request req) {
        if (isMessagesJson(req) || isMethod(req, "post") || isMethod(req, "put") || isMethod(req, "delete")) {
            return getReferer(req);
        }
        return req.getUri().toString();
    }

    private boolean isMessagesJson(Request req) {
        return req.getUri().getPath().equals("/go/server/messages.json");
    }

    private String getReferer(HttpServletRequest req) {
        String referer = req.getHeader(REFERER);
        return referer == null? "" : referer;
    }

    private boolean isMethod(Request req, String method) {
        return req.getMethod().equalsIgnoreCase(method);
    }

    protected BackupStatusProvider getBackupStatusProvider(HttpServletRequest req) {
        return (BackupStatusProvider) WebApplicationContextUtils.getWebApplicationContext(req.getSession().getServletContext()).getBean("backupService");
    }
}
