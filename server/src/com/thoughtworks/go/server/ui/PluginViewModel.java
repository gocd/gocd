/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.ui;

import java.util.ArrayList;
import java.util.List;

public class PluginViewModel {

    private final String id;
    private final String name;
    private final String version;
    private final String type;
    private List<PluginConfigurationViewModel> configurations;

    public PluginViewModel(String id, String name, String version, String type) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.type = type;
    }

    public PluginViewModel(String id, String name, String version, String type, List<PluginConfigurationViewModel> pluginConfigurationViewModels) {
        this(id, name, version, type);

        this.configurations = pluginConfigurationViewModels;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public List<PluginConfigurationViewModel> getConfigurations() {
        return configurations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluginViewModel pluginViewModel = (PluginViewModel) o;

        if (id != null ? !id.equals(pluginViewModel.id) : pluginViewModel.id != null) return false;
        if (version != null ? !version.equals(pluginViewModel.version) : pluginViewModel.version != null) return false;
        if (type != null ? !type.equals(pluginViewModel.type) : pluginViewModel.type != null) return false;
        return configurations != null ? configurations.equals(pluginViewModel.configurations) : pluginViewModel.configurations == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (configurations != null ? configurations.hashCode() : 0);
        return result;
    }
}