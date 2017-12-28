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

package com.thoughtworks.go.config.representers;

import cd.go.jrepresenter.annotations.Property;
import cd.go.jrepresenter.annotations.Represents;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;

import java.util.function.Function;

@Represents(ConfigurationProperty.class)
public interface ConfigurationPropertyRepresenter {

    @Property(modelAttributeType = ConfigurationKey.class, serializer = ConfigurationKeySerializer.class, deserializer = ConfigurationKeyDeserializer.class)
    String key();

    @Property(modelAttributeType = ConfigurationValue.class, modelAttributeName = "configurationValue",
            serializer = ConfigurationValueSerializer.class, deserializer = ConfigurationValueDeserializer.class)
    String value();

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
            return configurationValue.getValue();
        }
    }

    class ConfigurationValueDeserializer implements Function<String, ConfigurationValue> {
        @Override
        public ConfigurationValue apply(String s) {
            return new ConfigurationValue(s);
        }
    }

}
