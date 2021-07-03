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

import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class AuthorizationMetadataLoaderTest {

    private AuthorizationExtension extension;
    private AuthorizationPluginInfoBuilder infoBuilder;
    private AuthorizationMetadataStore metadataStore;
    private PluginManager pluginManager;

    @BeforeEach
    public void setUp() throws Exception {
        extension = mock(AuthorizationExtension.class);
        infoBuilder = mock(AuthorizationPluginInfoBuilder.class);
        metadataStore = mock(AuthorizationMetadataStore.class);
        pluginManager = mock(PluginManager.class);
    }

    @Test
    public void shouldBeAPluginChangeListener() throws Exception {
        AuthorizationMetadataLoader authorizationMetadataLoader = new AuthorizationMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);

        verify(pluginManager).addPluginChangeListener(eq(authorizationMetadataLoader));
    }

    @Test
    public void onPluginLoaded_shouldAddPluginInfoToMetadataStore() throws Exception {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        AuthorizationMetadataLoader metadataLoader = new AuthorizationMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(descriptor, null, null, null, null);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(true);
        when(infoBuilder.pluginInfoFor(descriptor)).thenReturn(pluginInfo);

        metadataLoader.pluginLoaded(descriptor);

        verify(metadataStore).setPluginInfo(pluginInfo);
    }

    @Test
    public void onPluginLoaded_shouldIgnoreNonAuthorizationPlugins() throws Exception {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        AuthorizationMetadataLoader metadataLoader = new AuthorizationMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(false);

        metadataLoader.pluginLoaded(descriptor);

        verifyNoMoreInteractions(infoBuilder);
        verifyNoMoreInteractions(metadataStore);
    }

    @Test
    public void onPluginUnloded_shouldRemoveTheCorrespondingPluginInfoFromStore() throws Exception {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        AuthorizationMetadataLoader metadataLoader = new AuthorizationMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(descriptor, null, null, null, null);

        metadataStore.setPluginInfo(pluginInfo);

        metadataLoader.pluginUnLoaded(descriptor);

        verify(metadataStore).remove(descriptor.id());
    }
}
