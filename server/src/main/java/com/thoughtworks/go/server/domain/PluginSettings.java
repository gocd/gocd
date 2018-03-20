/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginSettings {
    private String pluginId;
    private List<ConfigurationProperty> settingsMap = new ArrayList<>();
    private boolean hasErrors = false;
    private ConfigErrors errors = new ConfigErrors();

    public PluginSettings() {
    }

    public PluginSettings(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public List<ConfigurationProperty> getPluginSettingsProperties() {
        return settingsMap;
    }

    public void addConfigurations(PluginInfo pluginInfo, List<ConfigurationProperty> configurationProperties) {
        settingsMap.clear();
        for (ConfigurationProperty property : configurationProperties) {
            final ConfigurationProperty configurationProperty = ConfigurationProperty.builder(property.getConfigKeyName())
                    .value(property.getValue())
                    .secure(isSecure(pluginInfo, property.getConfigKeyName()))
                    .build();
            settingsMap.add(configurationProperty);
        }
    }

    public boolean hasErrors() {
        return hasErrors || !errors().isEmpty();
    }

    public Map<String, String> getSettingsAsKeyValuePair() {
        Map<String, String> settingsAsKeyValuePair = new HashMap<>();
        for (ConfigurationProperty configurationProperty : settingsMap) {
            settingsAsKeyValuePair.put(configurationProperty.getConfigKeyName(), configurationProperty.getValue());
        }
        return settingsAsKeyValuePair;
    }

    public void populateErrorMessageFor(String settingsKey, String errorMessage) {
        hasErrors = true;
        for (ConfigurationProperty configurationProperty : settingsMap) {
            if (configurationProperty.getConfigKeyName().equals(settingsKey)) {
                configurationProperty.addError(settingsKey, errorMessage);
                break;
            }
        }

        if (getErrorFor(settingsKey) == null) {
            errors().add(settingsKey, errorMessage);
        }
    }

    public PluginSettingsConfiguration toPluginSettingsConfiguration() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        for (ConfigurationProperty configurationProperty : settingsMap) {
            configuration.add(new PluginSettingsProperty(configurationProperty.getConfigKeyName(), configurationProperty.getValue()));
        }
        return configuration;
    }

    public List<String> getErrorFor(String settingsKey) {
        for (ConfigurationProperty configurationProperty : settingsMap) {
            if (configurationProperty.getConfigKeyName().equals(settingsKey)) {
                return configurationProperty.errors().getAllOn(settingsKey);
            }
        }
        return errors().getAllOn(settingsKey);
    }

    public ConfigErrors errors() {
        return errors;
    }

    public static PluginSettings from(Plugin plugin, PluginInfo pluginInfo) {
        final PluginSettings pluginSettings = new PluginSettings(plugin.getPluginId());
        for (String key : plugin.getAllConfigurationKeys()) {
            final ConfigurationProperty configurationProperty = ConfigurationProperty.builder(key)
                    .value(plugin.getConfigurationValue(key))
                    .secure(isSecure(pluginInfo, key))
                    .build();

            pluginSettings.settingsMap.add(configurationProperty);
        }

        return pluginSettings;
    }

    private static boolean isSecure(PluginInfo pluginInfo, String key) {
        return pluginInfo.getPluginSettings().getConfiguration(key) != null &&
                pluginInfo.getPluginSettings().getConfiguration(key).isSecure();
    }
}
