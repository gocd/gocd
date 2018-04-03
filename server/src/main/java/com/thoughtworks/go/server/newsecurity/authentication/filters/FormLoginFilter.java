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

import com.thoughtworks.go.server.newsecurity.authentication.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.service.SecurityService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class FormLoginFilter extends AbstractAuthenticationFilter {
    public static final RequestMatcher LOGIN_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/auth/security_check", "POST");
    public final static String USERNAME = "j_username";
    public final static String PASSWORD = "j_password";
    private final SecurityService securityService;
    private final PasswordBasedPluginAuthenticationProvider authenticationProvider;

    @Autowired
    public FormLoginFilter(SecurityService securityService, PasswordBasedPluginAuthenticationProvider authenticationProvider) {
        this.securityService = securityService;
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    protected boolean isSecurityEnabled() {
        return securityService.isSecurityEnabled();
    }

    @Override
    protected void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        request.getSession().invalidate();
        if (exception instanceof BadCredentialsException) {
            request.getSession().setAttribute("AUTHENTICATION_ERROR", exception.getMessage());
        } else {
            request.getSession().setAttribute("AUTHENTICATION_ERROR", "There was an unknown error authenticating you. Please contact the server administrator for help.");
        }
        LOGGER.debug(String.format("[Basic Authentication Failure] Failed to authenticate user %s", request.getParameter(USERNAME)));
    }

    @Override
    protected void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response) {
        super.onAuthenticationSuccess(request, response);
        request.getSession().removeAttribute("AUTHENTICATION_ERROR");
    }

    @Override
    protected User attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        final String username = request.getParameter(USERNAME);
        final String password = request.getParameter(PASSWORD);

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new BadCredentialsException("[Form Authentication] Username and password must be specified!");
        }

        LOGGER.debug("[Basic Authentication] Requesting authentication for form auth.");
        return authenticationProvider.authenticate(username, password);
    }

    @Override
    protected boolean canHandleRequest(HttpServletRequest request) {
        return LOGIN_PATH_REQUEST_MATCHER.matches(request);
    }
}
