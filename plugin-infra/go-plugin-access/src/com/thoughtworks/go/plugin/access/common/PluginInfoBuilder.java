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

package com.thoughtworks.go.plugin.access.common;

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public interface PluginInfoBuilder<T extends PluginInfo> {
    Logger LOGGER = LoggerFactory.getLogger(PluginInfoBuilder.class);

    T pluginInfoFor(GoPluginDescriptor descriptor);

    default List<PluginConfiguration> configurations(Configuration config) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (Property property : config.list()) {
            Metadata metadata = new Metadata(property.getOption(Property.REQUIRED), property.getOption(Property.SECURE));
            pluginConfigurations.add(new PluginConfiguration(property.getKey(), metadata));
        }
        return pluginConfigurations;
    }

    default PluggableInstanceSettings getPluginSettingsAndView(GoPluginDescriptor descriptor, AbstractExtension extension) {
        PluggableInstanceSettings pluggableInstanceSettings = null;
        try {
            String pluginSettingsView = extension.getPluginSettingsView(descriptor.id());
            PluginSettingsConfiguration pluginSettingsConfiguration = extension.getPluginSettingsConfiguration(descriptor.id());
            if (pluginSettingsConfiguration == null || pluginSettingsView == null) {
                throw new RuntimeException("The plugin does not provide plugin settings or view.");
            }
            pluggableInstanceSettings = new PluggableInstanceSettings(configurations(pluginSettingsConfiguration), new PluginView(pluginSettingsView));
        } catch (Exception e) {
            LOGGER.error("Failed to fetch Plugin Settings metadata for plugin {}. Maybe the plugin does not implement plugin settings and view?", descriptor.id());
            LOGGER.debug(null, e);
        }
        return pluggableInstanceSettings;
    }
}
