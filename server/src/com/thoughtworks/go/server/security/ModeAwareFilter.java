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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ModeAwareFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger("GO_MODE_AWARE_FILTER");
    private final SystemEnvironment systemEnvironment;

    @Autowired
    public ModeAwareFilter(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (shouldBlockRequest((HttpServletRequest) servletRequest)) {
            LOGGER.warn("Got a non-GET request: {}", servletRequest);
            ((HttpServletResponse) servletResponse).sendRedirect(systemEnvironment.getWebappContextPath() + "/errors/inactive");
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private boolean shouldBlockRequest(HttpServletRequest servletRequest) {
        if (systemEnvironment.isServerActive()) return false;
        if (isReadOnlyRequest(servletRequest)) return false;
        if ((systemEnvironment.getWebappContextPath() + "/auth/security_check").equals(servletRequest.getRequestURI()))
            return false;
        if ((systemEnvironment.getWebappContextPath() + "/api/state/active").equals(servletRequest.getRequestURI()))
            return false;
        return true;
    }

    private boolean isReadOnlyRequest(HttpServletRequest servletRequest) {
        return RequestMethod.GET.name().equalsIgnoreCase(servletRequest.getMethod()) ||
                RequestMethod.HEAD.name().equalsIgnoreCase(servletRequest.getMethod());
    }

    @Override
    public void destroy() {
    }
}
