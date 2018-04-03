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

import com.thoughtworks.go.server.newsecurity.authentication.handlers.RedirectToLoginPageHandler;
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
import java.io.IOException;

@Component
public class FormLoginFilter extends AbstractAuthenticationFilter {
    public static final RequestMatcher LOGIN_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/auth/security_check", "POST");
    public final static String USERNAME = "j_username";
    public final static String PASSWORD = "j_password";
    private final SecurityService securityService;
    private final PasswordBasedPluginAuthenticationProvider authenticationProvider;
    private final RedirectToLoginPageHandler accessDeniedHandler;

    @Autowired
    public FormLoginFilter(SecurityService securityService, PasswordBasedPluginAuthenticationProvider authenticationProvider) {
        this.securityService = securityService;
        this.authenticationProvider = authenticationProvider;
        this.accessDeniedHandler = new RedirectToLoginPageHandler();
    }

    @Override
    protected boolean isSecurityEnabled() {
        return securityService.isSecurityEnabled();
    }

    @Override
    protected void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        request.getSession().invalidate();
        request.getSession().setAttribute("GOCD_SECURITY_AUTHENTICATION_ERROR", getMessage(exception));
        accessDeniedHandler.handle(request, response);
        LOGGER.debug("Failed to authenticate user {}", request.getParameter(USERNAME), exception);
    }

    private String getMessage(Exception exception) {
        if (exception instanceof BadCredentialsException) {
            return exception.getMessage();
        } else {
            return "There was an unknown error authenticating you. Please contact the server administrator for help.";
        }
    }

    @Override
    protected void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        super.onAuthenticationSuccess(request, response);
        request.getSession().removeAttribute("GOCD_SECURITY_AUTHENTICATION_ERROR");
        String redirectUrl = (String) request.getSession().getAttribute("REDIRECT_URL");
        if (redirectUrl == null) {
            redirectUrl = "/";
        }

        response.sendRedirect(redirectUrl);
    }

    @Override
    protected User attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        final String username = request.getParameter(USERNAME);
        final String password = request.getParameter(PASSWORD);

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new BadCredentialsException("Username and password must be specified!");
        }

        LOGGER.debug("Requesting authentication for form auth.");
        final User user = authenticationProvider.authenticate(username, password);
        if (user == null) {
            throw new BadCredentialsException("Invalid credentials. Either your username and password are incorrect, or there is a problem with your browser cookies. Please check with your administrator.");
        }
        return user;
    }

    @Override
    protected boolean canHandleRequest(HttpServletRequest request) {
        return LOGIN_PATH_REQUEST_MATCHER.matches(request);
    }
}
