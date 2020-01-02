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
package com.thoughtworks.go.server.newsecurity.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/* Set a short long-lived session. */
public abstract class AbstractSessionReduceIdleTimeoutFilter extends OncePerRequestFilter {
    private final int maxInactiveInterval;
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected AbstractSessionReduceIdleTimeoutFilter(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean hadNoSessionBeforeStarting = request.getSession(false) == null;
        try {
            chain.doFilter(request, response);
        } finally {
            HttpSession session = request.getSession(false);
            boolean hasSessionNow = session != null;

            if (hadNoSessionBeforeStarting && hasSessionNow) {
                LOGGER.debug("Setting max inactive interval for request: {} to {}.", request.getRequestURI(), maxInactiveInterval);
                session.setMaxInactiveInterval(maxInactiveInterval);
            }
        }
    }
}
