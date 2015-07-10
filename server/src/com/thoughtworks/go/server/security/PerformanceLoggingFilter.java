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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.server.perf.WebRequestPerformanceLogger;
import com.thoughtworks.go.server.util.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class PerformanceLoggingFilter implements Filter {
    private static final Log LOGGER = LogFactory.getLog(PerformanceLoggingFilter.class);
    private final boolean usingJetty9;
    private final boolean logRequestTimings;
    private WebRequestPerformanceLogger webRequestPerformanceLogger;

    public PerformanceLoggingFilter(WebRequestPerformanceLogger webRequestPerformanceLogger) {
        this.webRequestPerformanceLogger = webRequestPerformanceLogger;
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        logRequestTimings = systemEnvironment.getEnableRequestTimeLogging();
        usingJetty9 = systemEnvironment.usingJetty9();
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            if (webRequestPerformanceLogger.isLoggingTurnedOn()) {
                long endTime = System.currentTimeMillis();
                String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
                String requestor = servletRequest.getRemoteAddr();

                com.thoughtworks.go.server.util.ServletResponse response = ServletHelper.getInstance().getResponse(servletResponse);
                int status = response.getStatus();
                long contentCount = response.getContentCount();

                webRequestPerformanceLogger.logRequest(requestURI, requestor, status, contentCount, startTime, endTime);
            }
        }
    }


    public void destroy() {
    }
}