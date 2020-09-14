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
package com.thoughtworks.go.domain.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.*;

@ConfigTag("property")
public class ConfigurationProperty implements Serializable, Validatable, SecretParamAware {

    public static final String CONFIGURATION_KEY = "configurationKey";
    public static final String CONFIGURATION_VALUE = "configurationValue";
    public static final String ENCRYPTED_VALUE = "encryptedValue";
    public static final String IS_CHANGED = "isChanged";

    @Expose
    @SerializedName("configKey")
    @ConfigSubtag
    private ConfigurationKey configurationKey;

    @Expose
    @SerializedName("configValue")
    @ConfigSubtag
    private ConfigurationValue configurationValue;

    @Expose
    @SerializedName("encryptedConfigValue")
    @ConfigSubtag
    private EncryptedConfigurationValue encryptedValue;

    private final GoCipher cipher;
    private final ConfigErrors configErrors = new ConfigErrors();
    private SecretParams secretParamsForValue = new SecretParams();

    public ConfigurationProperty() {
        this(new GoCipher());
    }

    public ConfigurationProperty(GoCipher cipher) {
        this.cipher = cipher;
    }

    public ConfigurationProperty(ConfigurationKey configurationKey, ConfigurationValue configurationValue) {
        this();
        this.configurationKey = configurationKey;
        this.setConfigurationValue(configurationValue);
    }

    public ConfigurationProperty(ConfigurationKey configurationKey, EncryptedConfigurationValue encryptedValue) {
        this();
        this.configurationKey = configurationKey;
        this.setEncryptedValue(encryptedValue);
    }

    //for tests only
    public ConfigurationProperty(ConfigurationKey configurationKey, ConfigurationValue configurationValue, EncryptedConfigurationValue encryptedValue, GoCipher cipher) {
        this.cipher = cipher == null ? new GoCipher() : cipher;
        this.configurationKey = configurationKey;
        this.setConfigurationValue(configurationValue);
        this.setEncryptedValue(encryptedValue);
    }

    public ConfigurationProperty withKey(String key) {
        setConfigurationKey(new ConfigurationKey(key));
        return this;
    }

    public ConfigurationProperty withValue(String value) {
        setConfigurationValue(new ConfigurationValue(value));
        return this;
    }

    public ConfigurationProperty withEncryptedValue(String value) {
        setEncryptedValue(value);
        return this;
    }

    public ConfigurationKey getConfigurationKey() {
        return configurationKey;
    }

    public ConfigurationValue getConfigurationValue() {
        return configurationValue;
    }

    public void setConfigurationValue(ConfigurationValue configurationValue) {
        this.configurationValue = configurationValue;
        parseSecretParams();
    }

    public void setConfigurationKey(ConfigurationKey configurationKey) {
        this.configurationKey = configurationKey;
    }

    public void setEncryptedValue(EncryptedConfigurationValue encryptedValue) {
        this.encryptedValue = encryptedValue;
        parseSecretParams();
    }

    public boolean isSecure() {
        return encryptedValue != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigurationProperty that = (ConfigurationProperty) o;

        if (configurationKey != null ? !configurationKey.equals(that.configurationKey) : that.configurationKey != null) {
            return false;
        }
        if (configurationValue != null ? !configurationValue.equals(that.configurationValue) : that.configurationValue != null) {
            return false;
        }
        return cipher.passwordEquals(encryptedValue, that.encryptedValue);
    }

    @Override
    public int hashCode() {
        int result = configurationKey != null ? configurationKey.hashCode() : 0;
        result = 31 * result + (configurationValue != null ? configurationValue.hashCode() : 0);
        result = 31 * result + cipher.passwordHashcode(encryptedValue);
        return result;
    }

    public String forFingerprint() {
        String value = getValue();

        if (isEmpty(value)) {
            value = EMPTY;
        }

        return format("%s=%s", configurationKey.getName(), value);
    }

    public EncryptedConfigurationValue getEncryptedConfigurationValue() {
        return encryptedValue;
    }

    public void handleSecureValueConfiguration(boolean isSecure) {
        if (isSecure) {
            if (configurationValue != null) {
                try {
                    encryptedValue = new EncryptedConfigurationValue(isEmpty(configurationValue.getValue()) ? "" : cipher.encrypt(configurationValue.getValue()));
                } catch (CryptoException e) {
                    throw new RuntimeException(e);
                }
                configurationValue = null;
            }
        } else {
            encryptedValue = null;
        }
        parseSecretParams();
    }

    public String getValue() {
        if (isSecure()) {
            try {
                if (isEmpty(encryptedValue.getValue())) {
                    return EMPTY;
                }
                return cipher.decrypt(encryptedValue.getValue());
            } catch (CryptoException e) {
                throw new RuntimeException(format("Could not decrypt secure configuration property value for key %s", configurationKey.getName()), e);
            }
        }
        return configurationValue == null ? EMPTY : configurationValue.getValue();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        try {
            getValue();
        } catch (Exception e) {
            addError(ENCRYPTED_VALUE, format("Encrypted value for property with key '%s' is invalid. This usually happens when the cipher text is modified to have an invalid value.",
                    configurationKey.getName()));
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

    public boolean hasErrors() {
        return !configErrors.isEmpty();
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void addErrorAgainstConfigurationValue(String message) {
        if (isSecure()) {
            getEncryptedConfigurationValue().errors().add("value", message);
        } else {
            getConfigurationValue().errors().add("value", message);
        }
    }

    public boolean doesNotHaveErrorsAgainstConfigurationValue() {
        if (isSecure()) {
            List<String> errorsOnValue = getEncryptedConfigurationValue().errors().getAllOn("value");
            return errorsOnValue == null || errorsOnValue.isEmpty();
        } else {
            List<String> errorsOnValue = getConfigurationValue().errors().getAllOn("value");
            return errorsOnValue == null || errorsOnValue.isEmpty();
        }
    }

    public void setConfigAttributes(Object attributes, SecureKeyInfoProvider secureKeyInfoProvider) {
        Map attributesMap = (Map) attributes;
        if (attributesMap.containsKey(CONFIGURATION_KEY)) {
            configurationKey = new ConfigurationKey();
            configurationKey.setConfigAttributes(attributesMap.get(CONFIGURATION_KEY));
        }
        if (attributesMap.containsKey(CONFIGURATION_VALUE)) {
            configurationValue = new ConfigurationValue("");
            configurationValue.setConfigAttributes(attributesMap.get(CONFIGURATION_VALUE));
        }
        if (attributesMap.containsKey(ENCRYPTED_VALUE)) {
            encryptedValue = new EncryptedConfigurationValue();
            encryptedValue.setConfigAttributes(attributesMap.get(ENCRYPTED_VALUE));
        }
        if (secureKeyInfoProvider == null) {
            return;
        }
        if (secureKeyInfoProvider.isSecure(configurationKey.getName())) {
            if (!attributesMap.containsKey(IS_CHANGED)) {
                setConfigurationValue(null);
            } else {
                handleSecureValueConfiguration(true);
            }
        }
        parseSecretParams();
    }

    @Override
    public String toString() {
        return "ConfigurationProperty{" +
                "encryptedValue=" + encryptedValue +
                ", configurationValue=" + configurationValue +
                ", configurationKey=" + configurationKey +
                '}';
    }

    @PostConstruct
    public void initialize() {
        if (configurationValue == null && encryptedValue == null) {
            configurationValue = new ConfigurationValue("");
        }
    }

    public void setEncryptedValue(String encryptedValue) {
        setEncryptedValue(new EncryptedConfigurationValue(encryptedValue));
    }

    public void validateKeyUniqueness(HashMap<String, ConfigurationProperty> map, String entity) {
        String name = configurationKey.getName().toLowerCase();
        ConfigurationProperty propertyWithSameKey = map.get(name);
        if (propertyWithSameKey == null) {
            map.put(name, this);
        } else {
            String errorMessage = format("Duplicate key '%s' found for %s", configurationKey.getName(), entity);
            propertyWithSameKey.addError(ConfigurationProperty.CONFIGURATION_KEY, errorMessage);
            addError(ConfigurationProperty.CONFIGURATION_KEY, errorMessage);
        }
    }

    public String getConfigKeyName() {
        return configurationKey != null ? configurationKey.getName() : null;
    }

    public String getConfigValue() {
        return configurationValue != null ? configurationValue.getValue() : null;
    }

    public String getEncryptedValue() {
        return encryptedValue != null ? encryptedValue.getValue() : null;
    }

    public String getDisplayValue() {
        if (isSecure() || hasSecretParams()) {
            return "****";
        }
        return getValue();
    }

    public void setKey(ConfigurationKey key) {
        this.configurationKey = key;
    }

    public ConfigurationKey getKey() {
        return configurationKey;
    }

    public ConfigurationProperty deserialize(String name, String value, String encryptedValue) {
        setKey(new ConfigurationKey(name));

        if (isNotBlank(value) && isNotBlank(encryptedValue)) {
            addError("value", "You may only specify `value` or `encrypted_value`, not both!");
            addError(ENCRYPTED_VALUE, "You may only specify `value` or `encrypted_value`, not both!");
        }

        if (isNotBlank(encryptedValue)) {
            setEncryptedValue(new EncryptedConfigurationValue(encryptedValue));
        }

        if (isNotBlank(value)) {
            setConfigurationValue(new ConfigurationValue(value));
        }
        return this;
    }

    private void parseSecretParams() {
        try {
            this.secretParamsForValue = SecretParams.parse(getValue());
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean hasSecretParams() {
        return !this.secretParamsForValue.isEmpty();
    }

    @Override
    public SecretParams getSecretParams() {
        return this.secretParamsForValue;
    }

    public String getResolvedValue() {
        if (hasSecretParams()) {
            return getSecretParams().substitute(getValue());
        }

        return getValue();
    }
}
