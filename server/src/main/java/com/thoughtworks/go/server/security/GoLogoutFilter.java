/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.server.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.SpringSecurityFilter;
import org.springframework.security.ui.logout.LogoutFilter;
import org.springframework.security.ui.logout.LogoutHandler;

public class GoLogoutFilter extends SpringSecurityFilter {

    private final SecurityService securityService;
    private final String logoutUrl;
    private final LogoutHandler[] handlers;

    @Autowired
    public GoLogoutFilter(SecurityService securityService, String logoutUrl, LogoutHandler[] handlers) {
        this.securityService = securityService;
        this.logoutUrl = logoutUrl;
        this.handlers = handlers;
    }

    @Override protected void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        LogoutFilter logoutFilter = new LogoutFilter(securityService.logoutSuccessUrl(), handlers);
        logoutFilter.setFilterProcessesUrl(logoutUrl);
        logoutFilter.doFilterHttp(request, response, chain);
    }

    public int getOrder() {
        return FilterChainOrder.LOGOUT_FILTER;
    }
}
