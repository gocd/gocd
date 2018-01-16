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

package com.thoughtworks.go.apiv1.admin.representers;

import cd.go.jrepresenter.RequestContext;
import cd.go.jrepresenter.annotations.Errors;
import cd.go.jrepresenter.annotations.Property;
import cd.go.jrepresenter.annotations.Represents;
import com.google.gson.JsonIOException;
import com.thoughtworks.go.api.ErrorGetter;
import com.thoughtworks.go.api.IfNoErrors;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.apache.commons.lang.StringUtils.isBlank;

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
            skipRender = IfSecureConfigurationValueOrNull.class
    )
    String value();


    @Property(
            modelAttributeType = EncryptedConfigurationValue.class,
            modelAttributeName = "encryptedValue",
            deserializer = EncryptedConfigurationValueDeserializer.class,
            skipRender = IfPlainTextConfigurationValueOrNull.class
    )
    String encryptedValue();

    @Errors(getter = ConfigurationPropertyRoleGetter.class, skipRender = IfNoErrors.class)
    Map errors();

    class ConfigurationKeySerializer implements BiFunction<ConfigurationKey, RequestContext, String> {
        @Override
        public String apply(ConfigurationKey configurationKey, RequestContext requestContext) {
            return configurationKey.getName();
        }
    }

    class ConfigurationKeyDeserializer implements BiFunction<String, RequestContext, ConfigurationKey> {
        @Override
        public ConfigurationKey apply(String s, RequestContext requestContext) {
            return new ConfigurationKey(s);
        }
    }

    class ConfigurationValueSerializer implements BiFunction<ConfigurationValue, RequestContext, String> {
        @Override
        public String apply(ConfigurationValue configurationValue, RequestContext requestContext) {
            return configurationValue == null ? null : configurationValue.getValue();
        }
    }

    class EncryptedConfigurationValueSerializer implements BiFunction<EncryptedConfigurationValue, RequestContext, String> {
        @Override
        public String apply(EncryptedConfigurationValue configurationValue, RequestContext requestContext) {
            return configurationValue == null ? null : configurationValue.getValue();
        }
    }

    class EncryptedConfigurationValueDeserializer implements BiFunction<String, RequestContext, EncryptedConfigurationValue> {
        @Override
        public EncryptedConfigurationValue apply(String encryptedValue, RequestContext requestContext) {
            return new EncryptedConfigurationValue(encryptedValue);
        }
    }

    class ConfigurationValueDeserializer implements BiFunction<String, RequestContext, ConfigurationValue> {
        @Override
        public ConfigurationValue apply(String s, RequestContext requestContext) {
            return new ConfigurationValue(s);
        }
    }

    class IfPlainTextConfigurationValueOrNull implements BiFunction<ConfigurationProperty, RequestContext, Boolean> {
        @Override
        public Boolean apply(ConfigurationProperty configurationProperty, RequestContext requestContext) {
            return !configurationProperty.isSecure() || isBlank(configurationProperty.getEncryptedValue());
        }
    }

    class IfSecureConfigurationValueOrNull implements BiFunction<ConfigurationProperty, RequestContext, Boolean> {
        @Override
        public Boolean apply(ConfigurationProperty configurationProperty, RequestContext requestContext) {
            return configurationProperty.isSecure() || isBlank(configurationProperty.getConfigValue());
        }
    }

    class ConfigurationPropertyDeserializer implements BiFunction<Map, RequestContext, ConfigurationProperty> {
        @Override
        public ConfigurationProperty apply(Map jsonObject, RequestContext requestContext) {
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

    class ConfigurationPropertyRoleGetter extends ErrorGetter {
        public ConfigurationPropertyRoleGetter() {
            super(new LinkedHashMap<String, String>() {{
                put("encryptedValue", "encrypted_value");
                put("configurationValue", "configuration_value");
                put("configurationKey", "configuration_key");
            }});
        }
    }
}
