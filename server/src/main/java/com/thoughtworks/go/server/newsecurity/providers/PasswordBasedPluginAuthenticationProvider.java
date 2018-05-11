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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Collections.singletonList;

@Component
public class PasswordBasedPluginAuthenticationProvider extends AbstractPluginAuthenticationProvider<UsernamePassword> {
    private final AuthorizationExtension authorizationExtension;
    private final AuthorityGranter authorityGranter;
    private final UserService userService;
    private final AuthorizationMetadataStore store;
    private final Clock clock;

    @Autowired
    public PasswordBasedPluginAuthenticationProvider(AuthorizationExtension authorizationExtension,
                                                     AuthorityGranter authorityGranter,
                                                     GoConfigService goConfigService,
                                                     PluginRoleService pluginRoleService,
                                                     UserService userService,
                                                     Clock clock) {
        super(goConfigService, pluginRoleService);
        this.authorizationExtension = authorizationExtension;
        this.authorityGranter = authorityGranter;
        this.userService = userService;
        this.store = AuthorizationMetadataStore.instance();
        this.clock = clock;
    }


    @Override
    protected String getUsername(AuthenticationToken<UsernamePassword> authenticationToken) {
        return authenticationToken.getCredentials().getUsername();
    }

    @Override
    protected List<SecurityAuthConfig> getSecurityAuthConfigsToAuthenticateWith(String pluginId) {
        return goConfigService.security().securityAuthConfigs();
    }

    @Override
    protected AuthenticationToken<UsernamePassword> authenticateUser(UsernamePassword usernamePassword,
                                                                     SecurityAuthConfig authConfig) {
        String username = usernamePassword.getUsername();
        String password = usernamePassword.getPassword();

        String pluginId = authConfig.getPluginId();

        try {
            if (!store.doesPluginSupportPasswordBasedAuthentication(pluginId)) {
                return null;
            }

            final List<PluginRoleConfig> roleConfigs = goConfigService.security().getRoles().pluginRoleConfigsFor(authConfig.getId());
            LOGGER.debug("Authenticating user: `{}` using the authorization plugin: `{}`", username, pluginId);
            AuthenticationResponse response = authorizationExtension.authenticateUser(pluginId, username, password, singletonList(authConfig), roleConfigs);
            com.thoughtworks.go.plugin.access.authorization.models.User user = ensureDisplayNamePresent(response.getUser());
            if (user != null) {
                userService.addUserIfDoesNotExist(toDomainUser(user));

                pluginRoleService.updatePluginRoles(pluginId, user.getUsername(), CaseInsensitiveString.caseInsensitiveStrings(response.getRoles()));
                LOGGER.debug("Successfully authenticated user: `{}` using the authorization plugin: `{}`", username, pluginId);

                final GoUserPrinciple goUserPrinciple = new GoUserPrinciple(user.getUsername(), user.getDisplayName(),
                        authorityGranter.authorities(user.getUsername()));

                return new AuthenticationToken<>(goUserPrinciple, usernamePassword, pluginId, clock.currentTimeMillis(), authConfig.getId());

            }
        } catch (Exception e) {
            LOGGER.error("Error while authenticating user: `{}` using the authorization plugin: {} ", username, pluginId);
        }
        LOGGER.debug("Authentication failed for user: `{}` using the authorization plugin: `{}`", username, pluginId);
        return null;
    }
}
