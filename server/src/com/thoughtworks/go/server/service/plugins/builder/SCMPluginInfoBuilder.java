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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Deprecated
public class SCMPluginInfoBuilder implements NewPluginInfoBuilder<SCMPluginInfo> {
    private final PluginManager pluginManager;
    private final SCMMetadataStore store;

    public SCMPluginInfoBuilder(PluginManager pluginManager, SCMMetadataStore store) {
        this.pluginManager = pluginManager;
        this.store = store;
    }

    @Override
    public SCMPluginInfo pluginInfoFor(String pluginId) {
        if (!store.getPlugins().contains(pluginId)) {
            return null;
        }

        GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);
        SCMPreference scmPreference = store.preferenceFor(pluginId);

        List<PluginConfiguration> pluginConfigurations = configurations(scmPreference.getScmConfigurations());
        PluginView pluginView = new PluginView(scmPreference.getScmView().template());

        return new SCMPluginInfo(descriptor, scmPreference.getScmView().displayValue(), new PluggableInstanceSettings(pluginConfigurations, pluginView));
    }

    @Override
    public Collection<SCMPluginInfo> allPluginInfos() {
        return store.getPlugins().stream().map(new Function<String, SCMPluginInfo>() {
            @Override
            public SCMPluginInfo apply(String pluginId) {
                return SCMPluginInfoBuilder.this.pluginInfoFor(pluginId);
            }
        }).collect(Collectors.toList());
    }

    static List<PluginConfiguration> configurations(SCMConfigurations scmConfigurations) {
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for(SCMConfiguration configuration : scmConfigurations.list()) {
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("required", configuration.getOption(Property.REQUIRED));
            metaData.put("secure", configuration.getOption(Property.SECURE));
            metaData.put("part_of_identity", configuration.getOption(Property.PART_OF_IDENTITY));

            pluginConfigurations.add(new PluginConfiguration(configuration.getKey(), metaData));
        }
        return pluginConfigurations;
    }

}
