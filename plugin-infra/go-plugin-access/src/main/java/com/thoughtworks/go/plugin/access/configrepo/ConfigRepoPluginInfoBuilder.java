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

package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;
import com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigRepoPluginInfoBuilder implements PluginInfoBuilder<ConfigRepoPluginInfo> {
    private ConfigRepoExtension extension;

    @Autowired
    public ConfigRepoPluginInfoBuilder(ConfigRepoExtension extension) {
        this.extension = extension;
    }

    public ConfigRepoPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        PluggableInstanceSettings pluggableInstanceSettings = getPluginSettingsAndView(descriptor, extension);

        return new ConfigRepoPluginInfo(descriptor, image(descriptor.id()), pluggableInstanceSettings, capabilities(descriptor.id()));
    }

    private Capabilities capabilities(String pluginId) {
        return extension.getCapabilities(pluginId);
    }

    private com.thoughtworks.go.plugin.domain.common.Image image(String pluginId) {
        return extension.getIcon(pluginId);
    }

}
