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

package com.thoughtworks.go.server.security;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.AuthenticationException;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.security.ui.AccessDeniedHandler;
import org.springframework.security.ui.AuthenticationEntryPoint;
import org.springframework.security.ui.ExceptionTranslationFilter;
import org.springframework.security.ui.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class GoExceptionTranslationFilter extends ExceptionTranslationFilter {
    private static final Pattern URL_PATTERNS_THAT_SHOULD_NOT_BE_REDIRECTED_TO_AFTER_LOGIN = Pattern.compile("(\\.json)|(\\?.*format=json)|(/images/)|(\\.css)|(\\.ico)|(\\.js)|(/auth/login)|(/auth/logout)");
    private AuthenticationEntryPoint basicAuthenticationEntryPoint;
    private static final String REQUEST__FORMAT = "format";

    protected void sendStartAuthentication(ServletRequest request, ServletResponse response, FilterChain chain,
                                           AuthenticationException reason) throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        //TODO: This is a hack for bug #3175, we should revisit this code in V2.0
        if (isJson(httpRequest) || isJsonFormat(httpRequest)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final Logger logger = LoggerFactory.getLogger(GoExceptionTranslationFilter.class);
        SavedRequest savedRequest = new SavedRequest(httpRequest, getPortResolver());

        logger.debug("Authentication entry point being called; SavedRequest added to Session: {}", savedRequest);

        if (isCreateSessionAllowed() && shouldRedirect(savedRequest.getRequestUrl())) {
            // Store the HTTP request itself. Used by AbstractProcessingFilter
            // for redirection after successful authentication (SEC-29)
            httpRequest.getSession().setAttribute(AbstractProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY,
                    savedRequest);
        }

        // SEC-112: Clear the SecurityContextHolder's Authentication, as the
        // existing Authentication is no longer considered valid
        SecurityContextHolder.getContext().setAuthentication(null);

        determineAuthenticationPoint(httpRequest).commence(httpRequest, response, reason);
    }

    private AuthenticationEntryPoint determineAuthenticationPoint(HttpServletRequest httpRequest) {
        AuthenticationEntryPoint point = getAuthenticationEntryPoint();
        if (isJsonp(httpRequest)) {
            return basicAuthenticationEntryPoint;
        }
        return point;
    }

    private boolean isJsonFormat(HttpServletRequest httpRequest) {
        String format = httpRequest.getParameter(REQUEST__FORMAT);
        return format != null && format.equalsIgnoreCase("json");
    }

    private boolean isJson(HttpServletRequest httpRequest) {
        return httpRequest.getRequestURI().endsWith(".json") && StringUtils.isBlank(httpRequest.getParameter("callback"));
    }

    private boolean isJsonp(HttpServletRequest httpRequest) {
        return httpRequest.getRequestURI().endsWith(".json") &&
                !StringUtils.isBlank(httpRequest.getParameter("callback"));
    }

    boolean shouldRedirect(String url) {
        return !URL_PATTERNS_THAT_SHOULD_NOT_BE_REDIRECTED_TO_AFTER_LOGIN.matcher(url).find();
    }

    @Autowired
    public void setBasicAuthenticationEntryPoint(@Qualifier("basicProcessingFilterEntryPoint") AuthenticationEntryPoint basicAuthenticationEntryPoint) {
        this.basicAuthenticationEntryPoint = basicAuthenticationEntryPoint;
    }

    @Override
    @Autowired
    public void setAuthenticationEntryPoint(@Qualifier("sessionDenialAwareAuthenticationProcessingFilterEntryPoint") AuthenticationEntryPoint authenticationEntryPoint) {
        super.setAuthenticationEntryPoint(authenticationEntryPoint);
    }

    @Override
    @Autowired
    public void setAccessDeniedHandler(AccessDeniedHandler accessDeniedHandler) {
        super.setAccessDeniedHandler(accessDeniedHandler);
    }
}
