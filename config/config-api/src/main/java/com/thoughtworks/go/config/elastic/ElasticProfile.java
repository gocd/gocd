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

package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.PluginProfile;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

@ConfigTag("profile")
@ConfigCollection(value = ConfigurationProperty.class)
public class ElasticProfile extends PluginProfile {

    @ConfigAttribute(value = "clusterProfileId", allowNull = false)
    protected String clusterProfileId;

    public ElasticProfile() {
        super();
    }

    public ElasticProfile(String id, String pluginId, String clusterProfileId, ConfigurationProperty... props) {
        super(id, pluginId, props);
        this.clusterProfileId = StringUtils.isBlank(clusterProfileId) ? "default" : clusterProfileId;
    }

    public ElasticProfile(String id, String pluginId, String clusterProfileId, Collection<ConfigurationProperty> configProperties) {
        this(id, pluginId, clusterProfileId, configProperties.toArray(new ConfigurationProperty[0]));
    }

    public String getClusterProfileId() {
        return clusterProfileId;
    }

    @Override
    protected String getObjectDescription() {
        return "Elastic agent profile";
    }

    @Override
    protected boolean isSecure(String key) {
        ElasticAgentPluginInfo pluginInfo = this.metadataStore().getPluginInfo(getPluginId());

        if (pluginInfo == null
                || pluginInfo.getProfileSettings() == null
                || pluginInfo.getProfileSettings().getConfiguration(key) == null) {
            return false;
        }

        return pluginInfo.getProfileSettings().getConfiguration(key).isSecure();
    }

    @Override
    protected boolean hasPluginInfo() {
        return this.metadataStore().getPluginInfo(getPluginId()) != null;
    }

    private ElasticAgentMetadataStore metadataStore() {
        return ElasticAgentMetadataStore.instance();
    }
}
