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
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ui.basicauth.BasicProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebBasedPluginAuthenticationFilter extends BasicProcessingFilter {
    private static final Pattern LOGIN_REQUEST_PATTERN = Pattern.compile("^/go/plugin/([^\\s]+)/login$");
    private AuthorizationExtension authorizationExtension;
    private GoConfigService goConfigService;

    @Autowired
    public WebBasedPluginAuthenticationFilter(AuthorizationExtension authorizationExtension, GoConfigService goConfigService) {
        this.authorizationExtension = authorizationExtension;
        this.goConfigService = goConfigService;
    }

    @Override
    public void doFilterHttp(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain) throws IOException, ServletException {
        if(isWebBasedPluginLoginRequest(httpRequest)) {
            httpResponse.sendRedirect(identityProviderRedirectUrl(pluginId(httpRequest)));
        }

        super.doFilterHttp(httpRequest, httpResponse, chain);
    }

    private String pluginId(HttpServletRequest request) {
        Matcher matcher = LOGIN_REQUEST_PATTERN.matcher(request.getRequestURI());
        matcher.matches();

        return matcher.group(1);
    }

    private String identityProviderRedirectUrl(String pluginId) {
        List<SecurityAuthConfig> authConfigs = goConfigService.security().securityAuthConfigs().findByPluginId(pluginId);
        return this.authorizationExtension.getIdentityProviderRedirectUrl(pluginId, authConfigs);
    }

    private boolean isWebBasedPluginLoginRequest(HttpServletRequest request) {
        return LOGIN_REQUEST_PATTERN.matcher(request.getRequestURI()).matches();
    }
}
