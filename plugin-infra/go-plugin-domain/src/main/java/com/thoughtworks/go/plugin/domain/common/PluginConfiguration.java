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

public class PluginConfiguration {
    private final String key;
    private final Metadata metadata;

    public PluginConfiguration(String key, Metadata metadata) {
        this.key = key;
        this.metadata = metadata != null ? metadata : new Metadata(false, false);
    }

    public String getKey() {
        return key;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public boolean isSecure() {
        return metadata.isSecure();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluginConfiguration that = (PluginConfiguration) o;

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
