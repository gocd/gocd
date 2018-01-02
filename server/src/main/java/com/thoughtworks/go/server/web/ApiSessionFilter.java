/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.SpringSecurityFilter;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/* API requests should not start long-lived session. */
@Component
public class ApiSessionFilter extends SpringSecurityFilter {
    private final int idleTimeoutInSeconds;

    @Autowired
    public ApiSessionFilter(SystemEnvironment systemEnvironment) {
        idleTimeoutInSeconds = systemEnvironment.get(SystemEnvironment.API_REQUEST_IDLE_TIMEOUT_IN_SECONDS);
    }

    @Override
    protected void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        boolean hadSessionBeforeStarting = request.getSession(false) != null;

        try {
            chain.doFilter(request, response);
        } finally {
            HttpSession session = request.getSession(false);
            boolean hasSessionNow = session != null;

            if (!hadSessionBeforeStarting && hasSessionNow) {
               session.setMaxInactiveInterval(idleTimeoutInSeconds);
            }
        }
    }

    @Override
    public int getOrder() {
        return FilterChainOrder.HTTP_SESSION_CONTEXT_FILTER + 1;
    }
}
