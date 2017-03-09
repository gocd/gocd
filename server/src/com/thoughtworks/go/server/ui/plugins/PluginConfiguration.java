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

import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKey;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.server.service.plugins.builder.ViewModelBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PluginConfiguration {
    private final String key;
    private final Map<String, Object> metadata;
    @Deprecated // used only by v1 and v2
    private final String type;

    public PluginConfiguration(String key, Map<String, Object> metadata, String type) {
        this.key = key;
        this.metadata = metadata;
        this.type = type;
    }

    public PluginConfiguration(String key, Map<String, Object> metadata) {
        this(key, metadata, null);
    }

    public static ArrayList<PluginConfiguration> getPluginConfigurations(PluginProfileMetadataKeys config) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        for (PluginProfileMetadataKey property : config) {
            Map<String, Object> metaData = new HashMap<>();
            metaData.put(ViewModelBuilder.REQUIRED_OPTION, property.getMetadata().isRequired());
            metaData.put(ViewModelBuilder.SECURE_OPTION, property.getMetadata().isSecure());

            pluginConfigurations.add(new PluginConfiguration(property.getKey(), metaData));
        }
        return pluginConfigurations;
    }

    public String getKey() {
        return key;
    }

    @Deprecated // used only by v1 and v2
    public String getType() {
        return type;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluginConfiguration that = (PluginConfiguration) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
