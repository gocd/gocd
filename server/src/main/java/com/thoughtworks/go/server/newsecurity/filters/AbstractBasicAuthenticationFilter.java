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

import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.BasicAuthHeaderExtractor;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.thoughtworks.go.server.newsecurity.controllers.AuthenticationController.BAD_CREDENTIALS_MSG;

public abstract class AbstractBasicAuthenticationFilter extends OncePerRequestFilter {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final SecurityService securityService;
    private final PasswordBasedPluginAuthenticationProvider authenticationProvider;

    protected AbstractBasicAuthenticationFilter(SecurityService securityService,
                                                PasswordBasedPluginAuthenticationProvider authenticationProvider) {

        this.securityService = securityService;
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        try {
            if (isPreviouslyAuthenticated(request)) {
                LOGGER.debug("Request is already authenticated.");
                filterChain.doFilter(request, response);
                return;
            }

            final UsernamePassword credential = BasicAuthHeaderExtractor.extractBasicAuthenticationCredentials(request.getHeader("Authorization"));
            if (credential != null) {
                LOGGER.debug("[Basic Authentication] Authorization header found for user '{}'", credential.getUsername());
            }

            if (securityService.isSecurityEnabled()) {
                LOGGER.debug("Security is enabled.");
                filterWhenSecurityEnabled(request, response, filterChain, credential);
            } else {
                LOGGER.debug("Security is disabled.");
                filterWhenSecurityDisabled(request, response, filterChain, credential);
            }
        } catch (AuthenticationException e) {
            onAuthenticationFailure(request, response, e.getMessage());
        }
    }

    private boolean isPreviouslyAuthenticated(HttpServletRequest request) {
        final AuthenticationToken<?> existingToken = SessionUtils.getAuthenticationToken(request);
        return existingToken != null && existingToken.isUsernamePasswordToken();
    }

    private void filterWhenSecurityEnabled(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain,
                                           UsernamePassword usernamePassword) throws IOException, ServletException {
        if (usernamePassword == null) {
            LOGGER.debug("Basic auth credentials are not provided in request.");
            filterChain.doFilter(request, response);
        } else {
            LOGGER.debug("authenticating user {} using basic auth credentials", usernamePassword.getUsername());
            try {
                final AuthenticationToken<UsernamePassword> authenticationToken = authenticationProvider.authenticate(usernamePassword, null);

                if (authenticationToken == null) {
                    onAuthenticationFailure(request, response, BAD_CREDENTIALS_MSG);
                } else {
                    SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
                    filterChain.doFilter(request, response);
                }
            } catch (AuthenticationException e) {
                LOGGER.debug("Failed to authenticate user.", e);
                onAuthenticationFailure(request, response, e.getMessage());
            }
        }
    }

    private void filterWhenSecurityDisabled(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain,
                                            UsernamePassword usernamePassword) throws IOException, ServletException {
        if (usernamePassword == null) {
            filterChain.doFilter(request, response);
        } else {
            onAuthenticationFailure(request, response, "Basic authentication credentials are not required, since security has been disabled on this server.");
        }
    }

    protected abstract void onAuthenticationFailure(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    String errorMessage) throws IOException;

}
