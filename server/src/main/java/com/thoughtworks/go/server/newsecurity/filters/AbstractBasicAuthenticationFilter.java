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
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.server.newsecurity.controllers.AuthenticationController.BAD_CREDENTIALS_MSG;
import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class AbstractBasicAuthenticationFilter extends OncePerRequestFilter {
    private static final Pattern BASIC_AUTH_EXTRACTOR_PATTERN = Pattern.compile("basic (.*)", Pattern.CASE_INSENSITIVE);
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
            final UsernamePassword usernamePassword = extractBasicAuthenticationCredentials(request);

            if (securityService.isSecurityEnabled()) {
                filterWhenSecurityEnabled(request, response, filterChain, usernamePassword);
            } else {
                filterWhenSecurityDisabled(request, response, filterChain, usernamePassword);
            }
        } catch (AuthenticationException e) {
            onAuthenticationFailure(request, response, e.getMessage());
        }
    }

    private void filterWhenSecurityEnabled(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain,
                                           UsernamePassword usernamePassword) throws IOException, ServletException {
        if (usernamePassword == null) {
            filterChain.doFilter(request, response);
        } else {
            final AuthenticationToken<UsernamePassword> authenticationToken = authenticationProvider.authenticate(usernamePassword, null);

            if (authenticationToken == null) {
                onAuthenticationFailure(request, response, BAD_CREDENTIALS_MSG);
            } else {
                SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
                filterChain.doFilter(request, response);
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


    private UsernamePassword extractBasicAuthenticationCredentials(HttpServletRequest request) {
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
                throw new BadCredentialsException("Invalid basic authentication credentials specified in request.");
            }

            final String username = decodedCredentials.substring(0, indexOfSeparator);
            final String password = decodedCredentials.substring(indexOfSeparator + 1);

            LOGGER.debug("[Basic Authentication] Authorization header found for user '{}'", username);

            return new UsernamePassword(username, password);
        }

        return null;
    }


}
