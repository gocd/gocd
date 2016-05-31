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

import java.util.Map;

public class PluginConfigurationViewModel {
    private final String key;
    private final Map<String, Object> metadata;
    private String type;

    public PluginConfigurationViewModel(String key, Map<String, Object> metadata) {
        this.key = key;
        this.metadata = metadata;
    }

    public PluginConfigurationViewModel(String key, Map<String, Object> metadata, String type) {
        this(key, metadata);
        this.type = type;
    }

    public String getKey() {
        return key;
    }

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

        PluginConfigurationViewModel that = (PluginConfigurationViewModel) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return metadata != null ? metadata.equals(that.metadata) : that.metadata == null;

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }
}
