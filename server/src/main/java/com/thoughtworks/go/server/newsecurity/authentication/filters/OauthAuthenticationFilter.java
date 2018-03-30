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

import com.thoughtworks.go.server.newsecurity.authentication.matchers.RequestHeaderRequestMatcher;
import com.thoughtworks.go.server.newsecurity.authentication.providers.OAuthAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OauthAuthenticationFilter extends AbstractAuthenticationFilter {
    private static final String AUTHORIZATION = "Authorization";
    private static final Pattern OAUTH_TOKEN_PATTERN = Pattern.compile("^Token token=\"(.*?)\"$");
    private static final RequestHeaderRequestMatcher OAUTH_TOKEN_HEADER_MATCHER = new RequestHeaderRequestMatcher(AUTHORIZATION, OAUTH_TOKEN_PATTERN);
    private final OAuthAuthenticationProvider authenticationProvider;

    @Autowired
    public OauthAuthenticationFilter(OAuthAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    protected boolean isSecurityEnabled() {
        return true;
    }

    @Override
    protected void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //TODO: set a status code on response if needed
    }

    @Override
    protected User attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        final String header = request.getHeader(AUTHORIZATION);
        final Matcher matcher = OAUTH_TOKEN_PATTERN.matcher(header);

        if (matcher.matches()) {
            final String token = matcher.group(1);
            return authenticationProvider.authenticate(token);
        }

        return null;
    }

    @Override
    protected boolean canHandleRequest(HttpServletRequest request) {
        return OAUTH_TOKEN_HEADER_MATCHER.matches(request);
    }
}
