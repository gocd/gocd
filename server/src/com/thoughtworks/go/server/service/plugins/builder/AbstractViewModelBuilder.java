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

import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginView;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public abstract class AbstractViewModelBuilder implements ViewModelBuilder {
    protected Image image(com.thoughtworks.go.plugin.domain.common.Image image) {
        if(image == null) {
            return null;
        }

        return new Image(image.getContentType(), image.getData());
    }

    protected PluggableInstanceSettings settings(com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings settings) {
        if(settings == null) {
            return null;
        }

        List<PluginConfiguration> configurations = new ArrayList<>();
        for(com.thoughtworks.go.plugin.domain.common.PluginConfiguration configuration : settings.getConfigurations()) {
            configurations.add(new PluginConfiguration(configuration.getKey(), configuration.getMetadata().toMap()));
        }

        String template = settings.getView() != null ? settings.getView().getTemplate() : null;

        return new PluggableInstanceSettings(configurations, new PluginView(template));
    }
}
