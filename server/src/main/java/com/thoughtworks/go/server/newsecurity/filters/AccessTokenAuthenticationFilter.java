/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.handlers.renderer.ContentTypeNegotiationMessageRenderer;
import com.thoughtworks.go.server.newsecurity.models.AccessTokenCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.ContentTypeAwareResponse;
import com.thoughtworks.go.server.newsecurity.providers.AccessTokenBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.AccessTokenService;
import com.thoughtworks.go.server.service.SecurityAuthConfigService;
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
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Component
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final Logger ACCESS_TOKEN_LOGGER = LoggerFactory.getLogger(AccessToken.class);
    private static final String BAD_CREDENTIALS_MSG = "Invalid Personal Access Token.";
    protected final SecurityService securityService;
    private SecurityAuthConfigService securityAuthConfigService;
    private final AccessTokenBasedPluginAuthenticationProvider authenticationProvider;
    private AccessTokenService accessTokenService;

    @Autowired
    public AccessTokenAuthenticationFilter(SecurityService securityService,
                                           AccessTokenService accessTokenService,
                                           SecurityAuthConfigService securityAuthConfigService,
                                           AccessTokenBasedPluginAuthenticationProvider authenticationProvider) {
        this.securityService = securityService;
        this.securityAuthConfigService = securityAuthConfigService;
        this.authenticationProvider = authenticationProvider;
        this.accessTokenService = accessTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        if (isPreviouslyAuthenticated(request)) {
            LOGGER.debug("Request is already authenticated.");
            filterChain.doFilter(request, response);
            return;
        }

        AccessTokenCredential credential;
        try {
            credential = extractAuthTokenCredential(request.getHeader("Authorization"));
        } catch (Exception e) {
            onAuthenticationFailure(request, response, e.getMessage());
            return;
        }

        if (credential != null) {
            LOGGER.debug("[Bearer Authentication] Authorization header found for user '{}'", credential.getAccessToken().getUsername());
        }

        LOGGER.debug("Security Enabled: " + securityService.isSecurityEnabled());
        if (securityService.isSecurityEnabled()) {
            filterWhenSecurityEnabled(request, response, filterChain, credential);
        } else {
            filterWhenSecurityDisabled(request, response, filterChain, credential);
        }
    }

    private AccessTokenCredential extractAuthTokenCredential(String authorizationHeader) {
        final Pattern BEARER_AUTH_EXTRACTOR_PATTERN = Pattern.compile("bearer (.*)", Pattern.CASE_INSENSITIVE);

        if (isBlank(authorizationHeader)) {
            return null;
        }

        final Matcher matcher = BEARER_AUTH_EXTRACTOR_PATTERN.matcher(authorizationHeader);
        if (matcher.matches()) {
            String token = matcher.group(1);
            AccessToken accessToken = accessTokenService.findByAccessToken(token);
            return new AccessTokenCredential(accessToken);
        }

        return null;
    }

    private boolean isPreviouslyAuthenticated(HttpServletRequest request) {
        final AuthenticationToken<?> existingToken = SessionUtils.getAuthenticationToken(request);
        return existingToken != null && existingToken.getCredentials() instanceof AccessTokenCredential;
    }

    private void filterWhenSecurityEnabled(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain,
                                           AccessTokenCredential accessTokenCredential) throws IOException, ServletException {
        if (accessTokenCredential == null) {
            LOGGER.debug("Bearer auth credentials are not provided in request.");
            filterChain.doFilter(request, response);
        } else {
            accessTokenService.updateLastUsedCacheWith(accessTokenCredential.getAccessToken());
            ACCESS_TOKEN_LOGGER.debug("[Bearer Token Authentication] Authenticating bearer token for: " +
                            "GoCD User: '{}'. " +
                            "GoCD API endpoint: '{}', " +
                            "API Client: '{}', " +
                            "Is Admin Scoped Token: '{}', " +
                            "Current Time: '{}'."
                    , accessTokenCredential.getAccessToken().getUsername()
                    , request.getRequestURI()
                    , request.getHeader("User-Agent")
                    , securityService.isUserAdmin(new Username(accessTokenCredential.getAccessToken().getUsername()))
                    , new Timestamp(System.currentTimeMillis()));

            try {
                String authConfigId = accessTokenCredential.getAccessToken().getAuthConfigId();
                SecurityAuthConfig authConfig = securityAuthConfigService.findProfile(authConfigId);
                if(authConfig == null) {
                    String errorMessage = String.format("Can not find authorization configuration \"%s\" to which the requested personal access token belongs. Authorization Configuration \"%s\" might have been renamed or deleted. Please revoke the existing token and create a new one for the same.", authConfigId, authConfigId);
                    onAuthenticationFailure(request, response, errorMessage);
                    return;
                }
                final AuthenticationToken<AccessTokenCredential> authenticationToken = authenticationProvider.authenticateUser(accessTokenCredential, authConfig);
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
                                            AccessTokenCredential accessTokenCredential) throws IOException, ServletException {
        if (accessTokenCredential == null) {
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
