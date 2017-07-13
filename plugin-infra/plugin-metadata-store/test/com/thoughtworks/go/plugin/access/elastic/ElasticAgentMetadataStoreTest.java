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

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import org.junit.After;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticAgentMetadataStoreTest {
    @After
    public void tearDown() throws Exception {
        ElasticAgentMetadataStore.instance().clear();
    }

    @Test
    public void shouldListPluginsSupportingStatusReport() throws Exception {
        final ElasticAgentMetadataStore instance = ElasticAgentMetadataStore.instance();

        instance.setPluginInfo(new ElasticAgentPluginInfo(mockPluginDescriptor("plugin-1"), null, null, null, false));
        instance.setPluginInfo(new ElasticAgentPluginInfo(mockPluginDescriptor("plugin-2"), null, null, null, true));
        instance.setPluginInfo(new ElasticAgentPluginInfo(mockPluginDescriptor("plugin-3"), null, null, null, false));

        final Set<ElasticAgentPluginInfo> elasticAgentPluginInfos = instance.pluginsSupportingStatusReports();

        assertThat(elasticAgentPluginInfos, hasSize(1));
        assertTrue(elasticAgentPluginInfos.toArray(new ElasticAgentPluginInfo[0])[0].supportsStatusReport());
    }

    private PluginDescriptor mockPluginDescriptor(String pluginId) {
        final PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginDescriptor.id()).thenReturn(pluginId);
        return pluginDescriptor;
    }
}