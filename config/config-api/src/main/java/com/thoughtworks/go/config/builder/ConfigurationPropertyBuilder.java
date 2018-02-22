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

package com.thoughtworks.go.config.builder;

import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.security.GoCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;

import static org.apache.commons.lang.StringUtils.isNotBlank;

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
            setEncryptedValue(encryptedValue, configurationProperty);
            return configurationProperty;
        }

        if (isSecure) {
            if (isNotBlank(encryptedValue)) {
                setEncryptedValue(encryptedValue, configurationProperty);
            }

            if (isNotBlank(value)) {
                setEncryptedValue(encrypt(value), configurationProperty);
            }

        } else {
            if (isNotBlank(encryptedValue)) {
                configurationProperty.addError("encryptedValue", "encrypted_value cannot be specified to a unsecured property.");
                setEncryptedValue(encryptedValue, configurationProperty);
            }

            if (value != null) {
                configurationProperty.setConfigurationValue(new ConfigurationValue(value));
            }
        }

        return configurationProperty;
    }

    private void setEncryptedValue(String encryptedValue, ConfigurationProperty configurationProperty) {
        try {
            cipher.decrypt(encryptedValue);
        } catch (Exception e) {
            configurationProperty.addError(configurationProperty.getConfigKeyName(), String.format("Could not decrypt secure configuration property value for key %s.", configurationProperty.getConfigKeyName()));
        }

        configurationProperty.setEncryptedValue(new EncryptedConfigurationValue(encryptedValue));
    }

    private String encrypt(String data) {
        try {
            return cipher.encrypt(data);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
