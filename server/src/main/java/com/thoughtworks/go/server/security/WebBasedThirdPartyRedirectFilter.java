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
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.web.SiteUrlProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebBasedThirdPartyRedirectFilter implements Filter {
    private static final Pattern LOGIN_REQUEST_PATTERN = Pattern.compile("^/go/plugin/([\\w\\-.]+)/login$");
    private AuthorizationExtension authorizationExtension;
    private GoConfigService goConfigService;
    private SiteUrlProvider siteUrlProvider;
    private String DEFAULT_TARGET_URL = "/";

    @Autowired
    public WebBasedThirdPartyRedirectFilter(AuthorizationExtension authorizationExtension, GoConfigService goConfigService,
                                            SiteUrlProvider siteUrlProvider) {
        this.authorizationExtension = authorizationExtension;
        this.goConfigService = goConfigService;
        this.siteUrlProvider = siteUrlProvider;
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest httpRequest, ServletResponse httpResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) httpResponse;
        HttpServletRequest request = (HttpServletRequest) httpRequest;
        if (isWebBasedPluginLoginRequest(request)) {
            String redirectUrl = isAuthenticated() ? DEFAULT_TARGET_URL : authorizationServerUrl(pluginId(request), siteUrlProvider.siteUrl(request));
            response.sendRedirect(redirectUrl);
            return;
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    @Override
    public void destroy() {

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
}
