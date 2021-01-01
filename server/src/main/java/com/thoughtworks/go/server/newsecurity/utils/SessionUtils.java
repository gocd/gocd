/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.newsecurity.utils;

import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.PortResolver;
import org.springframework.security.web.PortResolverImpl;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;
import static com.thoughtworks.go.server.security.GoAuthority.ROLE_ANONYMOUS;

public class SessionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionUtils.class);
    private static final String AUTHENTICATION_TOKEN = "GOCD_SECURITY_AUTHENTICATION_TOKEN";
    private static final String CURRENT_USER_ID = "GOCD_SECURITY_CURRENT_USER_ID";
    private static final String AUTHENTICATION_ERROR = "GOCD_SECURITY_AUTHENTICATION_ERROR";
    private static final String SAVED_REQUEST = "GOCD_SECURITY_SAVED_REQUEST";
    private static final PortResolver PORT_RESOLVER = new PortResolverImpl();

    private static final ThreadLocal<GoUserPrinciple> USERS = ThreadLocal.withInitial(() -> new GoUserPrinciple("anonymous", "anonymous", ROLE_ANONYMOUS.asAuthority()));

    public static void setAuthenticationTokenWithoutRecreatingSession(AuthenticationToken<?> authenticationToken,
                                                                      HttpServletRequest request) {
        LOGGER.debug("Setting authentication on existing session.");
        request.getSession().setAttribute(AUTHENTICATION_TOKEN, authenticationToken);
    }

    public static void setAuthenticationTokenAfterRecreatingSession(AuthenticationToken<?> authenticationToken,
                                                                    HttpServletRequest request) {
        recreateSessionWithoutCopyingOverSessionState(request);
        LOGGER.debug("Setting authentication on new session.");
        request.getSession().setAttribute(AUTHENTICATION_TOKEN, authenticationToken);
    }

    public static AuthenticationToken<?> getAuthenticationToken(HttpServletRequest request) {
        return (AuthenticationToken<?>) request.getSession().getAttribute(AUTHENTICATION_TOKEN);
    }

    public static boolean hasAuthenticationToken(HttpServletRequest request) {
        return getAuthenticationToken(request) != null;
    }

    public static boolean isAnonymousAuthenticationToken(HttpServletRequest request) {
        return getAuthenticationToken(request).getCredentials() instanceof AnonymousCredential;
    }

    public static SavedRequest savedRequest(HttpServletRequest request) {
        return (SavedRequest) request.getSession().getAttribute(SAVED_REQUEST);
    }

    public static void saveRequest(HttpServletRequest request, SavedRequest savedRequest) {
        LOGGER.debug("Saving request {}", request.getRequestURI());
        request.getSession().setAttribute(SAVED_REQUEST, savedRequest);
    }

    public static void saveRequest(HttpServletRequest request) {
        saveRequest(request, new DefaultSavedRequest(request, PORT_RESOLVER));
    }

    public static void recreateSessionWithoutCopyingOverSessionState(HttpServletRequest request) {
        LOGGER.debug("Creating new session.");
        request.getSession().invalidate();
        request.getSession();
    }

    public static void redirectToLoginPage(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        SavedRequest savedRequest = SessionUtils.savedRequest(request);
        SessionUtils.recreateSessionWithoutCopyingOverSessionState(request);

        SessionUtils.saveRequest(request, savedRequest);
        SessionUtils.setAuthenticationError(errorMessage, request);
        response.sendRedirect("/go/auth/login");
    }

    public static void setAuthenticationError(String message, HttpServletRequest request) {
        request.getSession().setAttribute(AUTHENTICATION_ERROR, message);
    }

    public static void removeAuthenticationError(HttpServletRequest request) {
        request.getSession().removeAttribute(AUTHENTICATION_ERROR);
    }

    public static String getAuthenticationError(HttpServletRequest request) {
        return (String) request.getSession().getAttribute(AUTHENTICATION_ERROR);
    }

    public static boolean isAuthenticated(HttpServletRequest request,
                                          Clock clock,
                                          SystemEnvironment systemEnvironment) {
        final boolean isAuthenticated = hasAuthenticationToken(request) && getAuthenticationToken(request).isAuthenticated(clock, systemEnvironment);
        LOGGER.debug("Checking request is authenticated: {}", isAuthenticated);
        return isAuthenticated;
    }

    public static Long getUserId(HttpServletRequest request) {
        return (Long) request.getSession().getAttribute(CURRENT_USER_ID);
    }

    public static void setUserId(HttpServletRequest request, Long id) {
        if (id == null || id == NOT_PERSISTED) {
            LOGGER.debug("Unsetting current user id from session {}", id);
            request.getSession().removeAttribute(CURRENT_USER_ID);
        } else {
            LOGGER.debug("Setting current user id into session {}", id);
            request.getSession().setAttribute(CURRENT_USER_ID, id);
        }
    }

    public static GoUserPrinciple getCurrentUser() {
        return USERS.get();
    }

    public static void setCurrentUser(GoUserPrinciple user) {
        if (user == null) {
            throw new IllegalArgumentException("Use unsetCurrentUser instead");
        }
        LOGGER.debug("Setting user {} into thread local", user.getUsername());
        USERS.set(user);
    }

    public static void unsetCurrentUser() {
        LOGGER.debug("Unsetting current user from thread local");
        USERS.remove();
    }

    public static Username currentUsername() {
        return getCurrentUser().asUsernameObject();
    }
}
