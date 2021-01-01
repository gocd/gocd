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

import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.providers.AnonymousAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AssumeAnonymousUserFilter.class);
    private final SecurityService securityService;


    private final AnonymousAuthenticationProvider anonymousAuthenticationProvider;

    @Autowired
    public AssumeAnonymousUserFilter(SecurityService securityService,
                                     AnonymousAuthenticationProvider anonymousAuthenticationProvider) {
        this.securityService = securityService;
        this.anonymousAuthenticationProvider = anonymousAuthenticationProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!securityService.isSecurityEnabled()) {
            LOGGER.debug("Security is disabled.");
            authenticateAsAnonymous(request);
        } else if (SessionUtils.hasAuthenticationToken(request)) {
            LOGGER.debug("Already authenticated request.");
        } else {
            LOGGER.debug("Security is enabled.");
            authenticateAsAnonymous(request);
        }

        LOGGER.debug("User authenticated as anonymous user {}", SessionUtils.getAuthenticationToken(request));
        filterChain.doFilter(request, response);

    }

    private void authenticateAsAnonymous(HttpServletRequest request) {
        SessionUtils.setAuthenticationTokenWithoutRecreatingSession(anonymousAuthenticationProvider.authenticate(AnonymousCredential.INSTANCE, null), request);
    }
}
