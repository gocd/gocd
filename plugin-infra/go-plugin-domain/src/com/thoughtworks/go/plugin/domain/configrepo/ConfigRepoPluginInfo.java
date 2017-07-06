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

package com.thoughtworks.go.plugin.domain.configrepo;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class ConfigRepoPluginInfo extends PluginInfo {

    private final PluggableInstanceSettings pluginSettings;

    public ConfigRepoPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings pluginSettings) {
        super(descriptor, PluginConstants.CONFIG_REPO_EXTENSION);
        this.pluginSettings = pluginSettings;
    }

    public PluggableInstanceSettings getPluginSettings() {
        return pluginSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ConfigRepoPluginInfo that = (ConfigRepoPluginInfo) o;

        return pluginSettings != null ? pluginSettings.equals(that.pluginSettings) : that.pluginSettings == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pluginSettings != null ? pluginSettings.hashCode() : 0);
        return result;
    }
}
