/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokeStaleAccessTokenServiceTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private AccessTokenService accessTokenService;

    private RevokeStaleAccessTokenService service;

    private SecurityAuthConfig authConfig1;
    private SecurityAuthConfig authConfig2;

    private AccessToken.AccessTokenWithDisplayValue authConfig1_token1;
    private AccessToken.AccessTokenWithDisplayValue authConfig1_token2;
    private AccessToken.AccessTokenWithDisplayValue authConfig2_token1;


    @BeforeEach
    void setUp() {
        service = new RevokeStaleAccessTokenService(goConfigService, accessTokenService);

        authConfig1 = new SecurityAuthConfig("authConfig1", "ldap");
        authConfig2 = new SecurityAuthConfig("authConfig2", "ldap");

        authConfig1_token1 = AccessToken.create(null, null, "authConfig1", new TestingClock());
        authConfig1_token1.setId(0);
        authConfig1_token2 = AccessToken.create(null, null, "authConfig1", new TestingClock());
        authConfig1_token2.setId(1);
        authConfig2_token1 = AccessToken.create(null, null, "authConfig2", new TestingClock());
        authConfig2_token1.setId(2);
    }

    @Test
    void shouldDoNothingWhenAuthConfigHasNotChangedDuringStartup() {
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.server().security().securityAuthConfigs().add(authConfig1);
        config.server().security().securityAuthConfigs().add(authConfig2);

        when(goConfigService.getConfigForEditing()).thenReturn(config);
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn(Arrays.asList(authConfig1_token1, authConfig1_token2, authConfig2_token1));

        service.initialize();
        verify(accessTokenService, never()).revokeAccessTokenByGoCD(anyInt(), anyString());
    }

    @Test
    void shouldDoNothingWhenThereAreNoAccessTokens() {
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.server().security().securityAuthConfigs().add(authConfig1);
        config.server().security().securityAuthConfigs().add(authConfig2);

        when(goConfigService.getConfigForEditing()).thenReturn(config);
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn(Collections.emptyList());

        service.initialize();
        verify(accessTokenService, never()).revokeAccessTokenByGoCD(anyInt(), anyString());
    }

    @Test
    void shouldRevokeAllTheTokensWhenAllTheAuthConfigsAreRemovedDuringStartup() {
        BasicCruiseConfig config = new BasicCruiseConfig();
        when(goConfigService.getConfigForEditing()).thenReturn(config);
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn(Arrays.asList(authConfig1_token1, authConfig1_token2, authConfig2_token1));

        service.initialize();

        verify(accessTokenService).revokeAccessTokenByGoCD(0, "Revoked by GoCD: The authorization configuration 'authConfig1' referenced from the current access token is deleted from GoCD.");
        verify(accessTokenService).revokeAccessTokenByGoCD(1, "Revoked by GoCD: The authorization configuration 'authConfig1' referenced from the current access token is deleted from GoCD.");
        verify(accessTokenService).revokeAccessTokenByGoCD(2, "Revoked by GoCD: The authorization configuration 'authConfig2' referenced from the current access token is deleted from GoCD.");
    }

    @Test
    void shouldRevokeTheTokensWhenTheBelongingAuthConfigIsRemovedDuringStartup() {
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.server().security().securityAuthConfigs().add(authConfig1);

        when(goConfigService.getConfigForEditing()).thenReturn(config);
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn(Arrays.asList(authConfig1_token1, authConfig1_token2, authConfig2_token1));

        service.initialize();

        verify(accessTokenService).revokeAccessTokenByGoCD(2, "Revoked by GoCD: The authorization configuration 'authConfig2' referenced from the current access token is deleted from GoCD.");
    }

    // on config update
    @Test
    void shouldDoNothingWhenAuthConfigHasNotChangedDuringFullConfigUpdate() {
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.server().security().securityAuthConfigs().add(authConfig1);
        config.server().security().securityAuthConfigs().add(authConfig2);

        when(goConfigService.getConfigForEditing()).thenReturn(config);
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn(Arrays.asList(authConfig1_token1, authConfig1_token2, authConfig2_token1));

        service.initialize();
        verify(accessTokenService, never()).revokeAccessTokenByGoCD(anyInt(), anyString());

        service.onConfigChange(config);

        verify(accessTokenService, never()).revokeAccessTokenByGoCD(anyInt(), anyString());
    }


    @Test
    void shouldRevokeAllTheTokensWhenAllTheAuthConfigsAreRemovedDuringFullConfigUpdate() {
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.server().security().securityAuthConfigs().add(authConfig1);
        config.server().security().securityAuthConfigs().add(authConfig2);

        when(goConfigService.getConfigForEditing()).thenReturn(config);
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn(Arrays.asList(authConfig1_token1, authConfig1_token2, authConfig2_token1));

        service.initialize();
        verify(accessTokenService, never()).revokeAccessTokenByGoCD(anyInt(), anyString());

        BasicCruiseConfig updated = new BasicCruiseConfig();
        updated.server().security().securityAuthConfigs().add(authConfig1);

        service.onConfigChange(updated);

        verify(accessTokenService).revokeAccessTokenByGoCD(2, "Revoked by GoCD: The authorization configuration 'authConfig2' referenced from the current access token is deleted from GoCD.");
    }

    // on entity update
    @Test
    void shouldDoNothingWhenAuthConfigHasNotChangedDuringUpdateAuthConfig() {
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.server().security().securityAuthConfigs().add(authConfig1);
        config.server().security().securityAuthConfigs().add(authConfig2);

        when(goConfigService.getConfigForEditing()).thenReturn(config);
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn(Arrays.asList(authConfig1_token1, authConfig1_token2, authConfig2_token1));

        service.initialize();
        verify(accessTokenService, never()).revokeAccessTokenByGoCD(anyInt(), anyString());

        service.onEntityConfigChange(new SecurityAuthConfig());

        verify(accessTokenService, never()).revokeAccessTokenByGoCD(anyInt(), anyString());
    }

    @Test
    void shouldRevokeTheTokensWhenTheAuthConfigsIsDeletedFromTheConfig() {
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.server().security().securityAuthConfigs().add(authConfig1);
        config.server().security().securityAuthConfigs().add(authConfig2);

        when(goConfigService.getConfigForEditing()).thenReturn(config);
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn(Arrays.asList(authConfig1_token1, authConfig1_token2, authConfig2_token1));

        service.initialize();
        verify(accessTokenService, never()).revokeAccessTokenByGoCD(anyInt(), anyString());

        BasicCruiseConfig updated = new BasicCruiseConfig();
        updated.server().security().securityAuthConfigs().add(authConfig1);

        when(goConfigService.getConfigForEditing()).thenReturn(updated);

        service.onEntityConfigChange(new SecurityAuthConfig());

        verify(accessTokenService).revokeAccessTokenByGoCD(2, "Revoked by GoCD: The authorization configuration 'authConfig2' referenced from the current access token is deleted from GoCD.");
    }
}
