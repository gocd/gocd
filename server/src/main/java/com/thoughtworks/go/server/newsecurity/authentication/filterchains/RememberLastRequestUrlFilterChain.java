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

package com.thoughtworks.go.server.newsecurity.authentication.filterchains;

import com.thoughtworks.go.server.newsecurity.authentication.utils.SessionUtils;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Component
public class RememberLastRequestUrlFilterChain extends FilterChainProxy {

    public RememberLastRequestUrlFilterChain() {
        super(Arrays.asList(
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/cctray.xml")),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/**")),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/remoting/**")),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/agent-websocket/**")),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/auth/**")),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/plugin/*/authenticate")),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/plugin/*/login")),
                new DefaultSecurityFilterChain(new AntPathRequestMatcher("/**"), new RememberLastRequestUrlFilter())
        ));
    }

    private static class RememberLastRequestUrlFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            SessionUtils.saveRequest(request);
        }
    }
}
