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

    /**
     * Like {@link #fromJSONArray(JsonReader, String)}, but honors the `is_secure` flag
     *
     * @param jsonReader the reader for JSON input
     * @param arrayKey   the JSON key holding the configuration properties block
     * @return a {@link List<ConfigurationProperty>}
     */
    public static List<ConfigurationProperty> fromJSONArrayHandlingEncryption(JsonReader jsonReader, String arrayKey) {
        List<ConfigurationProperty> configurationProperties = new ArrayList<>();
        jsonReader.readArrayIfPresent(arrayKey, properties -> {
            properties.forEach(property -> {
                JsonReader configPropertyReader = new JsonReader(property.getAsJsonObject());
                ConfigurationProperty configurationProperty = fromJSONHandlingEncryption(configPropertyReader);
                configurationProperties.add(configurationProperty);
            });

        });
        return configurationProperties;
    }

    /**
     * Like {@link #fromJSON(JsonReader)}, but handles an additional `is_secure` flag.
     * <p>
     * Behavior:
     * <p>
     * 1. if `encrypted_value` is provided, it behaves like {@link #fromJSON(JsonReader)} and ignores `is_secure`
     * 2. only if `value` and `is_secure` are present, conditionally encrypts `value` => `encrypted_value` depending
     * on the `is_secure` flag: `true` causes encryption, `false` leaves as plaintext.
     *
     * @param jsonReader a reader for the serialized JSON input
     * @return a {@link ConfigurationProperty}
     */
    public static ConfigurationProperty fromJSONHandlingEncryption(JsonReader jsonReader) {
        try {
            final String key = jsonReader.getString("key");
            final String value = jsonReader.optString("value").orElse(null);
            final String encryptedValue = jsonReader.optString("encrypted_value").orElse(null);
            final Boolean isSecure = jsonReader.optBoolean("is_secure").orElse(null);

            final ConfigurationProperty property = new ConfigurationProperty().deserialize(key, value, encryptedValue);

            if (isBlank(encryptedValue) && null != isSecure) {
                property.handleSecureValueConfiguration(isSecure); // handle encryptions
            }

            return property;
        } catch (Exception e) {
            throw new JsonParseException("Could not parse configuration property");
        }
    }
}
