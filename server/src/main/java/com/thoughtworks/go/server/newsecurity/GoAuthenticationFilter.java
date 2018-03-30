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

import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class GoAuthenticationFilter extends AbstractAuthenticationFilter {

    private static final RequestMatcher AJAX_MATCHER = new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest");
    private final GoAccessDeniedHandler accessDeniedHandler;

    GoAuthenticationFilter(RequestHandler authenticationHandler) {
        this.accessDeniedHandler = new GoAccessDeniedHandler(authenticationHandler);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final User currentUser = (User) request.getSession().getAttribute(AuthenticationFilter.CURRENT_USER);

        if (currentUser == null) {
            if (isAjaxRequest(request)) {
                accessDeniedHandler.handle(request, response);
            } else {
                accessDeniedHandler.handle(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return AJAX_MATCHER.matches(request);
    }
}
