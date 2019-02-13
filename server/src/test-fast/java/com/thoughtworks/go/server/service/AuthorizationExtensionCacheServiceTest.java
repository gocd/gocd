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

package com.thoughtworks.go.server.service;

import com.google.common.base.Ticker;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.listener.SecurityConfigChangeListener;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthorizationExtensionCacheServiceTest {
    private final String pluginId = "pluginId";
    private final String username = "username";
    private final SecurityAuthConfig authConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");
    private final FakeTicker ticker = new FakeTicker();

    @Mock
    private AuthorizationExtension authorizationExtension;
    @Mock
    private GoConfigService goConfigService;

    AuthorizationExtensionCacheService service;

    @BeforeEach
    void setUp() {
        initMocks(this);
        service = new AuthorizationExtensionCacheService(goConfigService, authorizationExtension, ticker);
    }

    @Test
    void shouldRegisterSecurityConfigChangeListenerWithGoConfigService() {
        verify(goConfigService, times(1)).register(any(SecurityConfigChangeListener.class));
    }

    @Test
    void shouldAskAuthorizationExtensionWhetherIsUserIsValid() {
        when(authorizationExtension.isValidUser(pluginId, username, authConfig)).thenReturn(false);
        boolean validUser = service.isValidUser(pluginId, username, authConfig);

        assertThat(validUser).isFalse();
        verify(authorizationExtension, times(1)).isValidUser(pluginId, username, authConfig);
    }

    @Test
    void shouldLoadFromCacheOnSubsequentCallsToCheckIsUserIsValid() {
        when(authorizationExtension.isValidUser(pluginId, username, authConfig)).thenReturn(false);
        boolean validUser = service.isValidUser(pluginId, username, authConfig);
        assertThat(validUser).isFalse();

        validUser = service.isValidUser(pluginId, username, authConfig);
        assertThat(validUser).isFalse();

        verify(authorizationExtension, times(1)).isValidUser(pluginId, username, authConfig);
    }

    @Test
    void shouldAskExtensionAgainWhenCacheExpires() {
        when(authorizationExtension.isValidUser(pluginId, username, authConfig)).thenReturn(false);
        boolean validUser = service.isValidUser(pluginId, username, authConfig);
        assertThat(validUser).isFalse();

        validUser = service.isValidUser(pluginId, username, authConfig);
        assertThat(validUser).isFalse();

        verify(authorizationExtension, times(1)).isValidUser(pluginId, username, authConfig);

        ticker.advance(31, TimeUnit.MINUTES);

        validUser = service.isValidUser(pluginId, username, authConfig);
        assertThat(validUser).isFalse();

        verify(authorizationExtension, times(2)).isValidUser(pluginId, username, authConfig);
    }

    @Test
    void shouldAskAuthorizationExtensionToGetUserRoles() {
        List<PluginRoleConfig> pluginRoleConfigs = Collections.emptyList();
        when(authorizationExtension.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs)).thenReturn(Collections.emptyList());

        List<String> actualResponse = service.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
        assertThat(actualResponse).isEqualTo(Collections.emptyList());

        verify(authorizationExtension, times(1)).getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
    }

    @Test
    void shouldLoadFromCacheOnSubsequentCallsToGetUserRoles() {
        List<PluginRoleConfig> pluginRoleConfigs = Collections.emptyList();
        AuthenticationResponse response = new AuthenticationResponse(null, Collections.emptyList());
        when(authorizationExtension.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs)).thenReturn(Collections.emptyList());

        List<String> actualResponse = service.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
        assertThat(actualResponse).isEqualTo(response);

        actualResponse = service.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
        assertThat(actualResponse).isEqualTo(response);

        verify(authorizationExtension, times(1)).getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
    }

    @Test
    void shouldAskExtensionAgainToGetUserRolesWhenCacheExpires() {
        List<PluginRoleConfig> pluginRoleConfigs = Collections.emptyList();
        when(authorizationExtension.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs)).thenReturn(Collections.emptyList());

        List<String> actualResponse = service.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
        assertThat(actualResponse).isEqualTo(Collections.emptyList());

        actualResponse = service.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
        assertThat(actualResponse).isEqualTo(Collections.emptyList());

        verify(authorizationExtension, times(1)).getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);

        ticker.advance(31, TimeUnit.MINUTES);

        actualResponse = service.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
        assertThat(actualResponse).isEqualTo(Collections.emptyList());

        verify(authorizationExtension, times(2)).getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
    }

    @Test
    void shouldInvalidateGetUserRolesCacheWhenSecurityConfigIsChanged() {
        ArgumentCaptor<SecurityConfigChangeListener> captor = ArgumentCaptor.forClass(SecurityConfigChangeListener.class);
        verify(goConfigService, times(1)).register(captor.capture());
        SecurityConfigChangeListener listener = captor.getValue();

        List<PluginRoleConfig> pluginRoleConfigs = Collections.emptyList();
        when(authorizationExtension.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs)).thenReturn(Collections.emptyList());

        List<String> actualResponse = service.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
        assertThat(actualResponse).isEqualTo(Collections.emptyList());

        listener.onEntityConfigChange(new Object());

        actualResponse = service.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
        assertThat(actualResponse).isEqualTo(Collections.emptyList());

        verify(authorizationExtension, times(2)).getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
    }

    @Test
    void shouldInvalidateIsValidUserCacheWhenSecurityConfigIsChanged() {
        ArgumentCaptor<SecurityConfigChangeListener> captor = ArgumentCaptor.forClass(SecurityConfigChangeListener.class);
        verify(goConfigService, times(1)).register(captor.capture());
        SecurityConfigChangeListener listener = captor.getValue();

        when(authorizationExtension.isValidUser(pluginId, username, authConfig)).thenReturn(false);
        boolean validUser = service.isValidUser(pluginId, username, authConfig);
        assertThat(validUser).isFalse();

        listener.onEntityConfigChange(new Object());

        validUser = service.isValidUser(pluginId, username, authConfig);
        assertThat(validUser).isFalse();

        verify(authorizationExtension, times(2)).isValidUser(pluginId, username, authConfig);
    }


    class FakeTicker extends Ticker {
        private final AtomicLong nanos = new AtomicLong();

        void advance(long time, TimeUnit timeUnit) {
            nanos.addAndGet(timeUnit.toNanos(time));
        }

        public long read() {
            return nanos.getAndAdd(0);
        }
    }
}
