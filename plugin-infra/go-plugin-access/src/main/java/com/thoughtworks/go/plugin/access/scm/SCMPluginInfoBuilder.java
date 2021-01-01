/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@Component
public class SCMPluginInfoBuilder implements PluginInfoBuilder<SCMPluginInfo> {

    private SCMExtension extension;

    @Autowired
    public SCMPluginInfoBuilder(SCMExtension extension) {
        this.extension = extension;
    }

    @Override
    public SCMPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        SCMPropertyConfiguration scmConfiguration = extension.getSCMConfiguration(descriptor.id());
        SCMView scmView = extension.getSCMView(descriptor.id());
        PluggableInstanceSettings pluginSettingsAndView = getPluginSettingsAndView(descriptor, extension);
        if (scmConfiguration == null) {
            throw new RuntimeException(format("Plugin[%s] returned null scm configuration", descriptor.id()));
        }

        if (scmView == null) {
            throw new RuntimeException(format("Plugin[%s] returned null scm view", descriptor.id()));
        }

        PluggableInstanceSettings scmSettings = new PluggableInstanceSettings(scmPluginConfigurations(scmConfiguration), new PluginView(scmView.template()));
        return new SCMPluginInfo(descriptor, scmView.displayValue(), scmSettings, pluginSettingsAndView);
    }

    private List<PluginConfiguration> scmPluginConfigurations(Configuration config) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (Property property : config.list()) {
            Metadata metadata = new MetadataWithPartOfIdentity(property.getOption(Property.REQUIRED), property.getOption(Property.SECURE), property.getOption(Property.PART_OF_IDENTITY));
            pluginConfigurations.add(new PluginConfiguration(property.getKey(), metadata));
        }
        return pluginConfigurations;
    }

}

