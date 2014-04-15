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

import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

public class IgnoreResolver {
    private static final String[] STATIC_FILE_EXT = new String[]{".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".psd", ".ico"};

    public IgnoreResolver() {
    }

    public boolean shouldIgnore(HttpServletRequest request) {
        try {
            return isGoingToAbout(request) ||
                    isDownloadAgentJar(request) ||
                    isGoingToLogin(request) ||
                    isGoingToServerInfo(request) ||
                    isPost(request) ||
                    isPut(request) ||
                    isStaticFile(request) ||
                    isHelp(request);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isHelp(HttpServletRequest request) throws URISyntaxException {
        String path = new URI(request.getRequestURI()).getPath().toLowerCase();
        return path.startsWith(request.getContextPath() + "/help/");
    }

    private boolean isStaticFile(HttpServletRequest request) throws URISyntaxException {
        String path = new URI(request.getRequestURI()).getPath().toLowerCase();
        for (String ext : STATIC_FILE_EXT) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGoingToServerInfo(HttpServletRequest request) {
        return StringUtils.equals(request.getRequestURI(), request.getContextPath() + "/api/server.xml");
    }

    private boolean isPut(HttpServletRequest request) {
        return isHttpMethod(request, "PUT");
    }

    private boolean isPost(HttpServletRequest request) {
        return isHttpMethod(request, "POST");
    }

    private boolean isDownloadAgentJar(HttpServletRequest request) {
        return StringUtils.equals(request.getRequestURI(), request.getContextPath() + "/admin/agent");
    }

    private boolean isGoingToLogin(HttpServletRequest request) {
        return StringUtils.contains(request.getRequestURI(), request.getContextPath() + "/auth/");
    }

    private boolean isGoingToAbout(HttpServletRequest request) {
        return StringUtils.contains(request.getRequestURI(), request.getContextPath() + "/about");
    }

    private boolean isHttpMethod(HttpServletRequest request, String targetingMethod) {
        String method = (String) request.getAttribute("_method");
        return StringUtils.isNotEmpty(method) ? method.equalsIgnoreCase(targetingMethod) : request.getMethod().equalsIgnoreCase(targetingMethod);
    }
}
