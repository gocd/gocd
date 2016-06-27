package com.thoughtworks.go.config.builder;

import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.plugin.api.config.Property;
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
            configurationProperty.setEncryptedConfigurationValue(new EncryptedConfigurationValue(encryptedValue));
            return configurationProperty;
        }

        if (isSecure) {
            if (isNotBlank(encryptedValue)) {
                configurationProperty.setEncryptedConfigurationValue(new EncryptedConfigurationValue(encryptedValue));
            }

            if (isNotBlank(value)) {
                configurationProperty.setEncryptedConfigurationValue(new EncryptedConfigurationValue(encrypt(value)));
            }

        } else {
            if (isNotBlank(encryptedValue)) {
                configurationProperty.addError("encryptedValue", "encrypted_value cannot be specified to a unsecured property.");
                configurationProperty.setEncryptedConfigurationValue(new EncryptedConfigurationValue(encryptedValue));
            }

            if (isNotBlank(value)) {
                configurationProperty.setConfigurationValue(new ConfigurationValue(value));
            }
        }

        return configurationProperty;
    }

    private String encrypt(String data) {
        try {
            return cipher.encrypt(data);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
