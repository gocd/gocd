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

package com.thoughtworks.go.server.newsecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Component
public class AuthenticationFilter extends FilterChainProxy {
    public static final String CURRENT_USER = "CURRENT_USER";

    private static final GoAuthenticationFilter BASIC_AUTH_WITH_CHALLENGE = new GoAuthenticationFilter(new RequestHandler() {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) {
            response.addHeader("WWW-Authenticate", "Basic realm=\"GoCD\"");
        }
    });

    private static final GoAuthenticationFilter BASIC_AUTH_WITHOUT_CHALLENGE = new GoAuthenticationFilter(new RequestHandler() {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.sendRedirect("/go/auth/login");
        }
    });

    @Autowired
    public AuthenticationFilter(GoX509AuthenticationFilter x509AuthenticationFilter) {
        super(Arrays.asList(
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/remoting/**"), x509AuthenticationFilter),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/agent-websocket/**"), x509AuthenticationFilter),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/cctray.xml"), BASIC_AUTH_WITH_CHALLENGE),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/**"), BASIC_AUTH_WITH_CHALLENGE),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/**"), BASIC_AUTH_WITHOUT_CHALLENGE)
        ));
    }

}
