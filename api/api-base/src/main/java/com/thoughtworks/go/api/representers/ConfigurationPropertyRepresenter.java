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
package com.thoughtworks.go.api.representers;

import com.google.gson.JsonParseException;
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ConfigurationPropertyRepresenter {
    public static void toJSON(OutputListWriter propertiesWriter, List<ConfigurationProperty> configurationProperties) {
        configurationProperties.forEach(configurationProperty -> {
            if (configurationProperty == null) {
                return;
            }
            propertiesWriter.addChild(propertyWriter -> toJSON(propertyWriter, configurationProperty));
        });
    }

    public static void toJSON(OutputWriter writer, ConfigurationProperty configurationProperty) {
        writer.add("key", configurationProperty.getKey().getName());
        if (!configurationProperty.isSecure() && !isBlank(configurationProperty.getConfigValue())) {
            writer.add("value", configurationProperty.getConfigurationValue().getValue());
        }
        if (configurationProperty.isSecure() && !isBlank(configurationProperty.getEncryptedValue())) {
            writer.add("encrypted_value", configurationProperty.getEncryptedValue());
        }
        if (configurationProperty.hasErrors()) {
            writer.addChild("errors", errorWriter -> new ErrorGetter(new LinkedHashMap<String, String>() {{
                put("encryptedValue", "encrypted_value");
                put("configurationValue", "configuration_value");
                put("configurationKey", "configuration_key");
            }}).toJSON(errorWriter, configurationProperty));
        }
    }

    public static List<ConfigurationProperty> fromJSONArray(JsonReader jsonReader, String arrayKey) {
        List<ConfigurationProperty> configurationProperties = new ArrayList<>();
        jsonReader.readArrayIfPresent(arrayKey, properties -> {
            properties.forEach(property -> {
                JsonReader configPropertyReader = new JsonReader(property.getAsJsonObject());
                ConfigurationProperty configurationProperty = fromJSON(configPropertyReader);
                configurationProperties.add(configurationProperty);
            });

        });
        return configurationProperties;
    }

    public static ConfigurationProperty fromJSON(JsonReader jsonReader) {
        try {
            String key = jsonReader.getString("key");
            String value = jsonReader.optString("value").orElse(null);
            String encryptedValue = jsonReader.optString("encrypted_value").orElse(null);
            return new ConfigurationProperty().deserialize(key, value, encryptedValue);
        } catch (Exception e) {
            throw new JsonParseException("Could not parse configuration property");
        }
    }
}
