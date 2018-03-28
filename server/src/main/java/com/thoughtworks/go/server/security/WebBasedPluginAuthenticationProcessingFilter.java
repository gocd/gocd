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


import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.security.providers.WebBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.security.tokens.PreAuthenticatedAuthenticationToken;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebBasedPluginAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private static final String AUTHENTICATE_ENDPOINT_REGEX = "/plugin/([\\w\\-.]+)/authenticate$";
    private static final String AUTHENTICATE_ENDPOINT_ANT_PATH = "/plugin/*/authenticate";
    private static final Pattern AUTHENTICATE_ENDPOINT_PATTERN = Pattern.compile(AUTHENTICATE_ENDPOINT_REGEX);

    private static final RequestMatcher REQUEST_MATCHER = new AndRequestMatcher(
            new AntPathRequestMatcher(AUTHENTICATE_ENDPOINT_ANT_PATH, null),
            new RequestMatcher() {
                @Override
                public boolean matches(HttpServletRequest request) {
                    return SecurityContextHolder.getContext().getAuthentication() == null;
                }
            }
    );

    private final AuthorizationExtension authorizationExtension;
    private final GoConfigService configService;

    @Autowired
    public WebBasedPluginAuthenticationProcessingFilter(AuthorizationExtension authorizationExtension, GoConfigService configService, WebBasedPluginAuthenticationProvider authenticationProvider) {
        super(REQUEST_MATCHER);
        this.authorizationExtension = authorizationExtension;
        this.configService = configService;
        setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/auth/login?login_error=1"));
        setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler("/"));
        setAuthenticationManager(new ProviderManager(Collections.singletonList(authenticationProvider)));

//        setInvalidateSessionOnSuccessfulAuthentication(true);
    }

    protected Map<String, String> fetchAuthorizationServerAccessToken(HttpServletRequest request) {
        String pluginId = pluginId(request);
        List<SecurityAuthConfig> authConfigs = configService.security().securityAuthConfigs().findByPluginId(pluginId);

        return authorizationExtension.fetchAccessToken(pluginId, getRequestHeaders(request), getParameterMap(request), authConfigs);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        PreAuthenticatedAuthenticationToken authRequest = new PreAuthenticatedAuthenticationToken(null,
                fetchAuthorizationServerAccessToken(request), pluginId(request));

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private String pluginId(HttpServletRequest request) {
        Matcher matcher = AUTHENTICATE_ENDPOINT_PATTERN.matcher(request.getServletPath());
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IllegalStateException();
        }
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
}
