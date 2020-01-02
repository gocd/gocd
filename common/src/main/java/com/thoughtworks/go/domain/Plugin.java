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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.json.JsonHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

public class Plugin extends PersistentObject {
    protected String pluginId;
    private String configuration;
    private Map<String, String> configurationDataMap;

    protected Plugin() {
    }

    public Plugin(String pluginId, String configuration) {
        bombIfNull(pluginId, "pluginId name cannot be null.");

        this.pluginId = pluginId;
        setConfiguration(configuration);
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
        this.configurationDataMap = JsonHelper.safeFromJson(this.configuration, HashMap.class);
    }

    public Set<String> getAllConfigurationKeys() {
        return Collections.unmodifiableSet(getConfigurationDataMap().keySet());
    }

    public String getConfigurationValue(String key) {
        return getConfigurationDataMap().get(key);
    }

    private Map<String, String> getConfigurationDataMap() {
        return configurationDataMap == null ? new HashMap<>() : configurationDataMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Plugin plugin = (Plugin) o;

        if (configuration != null ? !configuration.equals(plugin.configuration) : plugin.configuration != null)
            return false;
        if (pluginId != null ? !pluginId.equals(plugin.pluginId) : plugin.pluginId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }
}
