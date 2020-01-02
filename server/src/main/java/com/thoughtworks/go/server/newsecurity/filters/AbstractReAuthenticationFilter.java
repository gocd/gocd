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

import com.thoughtworks.go.server.newsecurity.models.*;
import com.thoughtworks.go.server.newsecurity.providers.AnonymousAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AbstractReAuthenticationFilter extends OncePerRequestFilter {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final SecurityService securityService;
    protected final PasswordBasedPluginAuthenticationProvider passwordBasedPluginAuthenticationProvider;
    protected final WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider;
    protected final AnonymousAuthenticationProvider anonymousAuthenticationProvider;
    protected final SystemEnvironment systemEnvironment;
    protected final Clock clock;

    public AbstractReAuthenticationFilter(SecurityService securityService,
                                          SystemEnvironment systemEnvironment,
                                          Clock clock,
                                          PasswordBasedPluginAuthenticationProvider passwordBasedPluginAuthenticationProvider,
                                          WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider,
                                          AnonymousAuthenticationProvider anonymousAuthenticationProvider) {
        this.securityService = securityService;
        this.passwordBasedPluginAuthenticationProvider = passwordBasedPluginAuthenticationProvider;
        this.webBasedPluginAuthenticationProvider = webBasedPluginAuthenticationProvider;
        this.systemEnvironment = systemEnvironment;
        this.clock = clock;
        this.anonymousAuthenticationProvider = anonymousAuthenticationProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        if (!securityService.isSecurityEnabled()) {
            LOGGER.debug("Skipping filter, security is disabled.");
            filterChain.doFilter(request, response);
            return;
        }

        final AuthenticationToken<?> authenticationToken = SessionUtils.getAuthenticationToken(request);

        if (authenticationToken == null || authenticationToken.isAuthenticated(clock, systemEnvironment)) {
            LOGGER.debug("Continuing chain because authentication token is authenticated or null.");
            filterChain.doFilter(request, response);
        } else {
            performReauthentication(request, response, filterChain);
        }
    }

    private void performReauthentication(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain) throws IOException, ServletException {
        synchronized (request.getSession(false).getId().intern()) {
            if (SessionUtils.isAuthenticated(request, clock, systemEnvironment)) {
                LOGGER.debug("Continuing chain because user is authenticated.");
                filterChain.doFilter(request, response);
            } else {
                LOGGER.debug("Attempting reauthentication.");
                final AuthenticationToken reauthenticatedToken = attemptAuthentication(request, response);

                if (reauthenticatedToken == null) {
                    LOGGER.debug("Reauthentication failed.");
                    onAuthenticationFailure(request, response, "Unable to re-authenticate user after timeout.");
                } else {
                    SessionUtils.setAuthenticationTokenWithoutRecreatingSession(reauthenticatedToken, request);
                    filterChain.doFilter(request, response);
                }
            }
        }
    }

    protected abstract void onAuthenticationFailure(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    String errorMessage) throws IOException;

    private AuthenticationToken attemptAuthentication(HttpServletRequest request,
                                                      HttpServletResponse response) throws AuthenticationException {
        AuthenticationToken<? extends Credentials> authenticationToken = SessionUtils.getAuthenticationToken(request);

        if (authenticationToken == null) {
            LOGGER.debug("Existing authentication token not present!");
            return null;
        }

        try {
            final Credentials credentials = authenticationToken.getCredentials();
            if (credentials instanceof UsernamePassword) {
                return passwordBasedPluginAuthenticationProvider.reauthenticate((AuthenticationToken<UsernamePassword>) authenticationToken);
            } else if (credentials instanceof AccessToken) {
                return webBasedPluginAuthenticationProvider.reauthenticate((AuthenticationToken<AccessToken>) authenticationToken);
            } else if (authenticationToken.isAnonymousToken()) {
                return anonymousAuthenticationProvider.reauthenticate((AuthenticationToken<AnonymousCredential>) authenticationToken);
            } else {
                return null;
            }
        } catch (AuthenticationException e) {
            LOGGER.debug("Failed to authenticate user.", e);
            return null;
        }
    }
}