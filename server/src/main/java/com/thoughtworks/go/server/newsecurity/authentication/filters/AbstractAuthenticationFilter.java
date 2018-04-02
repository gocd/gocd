/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.newsecurity.authentication.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public abstract class AbstractAuthenticationFilter extends OncePerRequestFilter {
    public static final String CURRENT_USER = "CURRENT_USER";
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected boolean hasUser(HttpServletRequest request) {
        return getUser(request) != null;
    }

    protected User getUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(CURRENT_USER);
    }

    protected void setUser(User user, HttpServletRequest request) {
        request.getSession().setAttribute(CURRENT_USER, user);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isSecurityDisabled()) {
            LOGGER.debug(String.format("[Authentication] Security is disabled. Skipping authentication for request %s.", request.getRequestURI()));
            filterChain.doFilter(request, response);
            return;
        }

        if (hasUser(request)) {
            LOGGER.debug("[Authentication] Already authenticated request.");
            filterChain.doFilter(request, response);
            return;
        }

        if (canHandleRequest(request)) {
            LOGGER.debug("[Authentication] Request is not authenticated, attempting authentication.");
            try {
                User user = attemptAuthentication(request, response);
                if (user == null) {
                    LOGGER.debug("[Authentication] Request could not be authenticated.");
                } else {
                    setUser(user, request);
                    LOGGER.debug("[Authentication] Request successfully authenticated.");
                    final HttpSession session = request.getSession();
//                    session.g
                    onAuthenticationSuccess(request, response);
                }
            } catch (Exception e) {
                LOGGER.error("[Authentication] Failed to authenticate request: ", e);
                onAuthenticationFailure(request, response, e);
                return;
            }
        } else {
            LOGGER.debug(String.format("[Authentication] Filter %s cannot handle the request. Proceeding with the next filter in chain.", getClass().getSimpleName()));
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public String getAlreadyFilteredAttributeName() {
        return super.getAlreadyFilteredAttributeName();
    }

    private boolean isSecurityDisabled() {
        return !isSecurityEnabled();
    }

    protected abstract boolean isSecurityEnabled();

    protected abstract void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException;

    protected void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response) {
    }

    protected abstract User attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException;

    protected abstract boolean canHandleRequest(HttpServletRequest request);
}
