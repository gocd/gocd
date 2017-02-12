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

import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CasProcessingFilter extends CasAuthenticationFilter {

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.getContext();
        request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new OnlyKnownUsersAllowedException("Foo"));
        request.setAttribute(SessionDenialAwareAuthenticationProcessingFilterEntryPoint.SESSION_DENIED, true);
        context.setAuthentication(null);
        response.sendRedirect("/go/auth/login");
        super.unsuccessfulAuthentication(request, response, failed);
    }
}
