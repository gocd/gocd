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

package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.OAuthCredentials;
import com.thoughtworks.go.server.newsecurity.providers.OAuthAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
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

import static org.apache.commons.lang.StringUtils.isBlank;

@Component
public class OauthAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION = "Authorization";
    private static final Pattern OAUTH_TOKEN_PATTERN = Pattern.compile("^Token token=\"(.*?)\"$");
    protected final Logger LOGGER = LoggerFactory.getLogger(OauthAuthenticationFilter.class);
    private final SecurityService securityService;
    private final OAuthAuthenticationProvider authenticationProvider;

    @Autowired
    public OauthAuthenticationFilter(SecurityService securityService,
                                     OAuthAuthenticationProvider authenticationProvider) {
        this.securityService = securityService;
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            OAuthCredentials oAuthCredentials = extractOAuthToken(request);

            if (securityService.isSecurityEnabled()) {
                filterWhenSecurityEnabled(request, response, filterChain, oAuthCredentials);
            } else {
                filterWhenSecurityDisabled(request, response, filterChain, oAuthCredentials);
            }
        } catch (AuthenticationException e) {
            onAuthenticationFailure(request, response, e.getMessage());
        }
    }

    private void filterWhenSecurityEnabled(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain,
                                           OAuthCredentials oAuthCredentials) throws IOException, ServletException {
        if (oAuthCredentials == null) {
            filterChain.doFilter(request, response);
        } else {
            final AuthenticationToken<OAuthCredentials> authenticationToken = authenticationProvider.authenticate(oAuthCredentials, null);

            if (authenticationToken == null) {
                onAuthenticationFailure(request, response, "Provided OAuth token is invalid.");
            } else {
                SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
                filterChain.doFilter(request, response);
            }
        }
    }

    void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         String errorMessage) throws IOException {
        response.setStatus(401);
        response.getOutputStream().print(errorMessage);
    }

    private void filterWhenSecurityDisabled(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain,
                                            OAuthCredentials oAuthCredentials) throws IOException, ServletException {
        if (oAuthCredentials != null) {
            onAuthenticationFailure(request, response, "OAuth access token is not required, since security has been disabled on this server.");
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private OAuthCredentials extractOAuthToken(HttpServletRequest request) {
        final String header = request.getHeader(AUTHORIZATION);

        if (isBlank(header)) {
            return null;
        }

        final Matcher matcher = OAUTH_TOKEN_PATTERN.matcher(header);

        if (matcher.matches()) {
            return new OAuthCredentials(matcher.group(1));
        }
        return null;
    }
}
