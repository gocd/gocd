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
package com.thoughtworks.go.plugin.domain.elastic;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.Objects;

public class ElasticAgentPluginInfo extends PluginInfo {
    private final PluggableInstanceSettings profileSettings;
    private final PluggableInstanceSettings clusterProfileSettings;
    private final Capabilities capabilities;

    public ElasticAgentPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings profileSettings, PluggableInstanceSettings clusterProfileSettings, Image image,
                                  PluggableInstanceSettings pluginSettings, Capabilities capabilities) {
        super(descriptor, PluginConstants.ELASTIC_AGENT_EXTENSION, pluginSettings, image);
        this.profileSettings = profileSettings;
        this.clusterProfileSettings = clusterProfileSettings;
        this.capabilities = capabilities;
    }

    public PluggableInstanceSettings getElasticAgentProfileSettings() {
        return profileSettings;
    }

    public PluggableInstanceSettings getProfileSettings() {
        return getElasticAgentProfileSettings();
    }

    public PluggableInstanceSettings getClusterProfileSettings() {
        return clusterProfileSettings;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public boolean supportsStatusReport() {
        return this.capabilities != null ? this.capabilities.supportsPluginStatusReport() : false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ElasticAgentPluginInfo that = (ElasticAgentPluginInfo) o;
        return Objects.equals(profileSettings, that.profileSettings) &&
                Objects.equals(clusterProfileSettings, that.clusterProfileSettings) &&
                Objects.equals(capabilities, that.capabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), profileSettings, clusterProfileSettings, capabilities);
    }
}
