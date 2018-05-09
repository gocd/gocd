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


import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.security.tokens.PreAuthenticatedAuthenticationToken;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.security.ui.FilterChainOrder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreAuthenticatedRequestsProcessingFilter extends AbstractProcessingFilter {
    private final AuthorizationExtension authorizationExtension;
    private final GoConfigService configService;

    @Autowired
    public PreAuthenticatedRequestsProcessingFilter(AuthorizationExtension authorizationExtension, GoConfigService configService) {
        this.authorizationExtension = authorizationExtension;
        this.configService = configService;
    }

    protected Map<String, String> fetchAuthorizationServerAccessToken(HttpServletRequest request) {
        String pluginId = pluginId(request);
        List<SecurityAuthConfig> authConfigs = configService.security().securityAuthConfigs().findByPluginId(pluginId);

        return authorizationExtension.fetchAccessToken(pluginId, getRequestHeaders(request), getParameterMap(request), authConfigs);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request) throws AuthenticationException {
        PreAuthenticatedAuthenticationToken authRequest = new PreAuthenticatedAuthenticationToken(null,
                fetchAuthorizationServerAccessToken(request), pluginId(request));

        Authentication authResult = this.getAuthenticationManager().authenticate(authRequest);

        return authResult;
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return isPreAuthenticationRequest(request);
    }

    @Override
    public String getDefaultFilterProcessesUrl() {
        return null;
    }

    @Override
    public int getOrder() {
        return FilterChainOrder.PRE_AUTH_FILTER;
    }

    private boolean isPreAuthenticationRequest(HttpServletRequest request) {
        return SecurityContextHolder.getContext().getAuthentication() == null &&
                Pattern.compile(getFilterProcessesUrl()).matcher(request.getRequestURI()).matches();
    }

    private String pluginId(HttpServletRequest request) {
        Matcher matcher = Pattern.compile(getFilterProcessesUrl()).matcher(request.getRequestURI());
        matcher.matches();
        return matcher.group(1);
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
