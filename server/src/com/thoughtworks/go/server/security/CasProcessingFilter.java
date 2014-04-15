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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.AuthenticationException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

public class CasProcessingFilter extends  org.springframework.security.ui.cas.CasProcessingFilter {
    @Override protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        request.getSession().setAttribute(SPRING_SECURITY_LAST_EXCEPTION_KEY, new OnlyKnownUsersAllowedException("Foo"));
        request.setAttribute(SessionDenialAwareAuthenticationProcessingFilterEntryPoint.SESSION_DENIED, true);
        context.setAuthentication(null);
        response.sendRedirect("/go/auth/login");
        super.onUnsuccessfulAuthentication(request, response, failed);
    }
}
