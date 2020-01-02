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
package com.thoughtworks.go.plugin.access.secrets;

import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginView;
import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class SecretsPluginInfoBuilder implements PluginInfoBuilder<SecretsPluginInfo> {

    private SecretsExtension extension;

    @Autowired
    public SecretsPluginInfoBuilder(SecretsExtension extension) {
        this.extension = extension;
    }

    @Override
    public SecretsPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        String pluginId = descriptor.id();

        return new SecretsPluginInfo(descriptor, securityConfigSettings(pluginId), image(pluginId));
    }

    private Image image(String pluginId) {
        return extension.getIcon(pluginId);
    }

    private PluggableInstanceSettings securityConfigSettings(String pluginId) {
        return new PluggableInstanceSettings(extension.getSecretsConfigMetadata(pluginId),
                new PluginView(extension.getSecretsConfigView(pluginId)));
    }
}

