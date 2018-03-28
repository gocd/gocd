///*
// * Copyright 2018 ThoughtWorks, Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.thoughtworks.go.server.security;
//
//import com.thoughtworks.go.i18n.Localizer;
//import com.thoughtworks.go.server.service.GoConfigService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.AuthenticationServiceException;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
//import org.springframework.security.web.authentication.AuthenticationFailureHandler;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//
//@Component
//public class AuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {
//    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationProcessingFilter.class);
//    private GoConfigService goConfigService;
//    private Localizer localizer;
//
//    @Autowired
//    public AuthenticationProcessingFilter(GoConfigService goConfigService, Localizer localizer, @Qualifier("goAuthenticationManager") AuthenticationManager authenticationManager) {
//        super("/auth/security_check");
//        this.goConfigService = goConfigService;
//        this.localizer = localizer;
//        this.setAuthenticationManager(authenticationManager);
//        setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/auth/login?login_error=1"));
//        setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler("/"));
//        setInvalidateSessionOnSuccessfulAuthentication(true);
//    }
//
//    @Override
//    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
//        if (goConfigService.isSecurityEnabled()) {
//            return super.requiresAuthentication(request, response);
//        } else {
//            return false;
//        }
//    }
//
//    @Override
//    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
//        return null;
//    }
//
//    @Override
//    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
//        super.unsuccessfulAuthentication(request, response, failed);
//
//        if (failed.getClass() == AuthenticationServiceException.class) {
//            request.getSession().setAttribute(SPRING_SECURITY_LAST_EXCEPTION_KEY, new Exception(localizer.localize("AUTHENTICATION_SERVICE_EXCEPTION")));
//            LOGGER.error(failed.getMessage());
//            LOGGER.trace(failed.getMessage(), failed);
//        }
//    }
//
//}
//
