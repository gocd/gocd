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

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationServiceException;

public class AuthenticationProcessingFilter extends org.springframework.security.ui.webapp.AuthenticationProcessingFilter {
    public static final String LICENSE_EXPIRING_IN = "LICENSE_EXPIRING_IN";
    private static final Logger LOGGER = Logger.getLogger(AuthenticationProcessingFilter.class);
    private GoConfigService goConfigService;
    private final GoLicenseService goLicenseService;
    private final UserService userService;
    private final TimeProvider timeProvider;
    private final SystemEnvironment systemEnvironment;
    private Localizer localizer;

    
    @Autowired
    public AuthenticationProcessingFilter(GoConfigService goConfigService, GoLicenseService goLicenseService, UserService userService, TimeProvider timeProvider,
                                          SystemEnvironment systemEnvironment, Localizer localizer) {
        this.goConfigService = goConfigService;
        this.goLicenseService = goLicenseService;
        this.userService = userService;
        this.timeProvider = timeProvider;
        this.systemEnvironment = systemEnvironment;
        this.localizer = localizer;
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        if (goConfigService.isSecurityEnabled()) {
            return super.requiresAuthentication(request, response);
        } else {
            return false;
        }
    }

    @Override protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
        super.onSuccessfulAuthentication(request,response, authResult);
        long diff = goLicenseService.getExpirationDate().getTime() - timeProvider.currentTimeMillis();
        int days = systemEnvironment.getLicenseExpiryWarningTime();
        User user = user();

        if (diff <= (days * millsInADay()) && !user.hasDisabledLicenseExpiryWarning()) {
            long numberOfDaysToExpiry = diff / millsInADay();
            request.getSession().setAttribute(LICENSE_EXPIRING_IN,numberOfDaysToExpiry);
        }
    }

    @Override protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        super.onUnsuccessfulAuthentication(request, response, failed);
        if( failed.getClass() == AuthenticationServiceException.class){
            request.getSession().setAttribute(SPRING_SECURITY_LAST_EXCEPTION_KEY, new Exception(localizer.localize("AUTHENTICATION_SERVICE_EXCEPTION")));
            LOGGER.error(failed);
        }
    }

    private User user() {
        return userService.findUserByName(UserHelper.getUserName().getUsername().toString());
    }

    public static long millsInADay() {
        return 1000 * 3600 * 24L;
    }
}

