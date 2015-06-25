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

import org.apache.log4j.Logger;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.security.ui.basicauth.BasicProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BasicAuthenticationFilter extends BasicProcessingFilter {

    private static ThreadLocal<Boolean> isProcessingBasicAuth = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private static final Logger LOG = Logger.getLogger(BasicAuthenticationFilter.class);

    @Override
    public void doFilterHttp(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain) throws IOException, ServletException {
        try {
            isProcessingBasicAuth.set(true);
            super.doFilterHttp(httpRequest, httpResponse, chain);
        } catch (Exception e) {
            handleException(httpRequest, httpResponse, e);
        } finally {
            isProcessingBasicAuth.set(false);
        }
    }

    public void handleException(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Exception e) throws IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        String message = "There was an error authenticating you. Please check the server logs, or contact your the go administrator.";
        httpRequest.getSession().setAttribute(AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY, new RuntimeException(message));
        httpRequest.setAttribute(SessionDenialAwareAuthenticationProcessingFilterEntryPoint.SESSION_DENIED, true);
        context.setAuthentication(null);
        httpResponse.sendRedirect("/go/auth/login?login_error=1");
        LOG.error(e.getMessage());
        LOG.trace(e.getMessage(), e);
    }

    public static boolean isProcessingBasicAuth() {
        return isProcessingBasicAuth.get();
    }
}
