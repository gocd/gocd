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
package com.thoughtworks.go.plugin.domain.common;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;

public class PluginInfo {
    protected final PluginDescriptor descriptor;
    protected final String extensionName;
    public PluggableInstanceSettings pluginSettings;
    protected Image image;

    public PluginInfo(PluginDescriptor descriptor, String extensionName, PluggableInstanceSettings pluginSettings, Image image) {
        this.descriptor = descriptor;
        this.extensionName = extensionName;
        this.pluginSettings = pluginSettings;
        this.image = image;
    }

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public PluggableInstanceSettings getPluginSettings() {
        return pluginSettings;
    }

    public boolean isSecure(String key) {
        return getPluginSettings().getConfiguration(key) != null && getPluginSettings().getConfiguration(key).isSecure();
    }

    public Image getImage() {
        return image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluginInfo that = (PluginInfo) o;

        if (descriptor != null ? !descriptor.equals(that.descriptor) : that.descriptor != null) return false;
        if (extensionName != null ? !extensionName.equals(that.extensionName) : that.extensionName != null)
            return false;
        return pluginSettings != null ? pluginSettings.equals(that.pluginSettings) : that.pluginSettings == null;
    }

    @Override
    public int hashCode() {
        int result = descriptor != null ? descriptor.hashCode() : 0;
        result = 31 * result + (extensionName != null ? extensionName.hashCode() : 0);
        result = 31 * result + (pluginSettings != null ? pluginSettings.hashCode() : 0);
        return result;
    }
}
