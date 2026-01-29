/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.newsecurity.controllers;

import com.thoughtworks.go.plugin.domain.authorization.AuthorizationServerUrlResponse;
import com.thoughtworks.go.server.newsecurity.models.AccessToken;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.newsecurity.utils.SessionUtils.isAnonymousAuthenticationToken;

@Controller
public class AuthenticationController {
    public static final String BAD_CREDENTIALS_MSG = "Invalid credentials. Either your username and password are incorrect, or there is a problem with your browser cookies. Please check with your administrator.";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);
    private static final RedirectView REDIRECT_TO_LOGIN_PAGE = new RedirectView("/auth/login", true);
    private static final String UNKNOWN_ERROR_WHILE_AUTHENTICATION = "There was an unknown error authenticating you. Please try again after some time, and contact the administrator if the problem persists.";
    private final SecurityService securityService;
    private final SystemEnvironment systemEnvironment;
    private final Clock clock;
    private final PasswordBasedPluginAuthenticationProvider passwordBasedPluginAuthenticationProvider;
    private final WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider;

    @Autowired
    public AuthenticationController(SecurityService securityService,
                                    SystemEnvironment systemEnvironment,
                                    Clock clock,
                                    PasswordBasedPluginAuthenticationProvider passwordBasedPluginAuthenticationProvider,
                                    WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider) {
        this.securityService = securityService;
        this.systemEnvironment = systemEnvironment;
        this.clock = clock;
        this.passwordBasedPluginAuthenticationProvider = passwordBasedPluginAuthenticationProvider;
        this.webBasedPluginAuthenticationProvider = webBasedPluginAuthenticationProvider;
    }

    @RequestMapping(value = "/auth/security_check", method = RequestMethod.POST)
    public RedirectView performLogin(@RequestParam("j_username") String username,
                                     @RequestParam("j_password") String password,
                                     HttpServletRequest request) {
        if (securityIsDisabledOrAlreadyLoggedIn(request)) {
            return new RedirectView("/pipelines", true);
        }

        LOGGER.debug("Requesting authentication for form auth.");
        try {
            SavedRequest savedRequest = SessionUtils.savedRequest(request);

            final AuthenticationToken<UsernamePassword> authenticationToken = passwordBasedPluginAuthenticationProvider.authenticate(new UsernamePassword(username, password), null);

            if (authenticationToken == null) {
                return badAuthentication(request, BAD_CREDENTIALS_MSG);
            } else {
                SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
            }
            String redirectUrl = savedRequest == null ? "/go/pipelines" : savedRequest.getRedirectUrl();
            return new RedirectView(redirectUrl, false);

        } catch (AuthenticationException e) {
            LOGGER.error("Failed to authenticate user: {} ", username, e);
            return badAuthentication(request, e.getMessage());
        } catch (Exception e) {
            return unknownAuthenticationError(request);
        }
    }

    @RequestMapping(value = "/plugin/{pluginId}/login", method = RequestMethod.GET)
    public RedirectView redirectToThirdPartyLoginPage(@PathVariable("pluginId") String pluginId,
                                                      HttpServletRequest request) {
        if (securityIsDisabledOrAlreadyLoggedIn(request)) {
            return new RedirectView("/pipelines", true);
        }

        AuthorizationServerUrlResponse authorizationServerUrlResponse = webBasedPluginAuthenticationProvider.getAuthorizationServerUrl(pluginId, () -> rootUrlFrom(request));

        SessionUtils.setPluginAuthSessionContext(request, pluginId, authorizationServerUrlResponse.getAuthSession());

        return new RedirectView(authorizationServerUrlResponse.getAuthorizationServerUrl(), false);
    }

    private static @NotNull String rootUrlFrom(HttpServletRequest request) {
        StringBuffer rootUrlWithPath = request.getRequestURL();
        return rootUrlWithPath.substring(0, rootUrlWithPath.length() - request.getRequestURI().length());
    }

    @RequestMapping(value = "/plugin/{pluginId}/authenticate")
    public RedirectView authenticateWithWebBasedPlugin(@PathVariable("pluginId") String pluginId,
                                                       HttpServletRequest request) {
        if (securityIsDisabledOrAlreadyLoggedIn(request)) {
            return new RedirectView("/pipelines", true);
        }

        LOGGER.debug("Requesting authentication for form auth.");
        SavedRequest savedRequest = SessionUtils.savedRequest(request);

        try {
            Map<String, String> pluginAuthSessionContext = SessionUtils.getPluginAuthSessionContext(request, pluginId);

            final AccessToken accessToken = webBasedPluginAuthenticationProvider.fetchAccessToken(pluginId, getRequestHeaders(request), getParameterMap(request), pluginAuthSessionContext);

            SessionUtils.removePluginAuthSessionContext(request, pluginId);

            AuthenticationToken<AccessToken> authenticationToken = webBasedPluginAuthenticationProvider.authenticate(accessToken, pluginId);

            if (authenticationToken == null) {
                return unknownAuthenticationError(request);
            }

            SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
        } catch (AuthenticationException e) {
            LOGGER.error("Failed to authenticate user.", e);
            return badAuthentication(request, e.getMessage());
        } catch (Exception e) {
            return unknownAuthenticationError(request);
        }

        SessionUtils.removeAuthenticationError(request);

        String redirectUrl = savedRequest == null ? "/go/pipelines" : savedRequest.getRedirectUrl();

        return new RedirectView(redirectUrl, false);
    }

    private boolean securityIsDisabledOrAlreadyLoggedIn(HttpServletRequest request) {
        return !securityService.isSecurityEnabled() || (!isAnonymousAuthenticationToken(request) && SessionUtils.isAuthenticated(request, clock, systemEnvironment));
    }

    private RedirectView badAuthentication(HttpServletRequest request, String message) {
        SessionUtils.setAuthenticationError(message, request);
        return REDIRECT_TO_LOGIN_PAGE;
    }

    private RedirectView unknownAuthenticationError(HttpServletRequest request) {
        return badAuthentication(request, UNKNOWN_ERROR_WHILE_AUTHENTICATION);
    }

    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            String value = request.getHeader(header);
            headers.put(header, value);
        }
        return headers;
    }

    private Map<String, String> getParameterMap(HttpServletRequest request) {
        Map<String, String[]> springParameterMap = request.getParameterMap();
        Map<String, String> pluginParameterMap = new HashMap<>();
        for (String parameterName : springParameterMap.keySet()) {
            String[] values = springParameterMap.get(parameterName);
            if (values != null && values.length > 0) {
                pluginParameterMap.put(parameterName, values[0]);
            } else {
                pluginParameterMap.put(parameterName, null);
            }
        }
        return pluginParameterMap;
    }

}
