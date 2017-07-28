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

package com.thoughtworks.go.plugin.domain.elastic;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class ElasticAgentPluginInfo extends PluginInfo {
    private final PluggableInstanceSettings profileSettings;
    private final Image image;
    private final boolean supportsStatusReport;

    public ElasticAgentPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings profileSettings, Image image,
                                  PluggableInstanceSettings pluginSettings, boolean supportsStatusReport) {
            super(descriptor, PluginConstants.ELASTIC_AGENT_EXTENSION, pluginSettings);
        this.profileSettings = profileSettings;
        this.image = image;
        this.supportsStatusReport = supportsStatusReport;
    }

    public PluggableInstanceSettings getProfileSettings() {
        return profileSettings;
    }

    public Image getImage() {
        return image;
    }

    public boolean supportsStatusReport() {
        return this.supportsStatusReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ElasticAgentPluginInfo that = (ElasticAgentPluginInfo) o;

        if (supportsStatusReport != that.supportsStatusReport) return false;
        if (profileSettings != null ? !profileSettings.equals(that.profileSettings) : that.profileSettings != null)
            return false;
        return image != null ? image.equals(that.image) : that.image == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (profileSettings != null ? profileSettings.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (supportsStatusReport ? 1 : 0);
        return result;
    }
}
