/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.GoCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isEmpty;

@ConfigTag("property")
public class ConfigurationProperty implements Serializable, Validatable {

    public static final String CONFIGURATION_KEY = "configurationKey";
    public static final String CONFIGURATION_VALUE = "configurationValue";
    public static final String ENCRYPTED_VALUE = "encryptedValue";
    public static final String IS_CHANGED = "isChanged";
    public static final String IS_SECURE = "isSecure";

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

    private GoCipher cipher = null;
    private ConfigErrors configErrors = new ConfigErrors();

    public ConfigurationProperty() {
        this.cipher = new GoCipher();
    }

    public ConfigurationProperty(ConfigurationKey configurationKey, ConfigurationValue configurationValue) {
        this();
        this.configurationKey = configurationKey;
        this.configurationValue = configurationValue;
    }

    public ConfigurationProperty(ConfigurationKey configurationKey, EncryptedConfigurationValue encryptedValue) {
        this();
        this.configurationKey = configurationKey;
        this.encryptedValue = encryptedValue;
    }

    //for tests only
    public ConfigurationProperty(ConfigurationKey configurationKey, ConfigurationValue configurationValue, EncryptedConfigurationValue encryptedValue, GoCipher cipher) {
        this.cipher = cipher;
        this.configurationKey = configurationKey;
        this.configurationValue = configurationValue;
        this.encryptedValue = encryptedValue;
    }

    public ConfigurationKey getConfigurationKey() {
        return configurationKey;
    }

    public ConfigurationValue getConfigurationValue() {
        return configurationValue;
    }

    public void setConfigurationValue(ConfigurationValue configurationValue) {
        this.configurationValue = configurationValue;
    }

    public void setConfigurationKey(ConfigurationKey configurationKey) {
        this.configurationKey = configurationKey;
    }

    public void setEncryptedConfigurationValue(EncryptedConfigurationValue encryptedValue) {
        this.encryptedValue = encryptedValue;
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
        if (encryptedValue != null ? !encryptedValue.equals(that.encryptedValue) : that.encryptedValue != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = configurationKey != null ? configurationKey.hashCode() : 0;
        result = 31 * result + (configurationValue != null ? configurationValue.hashCode() : 0);
        result = 31 * result + (encryptedValue != null ? encryptedValue.hashCode() : 0);
        return result;
    }

    public String forFingerprint() {
        return format("%s=%s", configurationKey.getName(), getValue());
    }

    public EncryptedConfigurationValue getEncryptedConfigurationValue() {
        return encryptedValue;
    }

    public void handleSecureValueConfiguration(boolean isSecure) {
        if (isSecure) {
            if (configurationValue != null) {
                try {
                    encryptedValue = new EncryptedConfigurationValue(isEmpty(configurationValue.getValue()) ? "" : cipher.encrypt(configurationValue.getValue()));
                } catch (InvalidCipherTextException e) {
                    throw new RuntimeException(e);
                }
                configurationValue = null;
            }
        } else {
            encryptedValue = null;
        }
    }

    public String getValue() {
        if (isSecure()) {
            try {
                if (isEmpty(encryptedValue.getValue())) {
                    return EMPTY;
                }
                return cipher.decrypt(encryptedValue.getValue());
            } catch (InvalidCipherTextException e) {
                throw new RuntimeException(format("Could not decrypt secure configuration property value for key %s", configurationKey.getName()), e);
            }
        }
        return configurationValue == null ? null : configurationValue.getValue();
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

    public boolean hasErrors(){
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
        if(secureKeyInfoProvider==null){
            return;
        }
        if (secureKeyInfoProvider.isSecure(configurationKey.getName())) {
            if (!attributesMap.containsKey(IS_CHANGED)) {
                setConfigurationValue(null);
            } else {
                handleSecureValueConfiguration(true);
            }
        }

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
        if (isSecure()) {
            return "****";
        }
        return getValue();
    }
}
