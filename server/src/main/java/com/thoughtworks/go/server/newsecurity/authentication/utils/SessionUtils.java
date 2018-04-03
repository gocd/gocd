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

package com.thoughtworks.go.server.newsecurity.authentication.utils;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.PortResolver;
import org.springframework.security.web.PortResolverImpl;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SessionUtils {

    public static final String CURRENT_USER = "GOCD_SECURITY_CURRENT_USER";
    private static final String AUTHENTICATION_ERROR = "GOCD_SECURITY_AUTHENTICATION_ERROR";
    private static final String SAVED_REQUEST = "GOCD_SECURITY_SAVED_REQUEST";
    private static final String WEB_AUTH_ACCESS_TOKEN = "GOCD_SECURITY_WEB_AUTH_ACCESS_TOKEN";

    private static final PortResolver PORT_RESOLVER = new PortResolverImpl();

    public static void setUser(User user, HttpServletRequest request) {
        recreateSession(request);
        request.getSession().setAttribute(CURRENT_USER, user);
    }

    public static User getUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(CURRENT_USER);
    }

    public static boolean hasUser(HttpServletRequest request) {
        return getUser(request) != null;
    }

    public static SavedRequest savedRequest(HttpServletRequest request) {
        return (SavedRequest) request.getSession().getAttribute(SAVED_REQUEST);
    }

    public static void saveRequest(HttpServletRequest request) {
        request.getSession().setAttribute(SAVED_REQUEST, new DefaultSavedRequest(request, PORT_RESOLVER));
    }

    private static void recreateSession(HttpServletRequest request) {
        request.getSession().invalidate();
        request.getSession();
    }

    public static void setAccessToken(Map<String, String> accessToken, HttpServletRequest request) {
        request.getSession().setAttribute(WEB_AUTH_ACCESS_TOKEN, accessToken);
    }

    public static void setAuthenticationError(String message, HttpServletRequest request) {
        request.getSession().setAttribute(AUTHENTICATION_ERROR, message);
    }

    public static void removeAuthenticationError(HttpServletRequest request) {
        request.getSession().removeAttribute(AUTHENTICATION_ERROR);
    }
}
