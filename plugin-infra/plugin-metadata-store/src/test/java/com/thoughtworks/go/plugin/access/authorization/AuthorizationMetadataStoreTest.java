/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizationMetadataStoreTest {

    private AuthorizationMetadataStore store;
    private AuthorizationPluginInfo plugin1;
    private AuthorizationPluginInfo plugin2;
    private AuthorizationPluginInfo plugin3;

    @BeforeEach
    public void setUp() {
        store = new AuthorizationMetadataStore();
        plugin1 = pluginInfo("web.plugin-1", SupportedAuthType.Web);
        plugin2 = pluginInfo("password.plugin-2", SupportedAuthType.Password);
        plugin3 = pluginInfo("web.plugin-3", SupportedAuthType.Web);
        store.setPluginInfo(plugin1);
        store.setPluginInfo(plugin2);
        store.setPluginInfo(plugin3);
    }

    @Test
    public void shouldGetPluginsThatSupportWebBasedAuthorization() {
        Set<AuthorizationPluginInfo> pluginsThatSupportsWebBasedAuthentication = store.getPluginsThatSupportsWebBasedAuthentication();
        assertThat(pluginsThatSupportsWebBasedAuthentication.size()).isEqualTo(2);
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains(plugin1)).isEqualTo(true);
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains(plugin3)).isEqualTo(true);
    }

    @Test
    public void shouldGetPluginsThatSupportPasswordBasedAuthorization() {
        Set<AuthorizationPluginInfo> pluginsThatSupportsWebBasedAuthentication = store.getPluginsThatSupportsPasswordBasedAuthentication();
        assertThat(pluginsThatSupportsWebBasedAuthentication.size()).isEqualTo(1);
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains(plugin2)).isEqualTo(true);
    }

    @Test
    public void shouldGetPluginsThatSupportsGetUserRolesCall() {
        when(plugin1.getCapabilities().canGetUserRoles()).thenReturn(true);
        Set<String> pluginsThatSupportsGetUserRoles = store.getPluginsThatSupportsGetUserRoles();
        assertThat(pluginsThatSupportsGetUserRoles).containsExactly(plugin1.getDescriptor().id());
    }

    @Test
    public void shouldBeAbleToAnswerIfPluginSupportsPasswordBasedAuthentication() {
        assertTrue(store.doesPluginSupportPasswordBasedAuthentication("password.plugin-2"));
        assertFalse(store.doesPluginSupportPasswordBasedAuthentication("web.plugin-1"));
    }

    @Test
    public void shouldBeAbleToAnswerIfPluginSupportsWebBasedAuthentication() {
        assertTrue(store.doesPluginSupportWebBasedAuthentication("web.plugin-1"));
        assertFalse(store.doesPluginSupportWebBasedAuthentication("password.plugin-2"));
        assertTrue(store.doesPluginSupportWebBasedAuthentication("web.plugin-3"));
    }

    private AuthorizationPluginInfo pluginInfo(String pluginId, SupportedAuthType supportedAuthType) {
        AuthorizationPluginInfo pluginInfo = mock(AuthorizationPluginInfo.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginDescriptor.id()).thenReturn(pluginId);
        when(pluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        Capabilities capabilities = mock(Capabilities.class);
        when(capabilities.getSupportedAuthType()).thenReturn(supportedAuthType);
        when(pluginInfo.getCapabilities()).thenReturn(capabilities);
        return pluginInfo;
    }
}
