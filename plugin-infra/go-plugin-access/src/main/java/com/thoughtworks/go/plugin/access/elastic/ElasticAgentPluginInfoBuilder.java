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
package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.PluginView;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ElasticAgentPluginInfoBuilder implements PluginInfoBuilder<ElasticAgentPluginInfo> {
    private ElasticAgentExtension extension;

    @Autowired
    public ElasticAgentPluginInfoBuilder(ElasticAgentExtension extension) {
        this.extension = extension;
    }

    @Override
    public ElasticAgentPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        String pluginId = descriptor.id();

        PluggableInstanceSettings pluggableInstanceSettings = null;
        if (!extension.supportsClusterProfiles(pluginId)) {
            pluggableInstanceSettings = getPluginSettingsAndView(descriptor, extension);
        }

        return new ElasticAgentPluginInfo(descriptor,
                elasticElasticAgentProfileSettings(pluginId),
                elasticClusterProfileSettings(pluginId),
                image(pluginId),
                pluggableInstanceSettings,
                capabilities(pluginId));
    }

    private com.thoughtworks.go.plugin.domain.common.Image image(String pluginId) {
        return extension.getIcon(pluginId);
    }

    private PluggableInstanceSettings elasticElasticAgentProfileSettings(String pluginId) {
        List<PluginConfiguration> profileMetadata = extension.getProfileMetadata(pluginId);
        String profileView = extension.getProfileView(pluginId);
        return new PluggableInstanceSettings(profileMetadata, new PluginView(profileView));
    }

    private PluggableInstanceSettings elasticClusterProfileSettings(String pluginId) {
        if (extension.supportsClusterProfiles(pluginId)) {
            List<PluginConfiguration> profileMetadata = extension.getClusterProfileMetadata(pluginId);
            String profileView = extension.getClusterProfileView(pluginId);
            return new PluggableInstanceSettings(profileMetadata, new PluginView(profileView));
        }

        return new PluggableInstanceSettings(null, null);
    }

    private Capabilities capabilities(String pluginId) {
        return extension.getCapabilities(pluginId);
    }
}
