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

package com.thoughtworks.go.domain.config;

import com.thoughtworks.go.security.GoCipher;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

public class ConfigurationPropertyBuilder {
    private ConfigurationProperty configurationProperty;

    ConfigurationPropertyBuilder(String key) {
        configurationProperty = new ConfigurationProperty();
        configurationProperty.setConfigurationKey(new ConfigurationKey(key));
    }

    public ConfigurationValueBuilder value(String value) {
        return new ConfigurationValueBuilder(configurationProperty, value);
    }

    public EncryptedConfigurationValueBuilder encryptedValue(String encryptedValue) {
        return new EncryptedConfigurationValueBuilder(configurationProperty, encryptedValue);
    }

    public class ConfigurationValueBuilder {
        private final ConfigurationProperty configurationProperty;
        private final GoCipher cipher;
        private final String value;
        private boolean secure;

        ConfigurationValueBuilder(ConfigurationProperty configurationProperty, String value) {
            this.cipher = new GoCipher();
            this.configurationProperty = configurationProperty;
            this.value = value;
        }

        public ConfigurationValueBuilder secure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public ConfigurationProperty build() {
            if (secure) {
                configurationProperty.setEncryptedValue(getEncryptedValue());
            } else {
                ConfigurationValue configurationValue = (value == null) ? new ConfigurationValue() : new ConfigurationValue(value);
                configurationProperty.setConfigurationValue(configurationValue);
            }
            return configurationProperty;
        }

        private EncryptedConfigurationValue getEncryptedValue() {
            return StringUtils.isBlank(value) ? new EncryptedConfigurationValue() : new EncryptedConfigurationValue(encryptValue(value));
        }

        private String encryptValue(String value) {
            try {
                return cipher.encrypt(value);
            } catch (InvalidCipherTextException e) {
                configurationProperty.addError("encryptedValue", "Could not encrypt the value. This usually happens when the cipher text is invalid.");
            }
            return null;
        }
    }

    public class EncryptedConfigurationValueBuilder {
        private final GoCipher cipher;
        private final String encryptedValue;
        private final ConfigurationProperty configurationProperty;

        EncryptedConfigurationValueBuilder(ConfigurationProperty configurationProperty, String encryptedValue) {
            cipher = new GoCipher();
            this.configurationProperty = configurationProperty;
            this.encryptedValue = encryptedValue;
        }

        public ConfigurationProperty build() {
            if (encryptedValue == null) {
                configurationProperty.setEncryptedValue(new EncryptedConfigurationValue());
            } else {
                validateEncryptedValue(encryptedValue);
                configurationProperty.setEncryptedValue(new EncryptedConfigurationValue(encryptedValue));
            }
            return configurationProperty;
        }

        private void validateEncryptedValue(String encryptedValue) {
            try {
                cipher.decrypt(encryptedValue);
            } catch (Exception e) {
                configurationProperty.addError("encryptedValue", "Invalid encrypted value specified. This usually happens when the value is encrypted using different cipher text.");
            }
        }
    }
}


