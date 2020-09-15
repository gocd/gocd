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
package com.thoughtworks.go.domain.config;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@ConfigTag("configuration")
@ConfigCollection(value = ConfigurationProperty.class)
public class Configuration extends BaseCollection<ConfigurationProperty> implements Validatable, SecretParamAware {

    public static final String CONFIGURATION = "configuration";
    public static final String METADATA = "metadata";
    public static final String VALUE_KEY = "value";
    public static final String ERRORS_KEY = "errors";

    private ConfigErrors errors = new ConfigErrors();

    public Configuration() {
    }

    public Configuration(ConfigurationProperty... configurationProperties) {
        Collections.addAll(this, configurationProperties);
    }

    public Configuration(Collection<ConfigurationProperty> configurationProperties) {
        addAll(configurationProperties);
    }

    public String forDisplay(List<ConfigurationProperty> propertiesToDisplay) {
        ArrayList<String> list = new ArrayList<>();
        for (ConfigurationProperty property : propertiesToDisplay) {
            if (!property.isSecure()) {
                list.add(format("%s=%s", property.getConfigurationKey().getName().toLowerCase(), property.getConfigurationValue().getValue()));
            }
        }
        return format("[%s]", StringUtils.join(list, ", "));
    }

    public void setConfigAttributes(Object attributes, SecureKeyInfoProvider secureKeyInfoProvider) {
        this.clear();
        Map attributesMap = (Map) attributes;
        for (Object o : attributesMap.values()) {
            Map configurationAttributeMap = (Map) o;
            ConfigurationProperty configurationProperty = new ConfigurationProperty();
            configurationProperty.setConfigAttributes(configurationAttributeMap, secureKeyInfoProvider);
            this.add(configurationProperty);
        }
    }

    public List<String> listOfConfigKeys() {
        ArrayList<String> list = new ArrayList<>();
        for (ConfigurationProperty configurationProperty : this) {
            list.add(configurationProperty.getConfigurationKey().getName());
        }
        return list;
    }

    public void addNewConfiguration(String key, boolean isSecure) {
        if (isSecure) {
            add(new ConfigurationProperty(new ConfigurationKey(key), new EncryptedConfigurationValue()));
        } else {
            add(new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue()));
        }
    }

    public void addNewConfigurationWithValue(String key, String value, boolean isSecure) {
        if (isSecure) {
            add(new ConfigurationProperty(new ConfigurationKey(key), new EncryptedConfigurationValue(value)));
        } else {
            add(new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue(value)));
        }
    }

    public ConfigurationProperty getProperty(final String key) {
        return stream().filter(item -> item.getConfigurationKey().getName().equals(key)).findFirst().orElse(null);
    }

    public void addErrorFor(String key, String message) {
        for (ConfigurationProperty configurationProperty : this) {
            if (configurationProperty.getConfigurationKey().getName().equals(key)) {
                configurationProperty.addErrorAgainstConfigurationValue(message);
                return;
            }
        }
    }

    public void clearEmptyConfigurations() {
        List<ConfigurationProperty> propertiesToRemove = new ArrayList<>();
        for (ConfigurationProperty configurationProperty : this) {
            ConfigurationValue configurationValue = configurationProperty.getConfigurationValue();
            EncryptedConfigurationValue encryptedValue = configurationProperty.getEncryptedConfigurationValue();

            if (StringUtils.isBlank(configurationProperty.getValue()) && (configurationValue == null || configurationValue.errors().isEmpty()) && (encryptedValue == null || encryptedValue.errors().isEmpty())) {
                propertiesToRemove.add(configurationProperty);
            }
        }
        this.removeAll(propertiesToRemove);
    }

    public void validateUniqueness(String entity) {
        HashMap<String, ConfigurationProperty> map = new HashMap<>();
        for (ConfigurationProperty property : this) {
            property.validateKeyUniqueness(map, entity);
        }
    }

    public void validateTree() {
        for (ConfigurationProperty property : this) {
            property.validate(null);
        }
    }

    //TODO: Move the validateUniquenessCheck from the parents to this method. Parents include SCM, PluginProfile, PluggableArtifactConfig, PackageRepository, PackageDefinition, FetchPluggableTask
    @Override
    public void validate(ValidationContext validationContext) {
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    public boolean hasErrors() {
        for (ConfigurationProperty property : this) {
            if (property.hasErrors()) {
                return true;
            }
        }

        return false;
    }


    @Override
    public void addError(String fieldName, String message) {
        addErrorFor(fieldName, message);
    }

    public Map<String, String> getConfigurationAsMap(boolean addSecureFields) {
        Map<String, String> configurationMap = new LinkedHashMap<>();
        for (ConfigurationProperty currentConfiguration : this) {
            if (addSecureFields || !currentConfiguration.isSecure()) {
                configurationMap.put(currentConfiguration.getConfigKeyName(), currentConfiguration.getValue());
            }
        }
        return configurationMap;
    }

    //Used in erb
    public Map<String, Map<String, Object>> getPropertyMetadataAndValuesAsMap() {
        Map<String, Map<String, Object>> configMap = new HashMap<>();
        for (ConfigurationProperty property : this) {
            Map<String, Object> mapValue = new HashMap<>();
            mapValue.put("isSecure", property.isSecure());
            if (property.isSecure()) {
                mapValue.put(VALUE_KEY, property.getEncryptedValue());
            } else {
                final String value = property.getConfigurationValue() == null ? null : property.getConfigurationValue().getValue();
                mapValue.put(VALUE_KEY, value);
            }
            mapValue.put("displayValue", property.getDisplayValue());
            configMap.put(property.getConfigKeyName(), mapValue);
        }
        return configMap;
    }

    public Map<String, Map<String, String>> getConfigWithErrorsAsMap() {
        Map<String, Map<String, String>> configMap = new HashMap<>();
        for (ConfigurationProperty property : this) {
            Map<String, String> mapValue = new HashMap<>();
            if (property.isSecure()) {
                mapValue.put(VALUE_KEY, property.getEncryptedValue());
            } else {
                final String value = property.getConfigurationValue() == null ? null : property.getConfigurationValue().getValue();
                mapValue.put(VALUE_KEY, value);
            }
            if (!property.getAllErrors().isEmpty()) {
                mapValue.put(ERRORS_KEY, StringUtils.join(property.getAllErrors().stream().map(ConfigErrors::getAll).collect(toList()), ", "));
            }
            configMap.put(property.getConfigKeyName(), mapValue);
        }
        return configMap;
    }

    @Override
    public boolean hasSecretParams() {
        return this.stream()
                .anyMatch(ConfigurationProperty::hasSecretParams);
    }

    @Override
    public SecretParams getSecretParams() {
        return this.stream()
                .map(ConfigurationProperty::getSecretParams)
                .filter((params) -> !params.isEmpty())
                .collect(SecretParams.toFlatSecretParams());
    }
}
