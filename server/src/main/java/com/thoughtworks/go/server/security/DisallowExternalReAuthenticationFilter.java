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

import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.SpringSecurityFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class DisallowExternalReAuthenticationFilter extends SpringSecurityFilter {
    private final Pattern LOGIN_REQUEST_PATTERN = Pattern.compile("^/go/auth/login$");
    private final Pattern AUTHENTICATION_REQUEST_PATTERN = Pattern.compile("^/go/auth/security_check");
    private final Pattern WEB_BASED_AUTH_LOGIN_REQUEST_PATTERN = Pattern.compile("^/go/plugin/([\\w\\-.]+)/login$");
    private final Pattern WEB_BASED_AUTH_AUTHENTICATION_REQUEST_PATTERN = Pattern.compile("^/go/plugin/([\\w\\-.]+)/authenticate$");
    private final String DEFAULT_TARGET_URL = "/";

    @Override
    protected void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(isAuthenticationRequest(request) && isUserAuthenticated()) {
            response.sendRedirect(DEFAULT_TARGET_URL);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAuthenticationRequest(HttpServletRequest request) {
        return WEB_BASED_AUTH_LOGIN_REQUEST_PATTERN.matcher(request.getRequestURI()).matches() ||
                WEB_BASED_AUTH_AUTHENTICATION_REQUEST_PATTERN.matcher(request.getRequestURI()).matches() ||
                LOGIN_REQUEST_PATTERN.matcher(request.getRequestURI()).matches() ||
                AUTHENTICATION_REQUEST_PATTERN.matcher(request.getRequestURI()).matches();
    }

    private boolean isUserAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
    }

    @Override
    public int getOrder() {
        return FilterChainOrder.LOGOUT_FILTER + 1;
    }
}
