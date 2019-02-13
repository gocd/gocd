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

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import com.thoughtworks.go.server.exceptions.InvalidAccessTokenException;
import com.thoughtworks.go.server.newsecurity.models.AccessTokenCredential;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;

import static com.thoughtworks.go.helper.AccessTokenMother.randomAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class AccessTokenBasedPluginAuthenticationProviderTest {
    private final String pluginId = "pluginId";
    private final String authConfigId = "auth-config-id";
    private final String accessTokenName = "token1";


    @Mock
    private AuthorizationExtension authorizationExtension;
    @Mock
    private AuthorityGranter authorityGranter;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PluginRoleService pluginRoleService;
    @Mock
    private UserService userService;
    @Mock
    private Clock clock;
    @Mock
    private AuthorizationMetadataStore store;

    private AccessTokenBasedPluginAuthenticationProvider provider;
    private final SecurityAuthConfig authConfig = new SecurityAuthConfig();
    private final AccessTokenCredential credentials = new AccessTokenCredential(randomAccessToken());

    @BeforeEach
    void setUp() {
        initMocks(this);
        provider = new AccessTokenBasedPluginAuthenticationProvider(authorizationExtension, authorityGranter, goConfigService, pluginRoleService, userService, clock);
        provider.setStore(store);

        authConfig.setId(authConfigId);
    }

    @Test
    void shouldReturnRolesFetchedForTheUserFromThePluginForTheProvidedAuthConfig() {
        String username = credentials.getAccessToken().getUsername();
        AuthenticationResponse responseToSend = new AuthenticationResponse(null, Collections.emptyList());

        when(authorizationExtension.isValidUser(pluginId, username, authConfig)).thenReturn(true);
        when(store.doesPluginSupportGetUserRolesCall(pluginId)).thenReturn(true);
        when(authorizationExtension.getUserRoles(pluginId, username, authConfig, null)).thenReturn(responseToSend);

        AuthenticationResponse actual = provider.authenticateWithExtension(pluginId, credentials, authConfig, null);

        assertThat(actual).isEqualTo(responseToSend);
    }

    @Test
    void shouldReturnAuthenticationResponseFetchingUsersFromTheDBWhenPluginDoesNotSupportGetRolesCapability() {
        String username = credentials.getAccessToken().getUsername();
        User userToOperate = new User(username);
        AuthenticationResponse responseToSend = new AuthenticationResponse(new com.thoughtworks.go.plugin.domain.authorization.User(userToOperate.getUsername().getUsername().toString(), userToOperate.getDisplayName(), userToOperate.getEmail()), Collections.emptyList());

        when(authorizationExtension.isValidUser(pluginId, username, authConfig)).thenReturn(true);
        when(store.doesPluginSupportGetUserRolesCall(pluginId)).thenReturn(false);
        when(userService.findUserByName(username)).thenReturn(userToOperate);

        AuthenticationResponse actual = provider.authenticateWithExtension(pluginId, credentials, authConfig, null);

        assertThat(actual).isEqualTo(responseToSend);
    }

    @Test
    void itShouldThrowErrorWhenAccessTokenBelongingTheUserDoesNotExists() {
        when(authorizationExtension.isValidUser(pluginId, credentials.getAccessToken().getUsername(), authConfig)).thenReturn(false);

        InvalidAccessTokenException exception = assertThrows(InvalidAccessTokenException.class, () -> {
            provider.authenticateWithExtension(pluginId, credentials, authConfig, null);
        });

        assertThat(exception.getMessage()).startsWith("Invalid Personal Access Token. Access Token belonging to the user has either been disabled, removed or expired.");
    }
}
