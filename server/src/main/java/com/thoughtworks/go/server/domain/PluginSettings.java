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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.*;

public class PluginSettings implements Validatable {
    private String pluginId;
    private List<ConfigurationProperty> settingsMap = new ArrayList<>();
    private boolean hasErrors = false;
    private final ConfigErrors errors = new ConfigErrors();

    public PluginSettings() {
    }

    public PluginSettings(String pluginId) {
        this.pluginId = pluginId;
    }

    private PluginSettings(String pluginId, List<ConfigurationProperty> settingsMap) {
        this(pluginId);
        this.settingsMap = settingsMap;
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
        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
        for (ConfigurationProperty property : configurationProperties) {
            ConfigurationProperty configurationProperty = builder.create(property.getConfigKeyName(),
                    property.getConfigValue(),
                    property.getEncryptedValue(),
                    pluginInfo.isSecure(property.getConfigKeyName()));
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
            addError(settingsKey, errorMessage);
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

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, fieldName);
    }

    public static PluginSettings from(Plugin plugin, PluginInfo pluginInfo) {
        ArrayList<ConfigurationProperty> settingsMap = new ArrayList<>();
        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();

        for (String key : plugin.getAllConfigurationKeys()) {
            ConfigurationProperty configurationProperty = builder.create(key,
                    plugin.getConfigurationValue(key),
                    null,
                    pluginInfo.isSecure(key));

            settingsMap.add(configurationProperty);
        }

        return new PluginSettings(plugin.getPluginId(), settingsMap);
    }

    public void validateTree() {
        ConfigSaveValidationContext validationContext = new ConfigSaveValidationContext(this);
        for (ConfigurationProperty configurationProperty : settingsMap) {
            configurationProperty.validate(validationContext);
            if (configurationProperty.hasErrors()) {
                hasErrors = true;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginSettings that = (PluginSettings) o;
        return Objects.equals(pluginId, that.pluginId) &&
                Objects.equals(settingsMap, that.settingsMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, settingsMap);
    }
}
