/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.server.newsecurity.filters.helpers.ServerUnavailabilityResponse;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class ModeAwareFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger("GO_MODE_AWARE_FILTER");
    private final SystemEnvironment systemEnvironment;
    private MaintenanceModeService maintenanceModeService;

    private static final OrRequestMatcher ALLOWED_MAINTENANCE_MODE_REQUEST_MATCHER = new OrRequestMatcher(
            new AntPathRequestMatcher("/remoting/**"),
            new AntPathRequestMatcher("/api/backups", "POST", true),
            new AntPathRequestMatcher("/admin/backup", "POST", true),
            new RegexRequestMatcher("/api/stages/[0-9]*/cancel", "POST", true),
            new RegexRequestMatcher("/api/stages/.*/.*/cancel", "POST", true),
            new AntPathRequestMatcher("/api/admin/maintenance_mode/*")
    );

    @Autowired
    public ModeAwareFilter(SystemEnvironment systemEnvironment, MaintenanceModeService maintenanceModeService) {
        this.systemEnvironment = systemEnvironment;
        this.maintenanceModeService = maintenanceModeService;
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        if (blockBecauseInactive((HttpServletRequest) servletRequest)) {
            LOGGER.warn("Got a non-GET request: {} while server is in inactive state (Secondary)", servletRequest);
            ((HttpServletResponse) servletResponse).sendRedirect(systemEnvironment.getWebappContextPath() + "/errors/inactive");
        } else if (blockBecauseMaintenanceMode((HttpServletRequest) servletRequest)) {
            LOGGER.info("Got a non-GET request: {} while server is in maintenance state", servletRequest);
            String jsonMessage = "Server is in maintenance mode, please try later.";
            String htmlResponse = generateHTMLResponse();
            new ServerUnavailabilityResponse((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, jsonMessage, htmlResponse).render();
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    String generateHTMLResponse() throws IOException {
        String path = "server_in_maintenance_mode.html";
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(path)) {
            return IOUtils.toString(resourceAsStream, UTF_8)
                    .replaceAll("%updatedBy%", maintenanceModeService.updatedBy())
                    .replaceAll("%updatedOn%", maintenanceModeService.updatedOn())
                    .replaceAll("%docsBaseUrl%", CurrentGoCDVersion.getInstance().baseDocsUrl());
        }
    }

    private boolean blockBecauseInactive(HttpServletRequest servletRequest) {
        return (!systemEnvironment.isServerActive() && !isAllowedRequest(servletRequest));
    }

    private boolean blockBecauseMaintenanceMode(HttpServletRequest servletRequest) {
        if (isWhitelistedRequest(servletRequest) || isAllowedRequest(servletRequest)) {
            return false;
        }

        return maintenanceModeService.isMaintenanceMode();
    }

    private boolean isWhitelistedRequest(HttpServletRequest servletRequest) {
        return ALLOWED_MAINTENANCE_MODE_REQUEST_MATCHER.matches(servletRequest);
    }

    private boolean isAllowedRequest(HttpServletRequest servletRequest) {
        if ((systemEnvironment.getWebappContextPath() + "/auth/security_check").equals(servletRequest.getRequestURI()))
            return true;
        if ((systemEnvironment.getWebappContextPath() + "/api/state/active").equals(servletRequest.getRequestURI()))
            return true;


        return isReadOnlyRequest(servletRequest);
    }

    private boolean isReadOnlyRequest(HttpServletRequest servletRequest) {
        return RequestMethod.GET.name().equalsIgnoreCase(servletRequest.getMethod()) ||
                RequestMethod.HEAD.name().equalsIgnoreCase(servletRequest.getMethod());
    }

    @Override
    public void destroy() {
    }
}
