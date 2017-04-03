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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.server.ui.plugins.ElasticPluginInfo;

public class ElasticAgentPluginInfoBuilder extends PluginConfigMetadataStoreBasedPluginInfoBuilder<ElasticPluginInfo, ElasticAgentMetadataStore> {

    public ElasticAgentPluginInfoBuilder(ElasticAgentMetadataStore store) {
        super(store);
    }

    @Override
    public ElasticPluginInfo pluginInfoFor(String pluginId) {
        ElasticAgentPluginInfo pluginInfo = store.getPluginInfo(pluginId);

        if(pluginInfo == null) {
            return null;
        }

        return new ElasticPluginInfo(pluginInfo.getDescriptor(), settings(pluginInfo.getProfileSettings()), image(pluginInfo.getImage()));
    }
}
