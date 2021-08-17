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
package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizationMetadataStoreTest {

    private AuthorizationMetadataStore store;
    private AuthorizationPluginInfo plugin1;
    private AuthorizationPluginInfo plugin2;
    private AuthorizationPluginInfo plugin3;

    @BeforeEach
    public void setUp() throws Exception {
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
        assertThat(pluginsThatSupportsWebBasedAuthentication.size(), is(2));
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains(plugin1), is(true));
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains(plugin3), is(true));
    }

    @Test
    public void shouldGetPluginsThatSupportPasswordBasedAuthorization() {
        Set<AuthorizationPluginInfo> pluginsThatSupportsWebBasedAuthentication = store.getPluginsThatSupportsPasswordBasedAuthentication();
        assertThat(pluginsThatSupportsWebBasedAuthentication.size(), is(1));
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains(plugin2), is(true));
    }

    @Test
    public void shouldGetPluginsThatSupportsGetUserRolesCall() {
        when(plugin1.getCapabilities().canGetUserRoles()).thenReturn(true);
        Set<String> pluginsThatSupportsGetUserRoles = store.getPluginsThatSupportsGetUserRoles();
        assertThat(pluginsThatSupportsGetUserRoles.size(), is(1));
        assertThat(pluginsThatSupportsGetUserRoles, contains(plugin1.getDescriptor().id()));
    }

    @Test
    public void shouldBeAbleToAnswerIfPluginSupportsPasswordBasedAuthentication() throws Exception {
        assertTrue(store.doesPluginSupportPasswordBasedAuthentication("password.plugin-2"));
        assertFalse(store.doesPluginSupportPasswordBasedAuthentication("web.plugin-1"));
    }

    @Test
    public void shouldBeAbleToAnswerIfPluginSupportsWebBasedAuthentication() throws Exception {
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
