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

package com.thoughtworks.go.domain.config;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.StringUtil;

import java.util.*;

import static java.lang.String.format;

@ConfigTag("configuration")
@ConfigCollection(value = ConfigurationProperty.class)
public class Configuration extends BaseCollection<ConfigurationProperty> {

    public static final String CONFIGURATION = "configuration";
    public static final String METADATA = "metadata";

    public Configuration() {
    }

    public Configuration(ConfigurationProperty... configurationProperties) {
        Collections.addAll(this, configurationProperties);
    }

    public String forDisplay(List<ConfigurationProperty> propertiesToDisplay) {
        ArrayList<String> list = new ArrayList<>();
        for (ConfigurationProperty property : propertiesToDisplay) {
            if (!property.isSecure()) {
                list.add(format("%s=%s", property.getConfigurationKey().getName().toLowerCase(), property.getConfigurationValue().getValue()));
            }
        }
        return format("[%s]", ListUtil.join(list, ", "));
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
        return ListUtil.find(this, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return ((ConfigurationProperty) item).getConfigurationKey().getName().equals(key);
            }
        });
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

            if (StringUtil.isBlank(configurationProperty.getValue()) && (configurationValue == null || configurationValue.errors().isEmpty()) && (encryptedValue == null || encryptedValue.errors().isEmpty())) {
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

    public boolean hasErrors() {
        for (ConfigurationProperty property : this) {
            if (property.hasErrors()) {
                return true;
            }
        }

        return false;
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
}
