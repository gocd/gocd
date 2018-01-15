/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.representers.config.rolev1;

import cd.go.jrepresenter.annotations.Property;
import cd.go.jrepresenter.annotations.Represents;
import com.google.gson.JsonIOException;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;

import java.util.Map;
import java.util.function.Function;

@Represents(
        value = ConfigurationProperty.class,
        deserializer = ConfigurationPropertyRepresenter.ConfigurationPropertyDeserializer.class
)
public interface ConfigurationPropertyRepresenter {

    @Property(
            modelAttributeType = ConfigurationKey.class,
            serializer = ConfigurationKeySerializer.class,
            deserializer = ConfigurationKeyDeserializer.class
    )
    String key();

    @Property(
            modelAttributeType = ConfigurationValue.class,
            modelAttributeName = "configurationValue",
            serializer = ConfigurationValueSerializer.class,
            deserializer = ConfigurationValueDeserializer.class,
            skipRender = IfSecureConfigurationValue.class
    )
    String value();


    @Property(
            modelAttributeType = EncryptedConfigurationValue.class,
            modelAttributeName = "encryptedValue",
            deserializer = EncryptedConfigurationValueDeserializer.class,
            skipRender = IfPlainTextConfigurationValue.class
    )
    String encryptedValue();

    class ConfigurationKeySerializer implements Function<ConfigurationKey, String> {
        @Override
        public String apply(ConfigurationKey configurationKey) {
            return configurationKey.getName();
        }
    }

    class ConfigurationKeyDeserializer implements Function<String, ConfigurationKey> {
        @Override
        public ConfigurationKey apply(String s) {
            return new ConfigurationKey(s);
        }
    }

    class ConfigurationValueSerializer implements Function<ConfigurationValue, String> {
        @Override
        public String apply(ConfigurationValue configurationValue) {
            return configurationValue == null ? null : configurationValue.getValue();
        }
    }

    class EncryptedConfigurationValueSerializer implements Function<EncryptedConfigurationValue, String> {
        @Override
        public String apply(EncryptedConfigurationValue configurationValue) {
            return configurationValue == null ? null : configurationValue.getValue();
        }
    }

    class EncryptedConfigurationValueDeserializer implements Function<String, EncryptedConfigurationValue> {
        @Override
        public EncryptedConfigurationValue apply(String encryptedValue) {
            return new EncryptedConfigurationValue(encryptedValue);
        }
    }

    class ConfigurationValueDeserializer implements Function<String, ConfigurationValue> {
        @Override
        public ConfigurationValue apply(String s) {
            return new ConfigurationValue(s);
        }
    }

    class IfPlainTextConfigurationValue implements Function<ConfigurationProperty, Boolean> {
        @Override
        public Boolean apply(ConfigurationProperty configurationProperty) {
            return !configurationProperty.isSecure();
        }
    }

    class IfSecureConfigurationValue implements Function<ConfigurationProperty, Boolean> {
        @Override
        public Boolean apply(ConfigurationProperty configurationProperty) {
            return configurationProperty.isSecure();
        }
    }

    class ConfigurationPropertyDeserializer implements Function<Map, ConfigurationProperty> {
        @Override
        public ConfigurationProperty apply(Map jsonObject) {
            try {
                String key = (String) jsonObject.get("key");
                String value = (String) jsonObject.get("value");
                String encryptedValue = (String) jsonObject.get("encrypted_value");
                return ConfigurationProperty.deserialize(key, value, encryptedValue);
            } catch (Exception e) {
                throw new JsonIOException("Could not parse configuration property", e);
            }
        }
    }
}
