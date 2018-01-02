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

import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.*;

public class PluginSettings {
    private static final String VALUE_KEY = "value";
    private static final String ERRORS_KEY = "errors";

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

    public Map<String, Map<String, String>> getSettingsMap() {
        HashMap<String, Map<String, String>> map = new HashMap<>();
        for (ConfigurationProperty configurationProperty : settingsMap) {
            HashMap<String, String> valuesAndErrors = new HashMap<>();
            String settingsKey = configurationProperty.getConfigKeyName();
            valuesAndErrors.put(VALUE_KEY, configurationProperty.getConfigValue());
            List<String> errorsFor = configurationProperty.errors().getAllOn(settingsKey);
            if (errorsFor != null) {
                valuesAndErrors.put(ERRORS_KEY, errorsFor.toString());
            }
            map.put(settingsKey, valuesAndErrors);
        }
        return map;
    }

    public List<ConfigurationProperty> getSecurePluginSettingsProperties(PluginInfo pluginInfo) {
        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        if (pluginInfo != null) {
            ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
            PluggableInstanceSettings pluginSettings = pluginInfo.getPluginSettings();
            for (ConfigurationProperty configurationProperty : settingsMap) {
                PluginConfiguration pluginConfiguration = configPropertyFor(configurationProperty.getConfigKeyName(), pluginSettings);
                if (pluginConfiguration != null && pluginConfiguration.isSecure()) {
                    configurationProperties.add(builder.create(configurationProperty.getConfigKeyName(), configurationProperty.getConfigValue(), configurationProperty.getEncryptedValue(), true));
                } else {
                    configurationProperties.add(configurationProperty);
                }
            }
        }
        return configurationProperties;
    }

    public List<ConfigurationProperty> getPluginSettingsProperties() {
        return settingsMap;
    }

    public void setPluginSettingsProperties(List<ConfigurationProperty> configurationProperties) {
        settingsMap.clear();
        settingsMap.addAll(configurationProperties);
    }

    public void addConfigurations(PluginInfo pluginInfo, List<ConfigurationProperty> configurationProperties) {
        settingsMap.clear();
        if (pluginInfo != null) {
            ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
            for (ConfigurationProperty property : configurationProperties) {
                settingsMap.add(builder.create(property.getConfigKeyName(), property.getValue(), null, false));
            }
        }
    }

    private PluginConfiguration configPropertyFor(String configKeyName, PluggableInstanceSettings pluginSettings) {
        return pluginSettings.getConfiguration(configKeyName);
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public Map<String, String> getSettingsAsKeyValuePair() {
        Map<String, String> settingsAsKeyValuePair = new HashMap<>();
        for (ConfigurationProperty configurationProperty : settingsMap) {
            settingsAsKeyValuePair.put(configurationProperty.getConfigKeyName(), configurationProperty.getConfigValue());
        }
        return settingsAsKeyValuePair;
    }

    public void populateSettingsMap(Plugin plugin) {
        for (String settingsKey : plugin.getAllConfigurationKeys()) {
            settingsMap.add(new ConfigurationProperty(new ConfigurationKey(settingsKey), new ConfigurationValue(plugin.getConfigurationValue(settingsKey))));
        }
    }

    public void populateSettingsMap(PluginSettingsConfiguration configuration) {
        for (Property property : configuration.list()) {
            String settingsKey = property.getKey();
            settingsMap.add(new ConfigurationProperty(new ConfigurationKey(settingsKey), new ConfigurationValue("")));
        }
    }

    public void populateSettingsMap(Map<String, String> parameterMap) {
        for (String settingsKey : parameterMap.keySet()) {
            settingsMap.add(new ConfigurationProperty(new ConfigurationKey(settingsKey), new ConfigurationValue(parameterMap.get(settingsKey))));
        }
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
            configuration.add(new PluginSettingsProperty(configurationProperty.getConfigKeyName(), configurationProperty.getConfigValue()));
        }
        return configuration;
    }

    public String getValueFor(String settingsKey) {
        for (ConfigurationProperty configurationProperty : settingsMap) {
            if (configurationProperty.getConfigKeyName().equals(settingsKey)) {
                return configurationProperty.getConfigValue();
            }
        }
        return null;
    }

    public List<String> getErrorFor(String settingsKey) {
        for (ConfigurationProperty configurationProperty : settingsMap) {
            if (configurationProperty.getConfigKeyName().equals(settingsKey)) {
                return configurationProperty.errors().getAllOn(settingsKey);
            }
        }
        return errors().getAllOn(settingsKey);
    }

    public List<String> getPluginSettingsKeys() {
        List<String> list = new ArrayList<>();
        for (ConfigurationProperty configurationProperty : settingsMap) {
            list.add(configurationProperty.getConfigKeyName());
        }
        return list;
    }

    public ConfigErrors errors() {
        return errors;
    }
}
