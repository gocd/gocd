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

import com.thoughtworks.go.server.newsecurity.authentication.handlers.GoAccessDeniedHandler;
import com.thoughtworks.go.server.newsecurity.authentication.matchers.RequestHeaderRequestMatcher;
import com.thoughtworks.go.server.newsecurity.authentication.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isBlank;

@Component
public class BasicAuthenticationFilter extends AbstractAuthenticationFilter {
    private static final Pattern BASIC_AUTH_EXTRACTOR_PATTERN = Pattern.compile("basic (.*)", Pattern.CASE_INSENSITIVE);
    private static final RequestHeaderRequestMatcher AUTHORIZATION_HEADER_MATCHER = new RequestHeaderRequestMatcher("Authorization", BASIC_AUTH_EXTRACTOR_PATTERN);

    private final GoAccessDeniedHandler accessDeniedHandler;
    private final PasswordBasedPluginAuthenticationProvider authenticationProvider;
    private final SecurityService securityService;

    @Autowired
    public BasicAuthenticationFilter(SecurityService securityService, PasswordBasedPluginAuthenticationProvider authenticationProvider) {
        this.securityService = securityService;
        this.accessDeniedHandler = new GoAccessDeniedHandler();
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    protected boolean isSecurityEnabled() {
        return securityService.isSecurityEnabled();
    }

    @Override
    protected void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        LOGGER.debug("[Basic Authentication Failure] Failed to authenticate user using basic auth.");
        accessDeniedHandler.handle(request, response);
    }

    @Override
    protected User attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        final BasicAuthenticationDetails basicAuthenticationDetails = extractBasicAuthenticationCredentials(request);

        if (basicAuthenticationDetails == null) {
            throw new BadCredentialsException("[Basic Authentication] Invalid basic authentication credentials specified in request.");
        }

        LOGGER.debug("[Basic Authentication] Requesting authentication for basic auth.");
        return authenticationProvider.authenticate(basicAuthenticationDetails.getUsername(), basicAuthenticationDetails.getPassword());
    }

    @Override
    protected boolean canHandleRequest(HttpServletRequest request) {
        return AUTHORIZATION_HEADER_MATCHER.matches(request) &&
                authenticationProvider.hasPluginsForUsernamePasswordAuth();
    }

    public BasicAuthenticationDetails extractBasicAuthenticationCredentials(HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if (isBlank(header)) {
            return null;
        }

        final Matcher matcher = BASIC_AUTH_EXTRACTOR_PATTERN.matcher(header);
        if (matcher.matches()) {
            final String encodedCredentials = matcher.group(1);
            final byte[] decode = Base64.getDecoder().decode(encodedCredentials);
            String decodedCredentials = new String(decode, StandardCharsets.UTF_8);

            final int indexOfSeparator = decodedCredentials.indexOf(':');
            if (indexOfSeparator == -1) {
                throw new BadCredentialsException("[Basic Authentication] Invalid basic authentication credentials specified in request.");
            }

            final String username = decodedCredentials.substring(0, indexOfSeparator);
            final String password = decodedCredentials.substring(indexOfSeparator + 1);

            LOGGER.debug(String.format("[Basic Authentication] Authorization header found for user '%s'", username));

            return new BasicAuthenticationDetails(username, password);
        }

        return null;
    }

    private class BasicAuthenticationDetails {
        private final String username;
        private final String password;

        public BasicAuthenticationDetails(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
