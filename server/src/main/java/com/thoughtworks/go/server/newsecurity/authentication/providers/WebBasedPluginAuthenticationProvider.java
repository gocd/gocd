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

package com.thoughtworks.go.server.newsecurity.authentication.providers;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@Component
public class WebBasedPluginAuthenticationProvider {
    private final AuthorizationExtension authorizationExtension;
    private final GoConfigService goConfigService;
    private final PluginRoleService pluginRoleService;

    @Autowired
    public WebBasedPluginAuthenticationProvider(AuthorizationExtension authorizationExtension, GoConfigService goConfigService, PluginRoleService pluginRoleService) {
        this.authorizationExtension = authorizationExtension;
        this.goConfigService = goConfigService;
        this.pluginRoleService = pluginRoleService;
    }

    public String getAuthorizationServerUrl(String pluginId, String rootURL) {
        if (goConfigService.serverConfig().hasAnyUrlConfigured()) {
            rootURL = getRootUrl(goConfigService.serverConfig().getSiteUrlPreferablySecured().getUrl());
        }
        return authorizationExtension.getAuthorizationServerUrl(pluginId, getAuthConfigs(pluginId), rootURL);
    }

    private String getRootUrl(String string) {
        try {
            final URL url = new URL(string);
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), "").toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> fetchAccessToken(String pluginId, Map<String, String> requestHeaders, Map<String, String> parameterMap) {
        return authorizationExtension.fetchAccessToken(pluginId, requestHeaders, parameterMap, getAuthConfigs(pluginId));
    }

    private List<SecurityAuthConfig> getAuthConfigs(String pluginId) {
        return goConfigService.security().securityAuthConfigs().findByPluginId(pluginId);
    }

    public AuthenticationResponse authenticateUser(String pluginId, Map<String, String> accessToken) {
        AuthenticationResponse response = null;
        for (SecurityAuthConfig authConfig : getAuthConfigs(pluginId)) {
            response = authorizationExtension.authenticateUser(pluginId, accessToken,
                    Collections.singletonList(authConfig), pluginRoleConfigsForAuthConfig(authConfig.getId()));

            if (isAuthenticated(response)) {
                assignRoles(pluginId, response);
                break;
            }
        }

        return response;
    }

    private void assignRoles(String pluginId, AuthenticationResponse response) {
        pluginRoleService.updatePluginRoles(pluginId, response.getUser().getUsername(), CaseInsensitiveString.caseInsensitiveStrings(response.getRoles()));
    }

    private boolean isAuthenticated(AuthenticationResponse response) {
        return response != null && response.getUser() != null && isNotBlank(response.getUser().getUsername());
    }

    private List<PluginRoleConfig> pluginRoleConfigsForAuthConfig(String authConfigId) {
        return goConfigService.security().getRoles().pluginRoleConfigsFor(authConfigId);
    }

}
