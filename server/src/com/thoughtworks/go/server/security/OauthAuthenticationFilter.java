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

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OauthAuthenticationFilter extends org.springframework.security.web.authentication.www.BasicAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private static final Pattern OAUTH_TOKEN_PATTERN = Pattern.compile("^Token token=\"(.*?)\"$");
    static final String AUTHORIZATION = "Authorization";

    public OauthAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
        this.authenticationManager = authenticationManager;
    }

    protected void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(AUTHORIZATION);//Token token="ACCESS_TOKEN"

        if (header != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Oauth authorization header: " + header);
            }
            Matcher matcher = OAUTH_TOKEN_PATTERN.matcher(header);
            if (matcher.matches()) {
                String token = matcher.group(1);
                OauthAuthenticationToken authenticationToken = new OauthAuthenticationToken(token);
                try {
                    Authentication authResult = authenticationManager.authenticate(authenticationToken);
                    SecurityContextHolder.getContext().setAuthentication(authResult);
                } catch (AuthenticationException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Oauth authentication request for token: " + token, e);
                    }
                    SecurityContextHolder.getContext().setAuthentication(null);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
