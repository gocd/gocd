/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.server.newsecurity.models.AccessTokenCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

@Component
public class AccessTokenBasedPluginAuthenticationProvider extends AbstractPluginAuthenticationProvider<AccessTokenCredential> {
    private final AuthorizationExtension authorizationExtension;
    private final UserService userService;
    private final AuthorizationMetadataStore store;
    private final Clock clock;

    @Autowired
    public AccessTokenBasedPluginAuthenticationProvider(AuthorizationExtension authorizationExtension,
                                                        AuthorityGranter authorityGranter,
                                                        GoConfigService goConfigService,
                                                        PluginRoleService pluginRoleService,
                                                        UserService userService,
                                                        Clock clock) {
        super(goConfigService, pluginRoleService, userService, authorityGranter);
        this.authorizationExtension = authorizationExtension;
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
        if (store.doesPluginSupportGetUserRolesCall(pluginId)) {
            return authorizationExtension.getUserRoles(pluginId, credentials.getAccessToken().getUsername(), singletonList(authConfig), pluginRoleConfigs);
        }

        com.thoughtworks.go.domain.User fetched = userService.findUserByName(credentials.getAccessToken().getUsername());
        User user = new User(fetched.getUsername().getUsername().toString(), fetched.getDisplayName(), fetched.getEmail());
        return new AuthenticationResponse(user, Collections.emptyList());
    }

    @Override
    protected AuthenticationToken<AccessTokenCredential> createAuthenticationToken(GoUserPrinciple userPrinciple,
                                                                                   AccessTokenCredential credentials,
                                                                                   String pluginId,
                                                                                   String authConfigId) {
        return new AuthenticationToken<>(userPrinciple, credentials, pluginId, clock.currentTimeMillis(), authConfigId);
    }
}
