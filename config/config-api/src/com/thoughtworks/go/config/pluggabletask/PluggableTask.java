/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config.pluggabletask;

import com.thoughtworks.go.config.AbstractTask;
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.util.ListUtil;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @understands configuration of pluggable task
 */
@ConfigTag("task")
public class PluggableTask extends AbstractTask {
    public static final String TYPE = "pluggable_task";
    public static final String VALUE_KEY = "value";
    public static final String ERRORS_KEY = "errors";

    @ConfigSubtag
    private PluginConfiguration pluginConfiguration = new PluginConfiguration();

    @ConfigSubtag
    private Configuration configuration = new Configuration();

    public PluggableTask() {
    }

    public PluggableTask(PluginConfiguration pluginConfiguration, Configuration configuration) {
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = configuration;
    }

    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(PluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean hasSameTypeAs(Task task) {
        if (!getClass().equals(task.getClass())) {
            return false;
        }
        return this.pluginConfiguration.equals(((PluggableTask) task).pluginConfiguration);
    }

    @Override
    protected void setTaskConfigAttributes(Map attributes) {
        TaskConfig taskConfig = PluggableTaskConfigStore.store().preferenceFor(pluginConfiguration.getId()).getConfig();
        for (Property property : taskConfig.list()) {
            String key = property.getKey();
            if (attributes.containsKey(key)) {
                Boolean isSecure = property.getOption(Property.SECURE);
                if (configuration.getProperty(key) == null) {
                    configuration.addNewConfiguration(property.getKey(), isSecure);
                }
                configuration.getProperty(key).setConfigurationValue(new ConfigurationValue((String) attributes.get(key)));
                configuration.getProperty(key).handleSecureValueConfiguration(isSecure);
            }
        }
    }

    public TaskConfig toTaskConfig() {
        TaskConfig taskConfig = new TaskConfig();

        for (ConfigurationProperty configurationProperty : configuration) {
            taskConfig.add(new TaskConfigProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
        }

        return taskConfig;
    }

    public void addConfigurations(List<ConfigurationProperty> configurations) {
        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
        for (ConfigurationProperty property : configurations) {

            if(isValidPluginConfiguration(property.getConfigKeyName())) {
                configuration.add(builder.create(property.getConfigKeyName(), property.getConfigValue(), property.getEncryptedValue(),
                                                 pluginConfigurationFor(property.getConfigKeyName()).getOption(Property.SECURE)));
            } else {
                configuration.add(property);
            }
        }
    }

    private boolean isValidPluginConfiguration(String configKey) {
        return taskPreference() != null && pluginConfigurationFor(configKey) != null;
    }

    private Property pluginConfigurationFor(String configKey) {
        TaskPreference taskPreference = taskPreference();
        return taskPreference != null ? taskPreference.getConfig().get(configKey) : null;
    }

    private TaskPreference taskPreference() {
        return PluggableTaskConfigStore.store().preferenceFor(pluginConfiguration.getId());
    }

    @PostConstruct
    public void applyPluginMetadata() {
        if (taskPreference() != null) {
            for (ConfigurationProperty configurationProperty : configuration) {
                if (isValidPluginConfiguration(configurationProperty.getConfigKeyName())) {
                    Boolean isSecure = pluginConfigurationFor(configurationProperty.getConfigKeyName()).getOption(Property.SECURE);
                    configurationProperty.handleSecureValueConfiguration(isSecure);
                }
            }
        }
    }

    @Override
    protected void validateTask(ValidationContext validationContext) {
    }

    @Override
    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return (onCancelConfig.validateTree(validationContext) && errors.isEmpty() && !configuration.hasErrors());
    }

//  This method is called from PluggableTaskService to validate Tasks.
    public boolean isValid() {
        if (PluggableTaskConfigStore.store().preferenceFor(pluginConfiguration.getId()) == null) {
            addError(TYPE, String.format("Could not find plugin for given pluggable id:[%s].", pluginConfiguration.getId()));
        }

        configuration.validateTree();

        return (errors.isEmpty() && !configuration.hasErrors());
    }

    @Override
    public String getTaskType() {
        return "pluggable_task_" + getPluginConfiguration().getId().replaceAll("[^a-zA-Z0-9_]", "_");
    }

    @Override
    public String getTypeForDisplay() {
        return "Pluggable Task";
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        ArrayList<TaskProperty> taskProperties = new ArrayList<>();
        if (PluggableTaskConfigStore.store().hasPreferenceFor(pluginConfiguration.getId())) {
            TaskPreference preference = taskPreference();
            List<? extends Property> propertyDefinitions = preference.getConfig().list();
            for (Property propertyDefinition : propertyDefinitions) {
                ConfigurationProperty configuredProperty = configuration.getProperty(propertyDefinition.getKey());
                if (configuredProperty == null) continue;
                taskProperties.add(new TaskProperty(propertyDefinition.getOption(Property.DISPLAY_NAME), configuredProperty.getDisplayValue(), configuredProperty.getConfigKeyName()));
            }
            return taskProperties;
        }

        for (ConfigurationProperty property : configuration) {
            taskProperties.add(new TaskProperty(property.getConfigKeyName(), property.getDisplayValue()));
        }
        return taskProperties;
    }

    public Map<String, Map<String, String>> configAsMap() {
        Map<String, Map<String, String>> configMap = new HashMap<>();
        for (ConfigurationProperty property : configuration) {
            Map<String, String> mapValue = new HashMap<>();
            mapValue.put(VALUE_KEY, property.getValue());
            if (!property.errors().isEmpty()) {
                mapValue.put(ERRORS_KEY, ListUtil.join(property.errors().getAll()));
            }
            configMap.put(property.getConfigKeyName(), mapValue);
        }
        return configMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PluggableTask that = (PluggableTask) o;

        if (configuration != null ? !configuration.equals(that.configuration) : that.configuration != null) {
            return false;
        }
        if (!pluginConfiguration.equals(that.pluginConfiguration)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + pluginConfiguration.hashCode();
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }
}
