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

import com.thoughtworks.go.server.newsecurity.authentication.utils.SessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AbstractAuthenticationFilter extends OncePerRequestFilter {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isSecurityDisabled()) {
            LOGGER.debug("Security is disabled. Skipping authentication for request {}.", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        if (SessionUtils.hasUser(request)) {
            LOGGER.debug("Already authenticated request.");
            filterChain.doFilter(request, response);
            return;
        }

        if (canHandleRequest(request)) {
            LOGGER.debug("Request is not authenticated, attempting authentication.");
            try {
                User user = attemptAuthentication(request, response);
                if (user == null) {
                    LOGGER.debug("Request could not be authenticated.");
                } else {
                    SessionUtils.setUser(user, request);
                    LOGGER.debug("Request successfully authenticated.");
                    onAuthenticationSuccess(request, response);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to authenticate request: ", e);
                onAuthenticationFailure(request, response, e);
                return;
            }
        } else {
            LOGGER.debug("Filter {} cannot handle the request. Proceeding with the next filter in chain.", getClass().getSimpleName());
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

    protected void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    protected abstract User attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException;

    protected abstract boolean canHandleRequest(HttpServletRequest request);
}
