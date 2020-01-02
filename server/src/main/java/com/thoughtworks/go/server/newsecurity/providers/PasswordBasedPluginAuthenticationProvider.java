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
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
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
    private final AuthorizationMetadataStore store;
    private final Clock clock;

    @Autowired
    public PasswordBasedPluginAuthenticationProvider(AuthorizationExtension authorizationExtension,
                                                     AuthorityGranter authorityGranter,
                                                     GoConfigService goConfigService,
                                                     PluginRoleService pluginRoleService,
                                                     UserService userService,
                                                     Clock clock) {
        super(goConfigService, pluginRoleService, userService, authorityGranter);
        this.authorizationExtension = authorizationExtension;
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
    protected boolean doesPluginSupportAuthentication(String pluginId) {
        return store.doesPluginSupportPasswordBasedAuthentication(pluginId);
    }

    @Override
    protected AuthenticationResponse authenticateWithExtension(String pluginId,
                                                               UsernamePassword credentials,
                                                               SecurityAuthConfig authConfig,
                                                               List<PluginRoleConfig> pluginRoleConfigs) {
        return authorizationExtension.authenticateUser(pluginId, credentials.getUsername(), credentials.getPassword(),
                singletonList(authConfig), pluginRoleConfigs);
    }

    @Override
    protected AuthenticationToken<UsernamePassword> createAuthenticationToken(GoUserPrinciple userPrinciple,
                                                                              UsernamePassword credentials,
                                                                              String pluginId,
                                                                              String authConfigId) {
        return new AuthenticationToken<>(userPrinciple, credentials, pluginId, clock.currentTimeMillis(), authConfigId);
    }
}
