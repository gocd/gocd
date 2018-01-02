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
import com.thoughtworks.go.server.web.SiteUrlProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.SpringSecurityFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebBasedAuthenticationFilter extends SpringSecurityFilter {
    private static final Pattern LOGIN_REQUEST_PATTERN = Pattern.compile("^/go/plugin/([\\w\\-.]+)/login$");
    private AuthorizationExtension authorizationExtension;
    private GoConfigService goConfigService;
    private SiteUrlProvider siteUrlProvider;
    private String DEFAULT_TARGET_URL = "/";

    @Autowired
    public WebBasedAuthenticationFilter(AuthorizationExtension authorizationExtension, GoConfigService goConfigService,
                                        SiteUrlProvider siteUrlProvider) {
        this.authorizationExtension = authorizationExtension;
        this.goConfigService = goConfigService;
        this.siteUrlProvider = siteUrlProvider;
    }

    @Override
    public void doFilterHttp(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain) throws IOException, ServletException {
        if(isWebBasedPluginLoginRequest(httpRequest)) {
            String redirectUrl = isAuthenticated() ? DEFAULT_TARGET_URL : authorizationServerUrl(pluginId(httpRequest), siteUrlProvider.siteUrl(httpRequest));
            httpResponse.sendRedirect(redirectUrl);
            return;
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    private String pluginId(HttpServletRequest request) {
        Matcher matcher = LOGIN_REQUEST_PATTERN.matcher(request.getRequestURI());
        matcher.matches();

        return matcher.group(1);
    }

    private String authorizationServerUrl(String pluginId, String siteUrl) {
        List<SecurityAuthConfig> authConfigs = goConfigService.security().securityAuthConfigs().findByPluginId(pluginId);
        return this.authorizationExtension.getAuthorizationServerUrl(pluginId, authConfigs, siteUrl);
    }

    private boolean isWebBasedPluginLoginRequest(HttpServletRequest request) {
        return LOGIN_REQUEST_PATTERN.matcher(request.getRequestURI()).matches();
    }

    private boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Override
    public int getOrder() {
        return FilterChainOrder.PRE_AUTH_FILTER - 1;
    }
}
