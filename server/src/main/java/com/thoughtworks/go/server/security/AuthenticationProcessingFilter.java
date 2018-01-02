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

import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.GoConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationServiceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationProcessingFilter extends org.springframework.security.ui.webapp.AuthenticationProcessingFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationProcessingFilter.class);
    private GoConfigService goConfigService;
    private Localizer localizer;

    @Autowired
    public AuthenticationProcessingFilter(GoConfigService goConfigService, Localizer localizer) {
        this.goConfigService = goConfigService;
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

    @Override
    protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        super.onUnsuccessfulAuthentication(request, response, failed);
        if (failed.getClass() == AuthenticationServiceException.class) {
            request.getSession().setAttribute(SPRING_SECURITY_LAST_EXCEPTION_KEY, new Exception(localizer.localize("AUTHENTICATION_SERVICE_EXCEPTION")));
            LOGGER.error(failed.getMessage());
            LOGGER.trace(failed.getMessage(), failed);
        }
    }

}

