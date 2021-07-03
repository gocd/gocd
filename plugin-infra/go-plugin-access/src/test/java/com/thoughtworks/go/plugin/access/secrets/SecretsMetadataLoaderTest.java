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
package com.thoughtworks.go.plugin.access.secrets;

import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class SecretsMetadataLoaderTest {

    private SecretsExtension extension;
    private SecretsPluginInfoBuilder infoBuilder;
    private SecretsMetadataStore metadataStore;
    private PluginManager pluginManager;

    @BeforeEach
    public void setUp() throws Exception {
        extension = mock(SecretsExtension.class);
        infoBuilder = mock(SecretsPluginInfoBuilder.class);
        metadataStore = mock(SecretsMetadataStore.class);
        pluginManager = mock(PluginManager.class);
    }

    @Test
    public void shouldBeAPluginChangeListener() {
        SecretsMetadataLoader analyticsMetadataLoader = new SecretsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);

        verify(pluginManager).addPluginChangeListener(eq(analyticsMetadataLoader));
    }

    @Test
    public void onPluginLoaded_shouldAddPluginInfoToMetadataStore() {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        SecretsMetadataLoader metadataLoader = new SecretsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        SecretsPluginInfo pluginInfo = new SecretsPluginInfo(descriptor, null,null);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(true);
        when(infoBuilder.pluginInfoFor(descriptor)).thenReturn(pluginInfo);

        metadataLoader.pluginLoaded(descriptor);

        verify(metadataStore).setPluginInfo(pluginInfo);
    }

    @Test
    public void onPluginLoaded_shouldIgnoreNonSecretsPlugins() {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        SecretsMetadataLoader metadataLoader = new SecretsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(false);

        metadataLoader.pluginLoaded(descriptor);

        verifyNoMoreInteractions(infoBuilder);
        verifyNoMoreInteractions(metadataStore);
    }

    @Test
    public void onPluginUnloaded_shouldRemoveTheCorrespondingPluginInfoFromStore() {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        SecretsMetadataLoader metadataLoader = new SecretsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        SecretsPluginInfo pluginInfo = new SecretsPluginInfo(descriptor, null,null);

        metadataStore.setPluginInfo(pluginInfo);

        metadataLoader.pluginUnLoaded(descriptor);

        verify(metadataStore).remove(descriptor.id());
    }
}
