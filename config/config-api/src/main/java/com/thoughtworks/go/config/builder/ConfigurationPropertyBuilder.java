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
package com.thoughtworks.go.config.builder;

import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ConfigurationPropertyBuilder {

    private GoCipher cipher;

    public ConfigurationPropertyBuilder() {
        this.cipher = new GoCipher();
    }

    public ConfigurationProperty create(String key, String value, String encryptedValue, Boolean isSecure) {

        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        configurationProperty.setConfigurationKey(new ConfigurationKey(key));

        if (isNotBlank(value) && isNotBlank(encryptedValue)) {
            configurationProperty.addError("configurationValue", "You may only specify `value` or `encrypted_value`, not both!");
            configurationProperty.addError("encryptedValue", "You may only specify `value` or `encrypted_value`, not both!");

            configurationProperty.setConfigurationValue(new ConfigurationValue(value));
            configurationProperty.setEncryptedValue(new EncryptedConfigurationValue(encryptedValue));
            return configurationProperty;
        }

        if (isSecure) {
            if (isNotBlank(encryptedValue)) {
                configurationProperty.setEncryptedValue(new EncryptedConfigurationValue(encryptedValue));
            }

            if (isNotBlank(value)) {
                configurationProperty.setEncryptedValue(new EncryptedConfigurationValue(encrypt(value)));
            }

        } else {
            if (isNotBlank(encryptedValue)) {
                configurationProperty.addError("encryptedValue", "encrypted_value cannot be specified to a unsecured property.");
                configurationProperty.setEncryptedValue(new EncryptedConfigurationValue(encryptedValue));
            }

            if (value != null) {
                configurationProperty.setConfigurationValue(new ConfigurationValue(value));
            }
        }

        if (isNotBlank(configurationProperty.getEncryptedValue())) {
            configurationProperty.setEncryptedValue(new EncryptedConfigurationValue(configurationProperty.getEncryptedValue()));
        }
        return configurationProperty;
    }

    private String encrypt(String data) {
        try {
            return cipher.encrypt(data);
        } catch (CryptoException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
