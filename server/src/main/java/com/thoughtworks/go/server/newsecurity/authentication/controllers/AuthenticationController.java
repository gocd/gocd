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

package com.thoughtworks.go.server.newsecurity.authentication.controllers;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.newsecurity.authentication.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.authentication.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.newsecurity.authentication.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityAuthConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.web.GoVelocityView;
import com.thoughtworks.go.server.web.SiteUrlProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.newsecurity.authentication.utils.SessionUtils.AUTHENTICATION_ERROR;

@Controller
public class AuthenticationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);
    private static final RedirectView REDIRECT_TO_LOGIN_PAGE = new RedirectView("/auth/login", true);
    private final SecurityService securityService;
    private final SecurityAuthConfigService securityAuthConfigService;
    private final PasswordBasedPluginAuthenticationProvider passwordBasedPluginAuthenticationProvider;
    private final Localizer localizer;
    private final WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider;
    private final SiteUrlProvider urlProvider;

    @Autowired
    public AuthenticationController(Localizer localizer,
                                    SecurityService securityService,
                                    SecurityAuthConfigService securityAuthConfigService,
                                    PasswordBasedPluginAuthenticationProvider passwordBasedPluginAuthenticationProvider,
                                    WebBasedPluginAuthenticationProvider webBasedPluginAuthenticationProvider,
                                    SiteUrlProvider urlProvider) {
        this.localizer = localizer;
        this.securityService = securityService;
        this.securityAuthConfigService = securityAuthConfigService;
        this.passwordBasedPluginAuthenticationProvider = passwordBasedPluginAuthenticationProvider;
        this.webBasedPluginAuthenticationProvider = webBasedPluginAuthenticationProvider;
        this.urlProvider = urlProvider;
    }

    @RequestMapping(value = "/auth/security_check", method = RequestMethod.POST)
    public RedirectView performLogin(@RequestParam("j_username") String username,
                                     @RequestParam("j_password") String password,
                                     HttpServletRequest request) {
        if (securityIsDisabledOrAlreadyLoggedIn(request)) {
            return new RedirectView("/pipelines", false);
        }

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return badAuthentication(request, "Username and password must be specified!");
        }

        LOGGER.debug("Requesting authentication for form auth.");
        final User user = passwordBasedPluginAuthenticationProvider.authenticate(username, password);
        SavedRequest savedRequest = SessionUtils.savedRequest(request);

        if (user == null) {
            return badAuthentication(request, "Invalid credentials. Either your username and password are incorrect, or there is a problem with your browser cookies. Please check with your administrator.");
        } else {
            SessionUtils.setUser(user, request);
        }

        SessionUtils.set
        request.getSession().removeAttribute(AUTHENTICATION_ERROR);

        String redirectUrl = savedRequest == null ? "/pipelines" : savedRequest.getRedirectUrl();

        return new RedirectView(redirectUrl, false);
    }

    @RequestMapping(value = "/auth/login", method = RequestMethod.GET)
    public Object renderLoginPage(HttpServletRequest request) {
        if (securityIsDisabledOrAlreadyLoggedIn(request)) {
            return new RedirectView("/pipelines", false);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("l", localizer);
        model.put("security_auth_config_service", securityAuthConfigService);
        model.put(GoVelocityView.CURRENT_GOCD_VERSION, CurrentGoCDVersion.getInstance());

        return new ModelAndView("auth/login", model);
    }

    @RequestMapping(value = "/plugin/${pluginId}/login")
    public RedirectView redirectToThirdPartyLoginPage(@PathVariable("pluginId") String pluginId, HttpServletRequest request) {
        if (securityIsDisabledOrAlreadyLoggedIn(request)) {
            return new RedirectView("/pipelines", false);
        }


        final StringBuffer requestURL = request.getRequestURL();
        requestURL.setLength(requestURL.length() - request.getRequestURI().length());
        return new RedirectView(webBasedPluginAuthenticationProvider.getAuthorizationServerUrl(pluginId, requestURL.toString()), false);
    }


    @RequestMapping(value = "/plugin/${pluginId}/authenticate")
    public RedirectView authenticateWithWebBasedPlugin(@PathVariable("pluginId") String pluginId, HttpServletRequest request) {
        if (securityIsDisabledOrAlreadyLoggedIn(request)) {
            return new RedirectView("/pipelines", false);
        }

        LOGGER.debug("Requesting authentication for form auth.");
        SavedRequest savedRequest = SessionUtils.savedRequest(request);

        try {
            final Map<String, String> accessToken = webBasedPluginAuthenticationProvider.fetchAccessToken(pluginId, getRequestHeaders(request), getParameterMap(request));

            AuthenticationResponse authenticationResponse = webBasedPluginAuthenticationProvider.authenticateUser(pluginId, accessToken);

            if (authenticationResponse == null) {
                return badAuthentication(request, "There was an unknown error authenticating you. Please try again after some time, and contact the administrator if the problem persists.");
            }

            SessionUtils.setAccessToken(accessToken, request);
        } catch (Exception e) {
            return badAuthentication(request, "There was an unknown error authenticating you. Please try again after some time, and contact the administrator if the problem persists.");
        }


        if (user == null) {
            return badAuthentication(request, "Invalid credentials. Either your username and password are incorrect, or there is a problem with your browser cookies. Please check with your administrator.");
        } else {
            SessionUtils.setUser(user, request);
        }

        SessionUtils.removeAuthenticationError(request);

        String redirectUrl = savedRequest == null ? "/pipelines" : savedRequest.getRedirectUrl();

        return new RedirectView(redirectUrl, false);

    }

    private boolean securityIsDisabledOrAlreadyLoggedIn(HttpServletRequest request) {
        return !securityService.isSecurityEnabled() || SessionUtils.hasUser(request);
    }

    private RedirectView badAuthentication(HttpServletRequest request, String message) {
        SessionUtils.setAuthenticationError(message, request);

        return REDIRECT_TO_LOGIN_PAGE;
    }

    private HashMap<String, String> getRequestHeaders(HttpServletRequest request) {
        HashMap<String, String> headers = new HashMap<>();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = (String) headerNames.nextElement();
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
