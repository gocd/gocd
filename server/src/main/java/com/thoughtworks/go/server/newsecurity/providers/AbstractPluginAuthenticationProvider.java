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

package com.thoughtworks.go.server.newsecurity.providers;

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.Credentials;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;

import java.util.List;

public abstract class AbstractPluginAuthenticationProvider<T extends Credentials> extends AbstractAuthenticationProvider<T> {

    protected final GoConfigService goConfigService;
    protected final PluginRoleService pluginRoleService;

    public AbstractPluginAuthenticationProvider(GoConfigService goConfigService,
                                                PluginRoleService pluginRoleService) {
        this.goConfigService = goConfigService;
        this.pluginRoleService = pluginRoleService;
    }

    @Override
    public AuthenticationToken<T> reauthenticate(AuthenticationToken<T> authenticationToken) {
        final String authConfigId = authenticationToken.getAuthConfigId();

        final T credentials = authenticationToken.getCredentials();

        final SecurityAuthConfig authConfig = goConfigService.security().securityAuthConfigs().find(authConfigId);

        AuthenticationToken<T> reAuthenticatedToken;
        if (authConfig == null) {
            reAuthenticatedToken = authenticate(credentials, authenticationToken.getPluginId());
        } else {
            reAuthenticatedToken = authenticateUser(credentials, authConfig);
        }

        if (reAuthenticatedToken == null) {
            removeAnyAssociatedPluginRolesFor(getUsername(authenticationToken));
        }

        return reAuthenticatedToken;

    }

    protected abstract String getUsername(AuthenticationToken<T> authenticationToken);

    private void removeAnyAssociatedPluginRolesFor(String username) {
        pluginRoleService.revokeAllRolesFor(username);
    }

    @Override
    public AuthenticationToken<T> authenticate(T credentials, String pluginId) {
        for (SecurityAuthConfig authConfig : getSecurityAuthConfigsToAuthenticateWith(pluginId)) {
            AuthenticationToken<T> authenticationToken = authenticateUser(credentials, authConfig);
            if (authenticationToken != null) {
                return authenticationToken;
            }
        }

        return null;
    }

    protected abstract List<SecurityAuthConfig> getSecurityAuthConfigsToAuthenticateWith(String pluginId);

    protected abstract AuthenticationToken<T> authenticateUser(T credentials, SecurityAuthConfig authConfig);
}
