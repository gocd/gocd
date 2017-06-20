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

import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.ArrayList;
import java.util.List;

public interface PluginInfoBuilder<T extends PluginInfo> {
    T pluginInfoFor(GoPluginDescriptor descriptor);

    default List<PluginConfiguration> configurations(Configuration config) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (Property property : config.list()) {
            Metadata metadata = new Metadata(property.getOption(Property.REQUIRED), property.getOption(Property.SECURE));
            pluginConfigurations.add(new PluginConfiguration(property.getKey(), metadata));
        }
        return pluginConfigurations;
    }

}
