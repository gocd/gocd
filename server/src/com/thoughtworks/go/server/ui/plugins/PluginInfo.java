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

package com.thoughtworks.go.server.ui.plugins;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;

public class PluginInfo {
    private final String id;
    private final String name;
    private final String version;
    private final String type;
    private final String displayName;
    private final PluggableInstanceSettings pluggableInstanceSettings;

    public PluginInfo(String id, String name, String version, String type, String displayName, PluggableInstanceSettings settings) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.type = type;
        this.displayName = displayName;
        this.pluggableInstanceSettings = settings;
    }

    public PluginInfo(PluginDescriptor descriptor, String type, String displayName, PluggableInstanceSettings settings) {
        this(descriptor.id(), descriptor.about().name(), descriptor.about().version(), type, displayName, settings);
    }

    public PluginInfo(String id, String name, String version, String type, String displayName) {
        this(id, name, version, type, displayName, null);
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

    public PluggableInstanceSettings getPluggableInstanceSettings() {
        return pluggableInstanceSettings;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluginInfo that = (PluginInfo) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        return pluggableInstanceSettings != null ? pluggableInstanceSettings.equals(that.pluggableInstanceSettings) : that.pluggableInstanceSettings == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (pluggableInstanceSettings != null ? pluggableInstanceSettings.hashCode() : 0);
        return result;
    }
}
