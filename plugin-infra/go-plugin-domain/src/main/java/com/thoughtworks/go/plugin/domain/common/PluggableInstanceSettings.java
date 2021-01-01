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
package com.thoughtworks.go.plugin.domain.common;

import java.util.List;

public class PluggableInstanceSettings {
    private final List<PluginConfiguration> configurations;
    private final PluginView view;

    public PluggableInstanceSettings(List<PluginConfiguration> pluginConfigurations, PluginView pluginView) {
        this.configurations = pluginConfigurations;
        this.view = pluginView;
    }

    public PluggableInstanceSettings(List<PluginConfiguration> pluginConfigurations) {
        this(pluginConfigurations, null);
    }

    public List<PluginConfiguration> getConfigurations() {
        return configurations;
    }

    public PluginConfiguration getConfiguration(String key) {
        return this.configurations.stream().filter(c -> c.getKey().equals(key)).findFirst().orElse(null);
    }

    public PluginView getView() {
        return view;
    }

    public boolean hasView() {
        return view != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluggableInstanceSettings that = (PluggableInstanceSettings) o;

        if (configurations != null ? !configurations.equals(that.configurations) : that.configurations != null)
            return false;
        return view != null ? view.equals(that.view) : that.view == null;

    }

    @Override
    public int hashCode() {
        int result = configurations != null ? configurations.hashCode() : 0;
        result = 31 * result + (view != null ? view.hashCode() : 0);
        return result;
    }
}
