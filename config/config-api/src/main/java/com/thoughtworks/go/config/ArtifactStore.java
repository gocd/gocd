/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;

@ConfigTag("artifactStore")
@ConfigReferenceCollection(collectionName = "artifactStores", idFieldName = "id")
@ConfigCollection(value = ConfigurationProperty.class)
public class ArtifactStore extends PluginProfile {
    public ArtifactStore() {
        super();
    }

    public ArtifactStore(String id, String pluginId, ConfigurationProperty... configurationProperties) {
        super(id, pluginId, configurationProperties);
    }

    @Override
    protected String getObjectDescription() {
        return "Artifact store";
    }

    @Override
    protected boolean isSecure(String key) {
        ArtifactPluginInfo pluginInfo = metadataStore().getPluginInfo(getPluginId());

        if (pluginInfo == null
                || pluginInfo.getStoreConfigSettings() == null
                || pluginInfo.getStoreConfigSettings().getConfiguration(key) == null) {
            return false;
        }

        return pluginInfo.getStoreConfigSettings().getConfiguration(key).isSecure();
    }

    private ArtifactMetadataStore metadataStore() {
        return ArtifactMetadataStore.instance();
    }

    @Override
    protected boolean hasPluginInfo() {
        return metadataStore().getPluginInfo(getPluginId()) != null;
    }
}
