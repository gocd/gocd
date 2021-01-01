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
package com.thoughtworks.go.plugin.access.analytics;

import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.analytics.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsPluginInfoBuilder implements PluginInfoBuilder<AnalyticsPluginInfo> {

    private AnalyticsExtension extension;

    @Autowired
    public AnalyticsPluginInfoBuilder(AnalyticsExtension extension) {
        this.extension = extension;
    }

    @Override
    public AnalyticsPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        Capabilities capabilities = capabilities(descriptor.id());

        PluggableInstanceSettings pluginSettingsAndView = getPluginSettingsAndView(descriptor, extension);
        Image image = image(descriptor.id());

        return new AnalyticsPluginInfo(descriptor, image, capabilities, pluginSettingsAndView);
    }

    private Capabilities capabilities(String pluginId) {
        return extension.getCapabilities(pluginId);
    }

    private Image image(String pluginId) {
        return extension.getIcon(pluginId);
    }
}

