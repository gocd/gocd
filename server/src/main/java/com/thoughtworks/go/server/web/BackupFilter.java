/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @understands redirecting all requests to a service unavailable page when the server is being backed up.
 */
@Component
public class BackupFilter extends OncePerRequestFilter {
    private final static Logger LOGGER = LoggerFactory.getLogger(BackupFilter.class);

    public static final String JSON = "json";
    public static final String XML = "xml";

    private final BackupService backupService;

    @Autowired
    public BackupFilter(BackupService backupService) {
        this.backupService = backupService;
    }

    private static final OrRequestMatcher REQUESTS_ALLOWED_WHILE_BACKUP_RUNNING_MATCHER = new OrRequestMatcher(
            new RegexRequestMatcher("/api/backups/(\\d+|running)", "GET", true),
            new RegexRequestMatcher("/api/v1/health", "GET", true),
            new RegexRequestMatcher("/admin/backup", "GET", true),
            new RegexRequestMatcher("/assets/.*", "GET", true)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String url = request.getRequestURI();
        if (isBackupFinishJsonUrl(url)) {
            response.setHeader("Cache-Control", "private, max-age=0, no-cache");
            response.setDateHeader("Expires", 0);
            generateResponseForIsBackupFinishedAPI(response);
            return;
        }
        if (backupService.isBackingUp() && !isWhitelisted(request)) {
            String json = "Server is under maintenance mode, please try later.";
            String htmlResponse = generateHTMLResponse();
            new ServerUnavailabilityResponse(request, response, json, htmlResponse).render();
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        return REQUESTS_ALLOWED_WHILE_BACKUP_RUNNING_MATCHER.matches(request);
    }

    private String generateHTMLResponse() throws IOException {
        String path = "backup_in_progress.html";
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(path)) {
            String content = IOUtils.toString(resourceAsStream, UTF_8);
            return replaceStringLiterals(content);
        }
    }

    String replaceStringLiterals(String content) {
        content = content.replaceAll("%backup_initiated_by%", HtmlUtils.htmlEscape(backupService.backupRunningSinceISO8601().orElse("")));
        content = content.replaceAll("%backup_started_by%", HtmlUtils.htmlEscape(backupService.backupStartedBy().orElse("")));
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
