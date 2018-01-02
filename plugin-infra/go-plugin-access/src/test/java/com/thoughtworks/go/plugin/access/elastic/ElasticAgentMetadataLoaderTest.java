/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ElasticAgentMetadataLoaderTest {
    private ElasticAgentExtension extension;
    private ElasticAgentPluginInfoBuilder infoBuilder;
    private ElasticAgentMetadataStore metadataStore;
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        extension = mock(ElasticAgentExtension.class);
        infoBuilder = mock(ElasticAgentPluginInfoBuilder.class);
        metadataStore = mock(ElasticAgentMetadataStore.class);
        pluginManager = mock(PluginManager.class);
    }

    @Test
    public void shouldBeAPluginChangeListener() throws Exception {
        ElasticAgentMetadataLoader loader = new ElasticAgentMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);

        verify(pluginManager).addPluginChangeListener(eq(loader), any(GoPlugin.class.getClass()));
    }

    @Test
    public void onPluginLoaded_shouldAddPluginInfoToMetadataStore() throws Exception {
        GoPluginDescriptor descriptor =  new GoPluginDescriptor("plugin1", null, null, null, null, false);
        ElasticAgentMetadataLoader metadataLoader = new ElasticAgentMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        ElasticAgentPluginInfo pluginInfo = new ElasticAgentPluginInfo(descriptor, null, null, null, null);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(true);
        when(infoBuilder.pluginInfoFor(descriptor)).thenReturn(pluginInfo);

        metadataLoader.pluginLoaded(descriptor);

        verify(metadataStore).setPluginInfo(pluginInfo);
    }

    @Test
    public void onPluginLoaded_shouldIgnoreNonAuthorizationPlugins() throws Exception {
        GoPluginDescriptor descriptor =  new GoPluginDescriptor("plugin1", null, null, null, null, false);
        ElasticAgentMetadataLoader metadataLoader = new ElasticAgentMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(false);

        metadataLoader.pluginLoaded(descriptor);

        verifyZeroInteractions(infoBuilder);
        verifyZeroInteractions(metadataStore);
    }

    @Test
    public void onPluginUnloded_shouldRemoveTheCorrespondingPluginInfoFromStore() throws Exception {
        GoPluginDescriptor descriptor =  new GoPluginDescriptor("plugin1", null, null, null, null, false);
        ElasticAgentMetadataLoader metadataLoader = new ElasticAgentMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        ElasticAgentPluginInfo pluginInfo = new ElasticAgentPluginInfo(descriptor, null, null, null, null);

        metadataStore.setPluginInfo(pluginInfo);

        metadataLoader.pluginUnLoaded(descriptor);

        verify(metadataStore).remove(descriptor.id());
    }
}

