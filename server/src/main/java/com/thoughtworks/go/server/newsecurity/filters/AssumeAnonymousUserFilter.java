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

import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AssumeAnonymousUserFilter extends OncePerRequestFilter {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final SecurityService securityService;
    private final Clock clock;

    @Autowired
    public AssumeAnonymousUserFilter(SecurityService securityService,
                                     Clock clock) {
        this.securityService = securityService;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SessionUtils.hasAuthenticationToken(request)) {
            LOGGER.debug("Already authenticated request.");
        } else {
            GoUserPrinciple anonymous;
            if (securityService.isSecurityEnabled()) {
                anonymous = GoUserPrinciple.ANONYMOUS_WITH_SECURITY_ENABLED;
            } else {
                anonymous = GoUserPrinciple.ANONYMOUS_WITH_SECURITY_DISABLED;
            }

            AuthenticationToken<AnonymousCredential> authenticationToken = new AuthenticationToken<>(anonymous, AnonymousCredential.INSTANCE, null, clock.currentTimeMillis(), null);

            LOGGER.debug("Authenticating as anonymous user with role(s) {}", anonymous.getAuthorities());
            SessionUtils.setAuthenticationTokenWithoutRecreatingSession(authenticationToken, request);
        }

        filterChain.doFilter(request, response);
    }
}
