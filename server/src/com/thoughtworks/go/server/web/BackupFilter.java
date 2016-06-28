/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.web;

import com.google.gson.JsonObject;
import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @understands redirecting all requests to a service unavailable page when the server is being backed up.
 */
public class BackupFilter implements Filter {

    public static final String JSON = "json";
    public static final String XML = "xml";

    @Autowired
    private BackupService backupService;
    private final static Logger LOGGER = LoggerFactory.getLogger(BackupFilter.class);    // For the Test
    public static final Pattern PATTERN = Pattern.compile("^((.*/api/.*)|(.*[^/]+\\.(xml|json)(\\?.*)?))$");

    // For the Test
    protected BackupFilter(BackupService backupService) {
        this.backupService = backupService;
    }

    public BackupFilter() {

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (backupService == null) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        String url = ((HttpServletRequest) request).getRequestURI();
        if (isBackupFinishJsonUrl(url)) {
            ((HttpServletResponse) response).setHeader("Cache-Control", "private, max-age=0, no-cache");
            ((HttpServletResponse) response).setDateHeader("Expires", 0);
            generateResponseForIsBackupFinishedAPI(response);
            return;
        }
        if (backupService.isBackingUp()) {
            ((HttpServletResponse) response).setHeader("Cache-Control", "private, max-age=0, no-cache");
            ((HttpServletResponse) response).setDateHeader("Expires", 0);
            if (isAPIUrl(url) && !isMessagesJson(url)) {
                generateAPIResponse(request, response);
            } else {
                generateHTMLResponse(response);
            }
        } else {
            chain.doFilter(request, response);
        }

    }

    private void generateHTMLResponse(ServletResponse response) {
        String path = "backup_in_progress.html";
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(path);
        response.setContentType("text/html");
        try {
            String content = FileUtil.readToEnd(resourceAsStream);
            content = replaceStringLiterals(content);
            response.getWriter().print(content);
            resourceAsStream.close();
        } catch (IOException e) {
            LOGGER.error(String.format("General IOException: %s", e.getMessage()));
        }
    }

    String replaceStringLiterals(String content) {
        content = content.replaceAll("%backup_initiated_by%", HtmlUtils.htmlEscape(backupService.backupRunningSinceISO8601()));
        content = content.replaceAll("%backup_started_by%", HtmlUtils.htmlEscape(backupService.backupStartedBy()));
        return content;
    }

    private void generateAPIResponse(ServletRequest request, ServletResponse response) {

        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String message = "Server is under maintenance mode, please try later.";

            if (requestIsOfType(JSON, httpRequest)) {
                response.setContentType("application/json");
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("message", message);
                response.getWriter().print(jsonObject);
            } else if (requestIsOfType(XML, httpRequest)) {
                response.setContentType("application/xml");
                String xml = String.format("<message> %s </message>", message);
                response.getWriter().print(xml);
            } else {
                generateHTMLResponse(response);
            }

        } catch (IOException e) {
            LOGGER.error(String.format("General IOException: %s", e.getMessage()));
        }
        ((HttpServletResponse) response).setStatus(503);
    }

    private boolean requestIsOfType(String type, HttpServletRequest request) {
        String header = request.getHeader("Accept");
        String contentType = request.getContentType();
        String url = request.getRequestURI();
        return header != null && header.contains(type) || url != null && url.endsWith(type) || contentType != null && contentType.contains(type);
    }

    private boolean isBackupFinishJsonUrl(String url) {
        return "/go/is_backup_finished.json".equals(url);
    }

    private void generateResponseForIsBackupFinishedAPI(ServletResponse response) {
        response.setContentType("application/json");
        JsonObject json = new JsonObject();
        json.addProperty("is_backing_up", backupService.isBackingUp());
        try {
            response.getWriter().print(json);
        } catch (IOException e) {
            LOGGER.error(String.format("General IOException: %s", e.getMessage()));
        }
    }

    private boolean isAPIUrl(String url) {
        Matcher matcher = PATTERN.matcher(url);
        return matcher.matches();
    }

    private boolean isMessagesJson(String url) {
        return "/go/server/messages.json".equals(url);
    }

    @Override
    public void destroy() {

    }

}
