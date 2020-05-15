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
package com.thoughtworks.go.apiv7.plugininfos.representers.extensions;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv7.plugininfos.representers.PluggableInstanceSettingsRepresenter;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;

public class ElasticAgentExtensionRepresenter extends ExtensionRepresenter {
    @Override
    public void toJSON(OutputWriter extensionWriter, PluginInfo extension) {
        super.toJSON(extensionWriter, extension);

        ElasticAgentPluginInfo elasticAgentExtension = (ElasticAgentPluginInfo) extension;
        PluggableInstanceSettings elasticAgentProfileSettings = elasticAgentExtension.getProfileSettings();
        PluggableInstanceSettings clusterProfileSettings = elasticAgentExtension.getClusterProfileSettings();

        extensionWriter.addChild("elastic_agent_profile_settings", authConfigWriter -> PluggableInstanceSettingsRepresenter.toJSON(authConfigWriter, elasticAgentProfileSettings));

        if (clusterProfileSettings != null && clusterProfileSettings.getConfigurations() != null && clusterProfileSettings.getView() != null) {
            extensionWriter.add("supports_cluster_profiles", true);
            extensionWriter.addChild("cluster_profile_settings", authConfigWriter -> PluggableInstanceSettingsRepresenter.toJSON(authConfigWriter, clusterProfileSettings));
        } else {
            extensionWriter.add("supports_cluster_profiles", false);
        }

        extensionWriter.addChild("capabilities", capabilitiesWriter ->
                capabilitiesWriter.add("supports_plugin_status_report", elasticAgentExtension.getCapabilities().supportsPluginStatusReport())
                        .add("supports_agent_status_report", elasticAgentExtension.getCapabilities().supportsAgentStatusReport())
                        .add("supports_cluster_status_report", elasticAgentExtension.getCapabilities().supportsClusterStatusReport()));
    }
}
