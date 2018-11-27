/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.newsecurity.filters.helpers.ServerUnavailabilityResponse;
import com.thoughtworks.go.server.service.BackupService;
import org.apache.commons.io.IOUtils;
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
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

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
    public void init(FilterConfig filterConfig) {
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
        if (backupService.isBackingUp() && !isWhitelisted(url)) {
            String json = "Server is under maintenance mode, please try later.";
            String htmlResponse = generateHTMLResponse();
            new ServerUnavailabilityResponse((HttpServletRequest) request, (HttpServletResponse) response, json, htmlResponse).render();
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isWhitelisted(String url) {
        return url.equals("/go/api/v1/health");
    }

    private String generateHTMLResponse() throws IOException {
        String path = "backup_in_progress.html";
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(path);
        String content = IOUtils.toString(resourceAsStream, UTF_8);
        return replaceStringLiterals(content);
    }

    String replaceStringLiterals(String content) {
        content = content.replaceAll("%backup_initiated_by%", HtmlUtils.htmlEscape(backupService.backupRunningSinceISO8601()));
        content = content.replaceAll("%backup_started_by%", HtmlUtils.htmlEscape(backupService.backupStartedBy()));
        return content;
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
            LOGGER.error("General IOException: {}", e.getMessage());
        }
    }

    @Override
    public void destroy() {

    }

}
