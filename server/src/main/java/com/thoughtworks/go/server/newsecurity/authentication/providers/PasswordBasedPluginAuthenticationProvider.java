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
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Component
public class PasswordBasedPluginAuthenticationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordBasedPluginAuthenticationProvider.class);

    private final AuthorizationExtension authorizationExtension;
    private final AuthorityGranter authorityGranter;
    private final GoConfigService configService;
    private final PluginRoleService pluginRoleService;
    private final UserService userService;
    private final AuthorizationMetadataStore store;

    @Autowired
    public PasswordBasedPluginAuthenticationProvider(AuthorizationExtension authorizationExtension, AuthorityGranter authorityGranter, GoConfigService configService,
                                                     PluginRoleService pluginRoleService, UserService userService) {
        this.authorizationExtension = authorizationExtension;
        this.authorityGranter = authorityGranter;
        this.configService = configService;
        this.pluginRoleService = pluginRoleService;
        this.userService = userService;
        this.store = AuthorizationMetadataStore.instance();
    }

    public boolean hasPluginsForUsernamePasswordAuth() {
        return store.getPluginsThatSupportsPasswordBasedAuthentication().size() > 0;
    }

    public User authenticate(String username, String password) {
        if (StringUtils.isBlank(password)) {
            throw new BadCredentialsException("Empty Password");
        }

        com.thoughtworks.go.plugin.access.authorization.models.User user = getUserDetailsFromAuthorizationPlugins(username, password);

        if (user == null) {
            removeAnyAssociatedPluginRolesFor(username);
            throw new UsernameNotFoundException("Unable to authenticate user: " + username);
        }

        userService.addUserIfDoesNotExist(toDomainUser(user));
        return new GoUserPrinciple(user.getUsername(), user.getDisplayName(), "",
                authorityGranter.authorities(user.getUsername()), user.getUsername());
    }

    private com.thoughtworks.go.domain.User toDomainUser(com.thoughtworks.go.plugin.access.authorization.models.User user) {
        return new com.thoughtworks.go.domain.User(user.getUsername(), user.getDisplayName(), user.getEmailId());
    }

    private void removeAnyAssociatedPluginRolesFor(String username) {
        pluginRoleService.revokeAllRolesFor(username);
    }

    private com.thoughtworks.go.plugin.access.authorization.models.User getUserDetailsFromAuthorizationPlugins(String username, String password) {
        for (SecurityAuthConfig authConfig : configService.security().securityAuthConfigs()) {
            String pluginId = authConfig.getPluginId();

            if (!store.doesPluginSupportPasswordBasedAuthentication(pluginId)) {
                continue;
            }

            final List<PluginRoleConfig> roleConfigs = configService.security().getRoles().pluginRoleConfigsFor(authConfig.getId());

            try {
                LOGGER.debug("Authenticating user: `{}` using the authorization plugin: `{}`", username, pluginId);
                AuthenticationResponse response = authorizationExtension.authenticateUser(pluginId, username, password, singletonList(authConfig), roleConfigs);
                com.thoughtworks.go.plugin.access.authorization.models.User user = ensureDisplayNamePresent(response.getUser());
                if (user != null) {
                    pluginRoleService.updatePluginRoles(pluginId, user.getUsername(), CaseInsensitiveString.caseInsensitiveStrings(response.getRoles()));
                    LOGGER.debug("Successfully authenticated user: `{}` using the authorization plugin: `{}`", username, pluginId);
                    return user;
                }
            } catch (Exception e) {
                LOGGER.error("Error while authenticating user: `{}` using the authorization plugin: {} ", username, pluginId);
            }
            LOGGER.debug("Authentication failed for user: `{}` using the authorization plugin: `{}`", username, pluginId);
        }
        return null;
    }


    private com.thoughtworks.go.plugin.access.authorization.models.User ensureDisplayNamePresent(com.thoughtworks.go.plugin.access.authorization.models.User user) {
        if (user == null) {
            return null;
        }

        if (isNotBlank(user.getDisplayName())) {
            return user;
        }

        return new com.thoughtworks.go.plugin.access.authorization.models.User(user.getUsername(), user.getUsername(), user.getEmailId());
    }
}
