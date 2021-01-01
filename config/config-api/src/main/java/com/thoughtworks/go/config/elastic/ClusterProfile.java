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
package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.PluginProfile;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;

import java.util.Collection;

@ConfigTag("clusterProfile")
@ConfigCollection(value = ConfigurationProperty.class)
public class ClusterProfile extends PluginProfile {

    public ClusterProfile() {
        super();
    }

    public ClusterProfile(String id, String pluginId, ConfigurationProperty... props) {
        super(id, pluginId, props);
    }

    public ClusterProfile(String id, String pluginId, Collection<ConfigurationProperty> configProperties) {
        this(id, pluginId, configProperties.toArray(new ConfigurationProperty[0]));
    }

    @Override
    protected String getObjectDescription() {
        return "Cluster Profile";
    }

    @Override
    protected boolean isSecure(String key) {
        ElasticAgentPluginInfo pluginInfo = this.metadataStore().getPluginInfo(getPluginId());

        if (pluginInfo == null
                || pluginInfo.getClusterProfileSettings() == null
                || pluginInfo.getClusterProfileSettings().getConfigurations() == null
                || pluginInfo.getClusterProfileSettings().getConfiguration(key) == null) {
            return false;
        }

        return pluginInfo.getClusterProfileSettings().getConfiguration(key).isSecure();
    }

    @Override
    protected boolean hasPluginInfo() {
        return this.metadataStore().getPluginInfo(getPluginId()) != null;
    }

    private ElasticAgentMetadataStore metadataStore() {
        return ElasticAgentMetadataStore.instance();
    }
}
