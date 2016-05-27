/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.authentication;

import com.thoughtworks.go.plugin.access.authentication.model.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthenticationPluginRegistrarTest {
    @Mock
    private PluginManager pluginManager;
    @Mock
    private AuthenticationExtension authenticationExtension;
    @Mock
    private AuthenticationPluginRegistry authenticationPluginRegistry;

    private AuthenticationPluginRegistrar authenticationPluginRegistrar;

    @Before
    public void setUp() {
        initMocks(this);

        authenticationPluginRegistrar = new AuthenticationPluginRegistrar(pluginManager, authenticationExtension, authenticationPluginRegistry);
    }

    @Test
    public void shouldRegisterItselfAsPluginChangeListener() {
        verify(pluginManager).addPluginChangeListener(authenticationPluginRegistrar, GoPlugin.class);
    }

    @Test
    public void shouldRegisterAuthenticationPluginOnLoad() {
        String pluginId = "plugin-id-1";
        when(authenticationExtension.canHandlePlugin(pluginId)).thenReturn(true);
        AuthenticationPluginConfiguration configuration = new AuthenticationPluginConfiguration("plugin 1", "image 1", true, true);
        when(authenticationExtension.getPluginConfiguration(pluginId)).thenReturn(configuration);

        authenticationPluginRegistrar.pluginLoaded(getPluginDescriptor(pluginId));

        verify(authenticationPluginRegistry).registerPlugin(pluginId, configuration);
    }

    @Test
    public void shouldRegisterPluginOnLoadIfNotAuthenticationType() {
        String pluginId = "plugin-id-1";
        when(authenticationExtension.canHandlePlugin(pluginId)).thenReturn(false);

        authenticationPluginRegistrar.pluginLoaded(getPluginDescriptor(pluginId));

        verify(authenticationPluginRegistry, never()).registerPlugin(eq(pluginId), any(AuthenticationPluginConfiguration.class));
    }

    @Test
    public void shouldDeregisterAuthenticationPluginOnUnLoad() {
        String pluginId = "plugin-id-1";
        authenticationPluginRegistrar.pluginUnLoaded(getPluginDescriptor(pluginId));
        verify(authenticationPluginRegistry).deregisterPlugin(pluginId);
    }

    private GoPluginDescriptor getPluginDescriptor(String pluginId) {
        return new GoPluginDescriptor(pluginId, null, null, null, null, false);
    }
}
