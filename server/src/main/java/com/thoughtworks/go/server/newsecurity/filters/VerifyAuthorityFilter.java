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

import com.thoughtworks.go.server.newsecurity.handlers.ResponseHandler;
import com.thoughtworks.go.server.newsecurity.helpers.AuthorityVerifier;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class VerifyAuthorityFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyAuthorityFilter.class);
    private final AuthorityVerifier authorityVerifier;
    private final ResponseHandler requestHandler;

    public VerifyAuthorityFilter(Set<GrantedAuthority> grantedAuthorities,
                                 ResponseHandler responseHandler) {
        this.authorityVerifier = new AuthorityVerifier(grantedAuthorities);
        this.requestHandler = responseHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final AuthenticationToken<?> authentication = SessionUtils.getAuthenticationToken(request);
        final Set<GrantedAuthority> authorities = authentication.getUser().getAuthorities();

        if (authorityVerifier.hasAnyAuthorityMatching(authorities)) {
            LOGGER.debug("User {} authorized to access {}", authentication.getUser().getUsername(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } else {
            LOGGER.debug("User {} not authorized to access {}: has authorities {}", authentication.getUser().getUsername(), request.getRequestURI(), authentication.getUser().getAuthorities());
            if (SessionUtils.getCurrentUser().asUsernameObject().isAnonymous()) {
                requestHandler.handle(request, response, SC_UNAUTHORIZED, "You are not authenticated!");
            } else {
                requestHandler.handle(request, response, SC_FORBIDDEN, "You are not authorized to access this resource!");
            }
        }
    }
}
