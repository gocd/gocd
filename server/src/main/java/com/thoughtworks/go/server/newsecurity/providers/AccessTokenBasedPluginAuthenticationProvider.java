/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.server.exceptions.InvalidAccessTokenException;
import com.thoughtworks.go.server.newsecurity.models.AccessTokenCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.AuthorizationExtensionCacheService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AccessTokenBasedPluginAuthenticationProvider extends AbstractPluginAuthenticationProvider<AccessTokenCredential> {
    private final AuthorizationExtensionCacheService authorizationExtensionCacheService;
    private final UserService userService;
    private final Clock clock;
    private AuthorizationMetadataStore store;

    @Autowired
    public AccessTokenBasedPluginAuthenticationProvider(AuthorizationExtensionCacheService authorizationExtensionCacheService,
                                                        AuthorityGranter authorityGranter,
                                                        GoConfigService goConfigService,
                                                        PluginRoleService pluginRoleService,
                                                        UserService userService,
                                                        Clock clock) {
        super(goConfigService, pluginRoleService, userService, authorityGranter);
        this.authorizationExtensionCacheService = authorizationExtensionCacheService;
        this.userService = userService;
        this.store = AuthorizationMetadataStore.instance();
        this.clock = clock;
    }

    @Override
    protected String getUsername(AuthenticationToken<AccessTokenCredential> authenticationToken) {
        return authenticationToken.getUser().getUsername();
    }

    @Override
    protected List<SecurityAuthConfig> getSecurityAuthConfigsToAuthenticateWith(String pluginId) {
        return goConfigService.security().securityAuthConfigs();
    }

    @Override
    protected boolean doesPluginSupportAuthentication(String pluginId) {
        return true;
    }

    @Override
    protected AuthenticationResponse authenticateWithExtension(String pluginId,
                                                               AccessTokenCredential credentials,
                                                               SecurityAuthConfig authConfig,
                                                               List<PluginRoleConfig> pluginRoleConfigs) {
        String username = credentials.getAccessToken().getUsername();
        if (authorizationExtensionCacheService.isValidUser(pluginId, username, authConfig)) {
            List<String> roles = new ArrayList<>();
            if (store.doesPluginSupportGetUserRolesCall(pluginId)) {
                roles.addAll(authorizationExtensionCacheService.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs));
            }

            com.thoughtworks.go.domain.User fetched = userService.findUserByName(username);
            User user = new User(fetched.getUsername().getUsername().toString(), fetched.getDisplayName(), fetched.getEmail());
            return new AuthenticationResponse(user, roles);
        } else {
            String msg = String.format("Access Token belonging to the user has either been disabled, removed or expired. ", username, pluginId, authConfig.getId());
            throw new InvalidAccessTokenException(msg);
        }
    }

    //used only in tests
    @Deprecated
    public void setStore(AuthorizationMetadataStore store) {
        this.store = store;
    }

    @Override
    protected AuthenticationToken<AccessTokenCredential> createAuthenticationToken(GoUserPrinciple userPrinciple,
                                                                                   AccessTokenCredential credentials,
                                                                                   String pluginId,
                                                                                   String authConfigId) {
        return new AuthenticationToken<>(userPrinciple, credentials, pluginId, clock.currentTimeMillis(), authConfigId);
    }
}
