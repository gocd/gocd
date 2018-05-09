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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.ui.webapp.AuthenticationProcessingFilterEntryPoint;
import org.springframework.security.AuthenticationException;
import org.springframework.util.Assert;

/**
 * @understands the new unauthenticated sessions and denied sessions
 */
public class SessionDenialAwareAuthenticationProcessingFilterEntryPoint extends AuthenticationProcessingFilterEntryPoint {
    public static final String SESSION_DENIED = "session_denied";

    private String deniedSessionLoginFormUrl;

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.hasLength(deniedSessionLoginFormUrl, "deniedSessionLoginFormUrl must be specified");
    }

    @Override protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        Object hasSessionBeenDenied = request.getAttribute(SESSION_DENIED);
        return (hasSessionBeenDenied != null && (Boolean) hasSessionBeenDenied) ? deniedSessionLoginFormUrl : super.determineUrlToUseForThisRequest(request, response, exception);
    }

    public void setDeniedSessionLoginFormUrl(String deniedSessionLoginFormUrl) {
        this.deniedSessionLoginFormUrl = deniedSessionLoginFormUrl;
    }
}
