/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.newsecurity.handlers.renderer.ContentTypeNegotiationMessageRenderer;
import com.thoughtworks.go.server.newsecurity.models.AuthTokenCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.ContentTypeAwareResponse;
import com.thoughtworks.go.server.newsecurity.providers.AuthTokenBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.AuthTokenService;
import com.thoughtworks.go.server.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Component
public class AuthTokenAuthenticationFilter extends OncePerRequestFilter {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final String BAD_CREDENTIALS_MSG = "Invalid auth token credential";
    protected final SecurityService securityService;
    private final AuthTokenBasedPluginAuthenticationProvider authenticationProvider;
    private AuthTokenService authTokenService;

    @Autowired
    public AuthTokenAuthenticationFilter(SecurityService securityService, AuthTokenService authTokenService, AuthTokenBasedPluginAuthenticationProvider authenticationProvider) {
        this.securityService = securityService;
        this.authenticationProvider = authenticationProvider;
        this.authTokenService = authTokenService;
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

            final AuthTokenCredential credential = extractAuthTokenCredential(request.getHeader("Authorization"));

            if (credential != null) {
                LOGGER.debug("[Bearer Authentication] Authorization header found for user '{}'", credential.getUsername());
            }

            LOGGER.debug("Security enabled: " + securityService.isSecurityEnabled());
            if (securityService.isSecurityEnabled()) {
                filterWhenSecurityEnabled(request, response, filterChain, credential);
            } else {
                filterWhenSecurityDisabled(request, response, filterChain, credential);
            }
        } catch (AuthenticationException e) {
            onAuthenticationFailure(request, response, e.getMessage());
        }
    }

    private AuthTokenCredential extractAuthTokenCredential(String authorizationHeader) {
        final Pattern BEARER_AUTH_EXTRACTOR_PATTERN = Pattern.compile("bearer (.*)", Pattern.CASE_INSENSITIVE);

        if (isBlank(authorizationHeader)) {
            return null;
        }

        final Matcher matcher = BEARER_AUTH_EXTRACTOR_PATTERN.matcher(authorizationHeader);
        if (matcher.matches()) {
            String token = matcher.group(1);
            String user = authTokenService.getUsernameFromToken(token);
            return new AuthTokenCredential(user);
        }

        return null;
    }

    private boolean isPreviouslyAuthenticated(HttpServletRequest request) {
        final AuthenticationToken<?> existingToken = SessionUtils.getAuthenticationToken(request);
        return existingToken != null && existingToken.getCredentials() instanceof AuthTokenCredential;
    }

    private void filterWhenSecurityEnabled(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain,
                                           AuthTokenCredential authToken) throws IOException, ServletException {
        if (authToken == null) {
            LOGGER.debug("Bearer auth credentials are not provided in request.");
            filterChain.doFilter(request, response);
        } else {
            LOGGER.debug("authenticating user {} using bearer token", authToken.getUsername());
            try {
                final AuthenticationToken<AuthTokenCredential> authenticationToken = authenticationProvider.authenticate(authToken, null);

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
                                            AuthTokenCredential authTokenCredential) throws IOException, ServletException {
        if (authTokenCredential == null) {
            filterChain.doFilter(request, response);
        } else {
            onAuthenticationFailure(request, response, "Bearer authentication credentials are not required, since security has been disabled on this server.");
        }
    }

    private void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(SC_UNAUTHORIZED);
        ContentTypeAwareResponse contentTypeAwareResponse = new ContentTypeNegotiationMessageRenderer().getResponse(request);
        response.setCharacterEncoding("utf-8");
        response.setContentType(contentTypeAwareResponse.getContentType().toString());
        response.getOutputStream().print(contentTypeAwareResponse.getFormattedMessage(errorMessage));
    }
}
