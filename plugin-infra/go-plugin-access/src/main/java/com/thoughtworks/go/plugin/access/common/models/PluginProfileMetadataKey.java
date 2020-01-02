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
package com.thoughtworks.go.plugin.access.common.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;


public class PluginProfileMetadataKey {
    @Expose
    @SerializedName("key")
    private final String key;
    @Expose
    @SerializedName("metadata")
    private final PluginProfileMetadata metadata;

    public PluginProfileMetadataKey(String key, PluginProfileMetadata metadata) {
        this.key = key;
        this.metadata = metadata;
    }

    public String getKey() {
        return key;
    }

    public PluginProfileMetadata getMetadata() {
        if (metadata == null) {
            return new PluginProfileMetadata(false, false);
        }
        return metadata;
    }

    public PluginConfiguration toPluginConfiguration() {
        return new PluginConfiguration(key, getMetadata().toMetadata());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginProfileMetadataKey)) return false;

        PluginProfileMetadataKey that = (PluginProfileMetadataKey) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        return metadata != null ? metadata.equals(that.metadata) : that.metadata == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }
}
