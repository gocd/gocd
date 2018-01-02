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

import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.SpringSecurityFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ReAuthenticationFilter extends SpringSecurityFilter {
    private final SystemEnvironment systemEnvironment;
    private final TimeProvider timeProvider;
    protected static final String LAST_REAUTHENICATION_CHECK_TIME = "last_reauthentication_check_time";

    public ReAuthenticationFilter(SystemEnvironment systemEnvironment, TimeProvider timeProvider) {
        this.systemEnvironment = systemEnvironment;
        this.timeProvider = timeProvider;
    }

    @Override
    protected void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!systemEnvironment.isReAuthenticationEnabled() || (authentication == null) || !authenticatedUsingAuthorizationPlugin(authentication)) {
            chain.doFilter(request, response);
            return;
        }

        synchronized (request.getSession().getId().intern()) {
            Long lastAuthenticationTime = (Long) request.getSession().getAttribute(LAST_REAUTHENICATION_CHECK_TIME);
            if (lastAuthenticationTime == null) {
                request.getSession().setAttribute(LAST_REAUTHENICATION_CHECK_TIME, timeProvider.currentTimeMillis());
            } else if (forceReAuthentication(lastAuthenticationTime)) {
                request.getSession().setAttribute(LAST_REAUTHENICATION_CHECK_TIME, timeProvider.currentTimeMillis());
                authentication.setAuthenticated(false);
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return FilterChainOrder.AUTHENTICATION_PROCESSING_FILTER + 1;
    }

    private boolean forceReAuthentication(Long lastAuthenticationTime) {
        return (timeProvider.currentTimeMillis() - lastAuthenticationTime) > systemEnvironment.getReAuthenticationTimeInterval();
    }

    private boolean authenticatedUsingAuthorizationPlugin(Authentication authentication) {
        if (authentication.getPrincipal() instanceof GoUserPrinciple) {
            return ((GoUserPrinciple) (authentication.getPrincipal())).authenticatedUsingAuthorizationPlugin();
        }
        return false;
    }
}
