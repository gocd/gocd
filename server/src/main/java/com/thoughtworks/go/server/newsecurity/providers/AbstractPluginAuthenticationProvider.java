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

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.Credentials;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;

import java.util.List;

public abstract class AbstractPluginAuthenticationProvider<T extends Credentials> extends AbstractAuthenticationProvider<T> {

    protected final GoConfigService goConfigService;
    protected final PluginRoleService pluginRoleService;
    private final UserService userService;
    private final AuthorityGranter authorityGranter;

    AbstractPluginAuthenticationProvider(GoConfigService goConfigService,
                                         PluginRoleService pluginRoleService,
                                         UserService userService,
                                         AuthorityGranter authorityGranter) {
        this.goConfigService = goConfigService;
        this.pluginRoleService = pluginRoleService;
        this.userService = userService;
        this.authorityGranter = authorityGranter;
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

    protected abstract boolean doesPluginSupportAuthentication(String pluginId);

    protected abstract AuthenticationResponse authenticateWithExtension(String pluginId,
                                                                        T credentials,
                                                                        SecurityAuthConfig authConfig,
                                                                        List<PluginRoleConfig> pluginRoleConfigs);

    protected abstract AuthenticationToken<T> createAuthenticationToken(GoUserPrinciple userPrinciple,
                                                                        T credentials,
                                                                        String pluginId,
                                                                        String authConfigId);

    protected AuthenticationToken<T> authenticateUser(T credentials, SecurityAuthConfig authConfig) {
        String pluginId = authConfig.getPluginId();

        try {
            if (!doesPluginSupportAuthentication(pluginId)) {
                return null;
            }

            final List<PluginRoleConfig> roleConfigs = goConfigService.security().getRoles().pluginRoleConfigsFor(authConfig.getId());
            LOGGER.debug("Authenticating user using the authorization plugin: `{}`", pluginId);
            AuthenticationResponse response = authenticateWithExtension(pluginId, credentials, authConfig, roleConfigs);

            com.thoughtworks.go.plugin.access.authorization.models.User user = ensureDisplayNamePresent(response.getUser());
            if (user != null) {
                userService.addUserIfDoesNotExist(toDomainUser(user));

                pluginRoleService.updatePluginRoles(pluginId, user.getUsername(), CaseInsensitiveString.caseInsensitiveStrings(response.getRoles()));
                LOGGER.debug("Successfully authenticated user: `{}` using the authorization plugin: `{}`", user.getUsername(), pluginId);

                final GoUserPrinciple goUserPrinciple = new GoUserPrinciple(user.getUsername(), user.getDisplayName(),
                        authorityGranter.authorities(user.getUsername()));

                return createAuthenticationToken(goUserPrinciple, credentials, pluginId, authConfig.getId());

            }
        } catch (OnlyKnownUsersAllowedException e) {
            LOGGER.info("User {} is successfully authenticated. Auto register new user is disabled. Please refer https://docs.gocd.org/{}/configuration/dev_authentication.html#controlling-user-access", e.getUsername(), CurrentGoCDVersion.getInstance().goVersion());
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error while authenticating user using auth_config: {} with the authorization plugin: {} ", authConfig.getId(), pluginId);
        }
        LOGGER.debug("Authentication failed using the authorization plugin: `{}`", pluginId);
        return null;
    }
}
