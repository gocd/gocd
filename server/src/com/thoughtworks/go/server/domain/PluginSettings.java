/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.api.config.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PluginSettings {
    public static final String VALUE_KEY = "value";
    public static final String ERRORS_KEY = "errors";

    private String pluginId;
    private Map<String, Map<String, String>> settingsMap = new HashMap<>();
    private boolean hasErrors = false;

    public PluginSettings(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public Map<String, Map<String, String>> getSettingsMap() {
        return settingsMap;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public Map<String, String> getSettingsAsKeyValuePair() {
        Map<String, String> settingsAsKeyValuePair = new HashMap<>();
        for (String settingsKey : settingsMap.keySet()) {
            Map<String, String> settingsValueMap = settingsMap.get(settingsKey);
            String settingsValue = settingsValueMap.get(VALUE_KEY);
            settingsAsKeyValuePair.put(settingsKey, settingsValue);
        }
        return settingsAsKeyValuePair;
    }

    public void populateSettingsMap(Plugin plugin) {
        for (String settingsKey : plugin.getAllConfigurationKeys()) {
            Map<String, String> map = getSettingsMapFor(settingsKey);
            map.put(VALUE_KEY, plugin.getConfigurationValue(settingsKey));
        }
    }

    public void populateSettingsMap(PluginSettingsConfiguration configuration) {
        for (Property property : configuration.list()) {
            String settingsKey = property.getKey();
            Map<String, String> map = getSettingsMapFor(settingsKey);
            map.put(VALUE_KEY, "");
        }
    }

    public void populateSettingsMap(Map<String, String> parameterMap) {
        for (String settingsKey : parameterMap.keySet()) {
            Map<String, String> map = getSettingsMapFor(settingsKey);
            map.put(VALUE_KEY, parameterMap.get(settingsKey));
        }
    }

    public void populateErrorMessageFor(String settingsKey, String errorMessage) {
        hasErrors = true;
        Map<String, String> map = getSettingsMapFor(settingsKey);
        map.put(ERRORS_KEY, errorMessage);
    }

    public PluginSettingsConfiguration toPluginSettingsConfiguration() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        for (String settingsKey : settingsMap.keySet()) {
            configuration.add(new PluginSettingsProperty(settingsKey, getValueFor(settingsKey)));
        }
        return configuration;
    }

    private Map<String, String> getSettingsMapFor(String settingsKey) {
        if (settingsMap.get(settingsKey) == null) {
            settingsMap.put(settingsKey, new HashMap<String, String>());
        }
        return settingsMap.get(settingsKey);
    }

    public Set<String> getPluginSettingsKeys() {
        return settingsMap.keySet();
    }

    public String getValueFor(String settingsKey) {
        Map<String, String> map = settingsMap.get(settingsKey);
        return map == null ? null : map.get(VALUE_KEY);
    }

    public String getErrorFor(String settingsKey) {
        Map<String, String> map = settingsMap.get(settingsKey);
        return map == null ? null : map.get(ERRORS_KEY);
    }
}
