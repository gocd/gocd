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

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.exceptions.UnresolvedSecretParamException;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

public class ConfigurationPropertyTest {

    private GoCipher cipher;

    @BeforeEach
    void setUp() {
        cipher = mock(GoCipher.class);
    }

    @Test
    void shouldCheckForConfigurationPropertyEquality() {
        ConfigurationValue configurationValue = new ConfigurationValue();
        ConfigurationKey configurationKey = new ConfigurationKey();
        ConfigurationProperty configurationProperty = new ConfigurationProperty(configurationKey, configurationValue, null, null);
        assertThat(configurationProperty).isEqualTo(new ConfigurationProperty(configurationKey, configurationValue, null, null));
    }

    @Nested
    class forFingerprint {
        @Test
        void shouldGetPropertyForFingerprint() {
            ConfigurationValue configurationValue = new ConfigurationValue("value");
            ConfigurationKey configurationKey = new ConfigurationKey("key");
            ConfigurationProperty configurationProperty = new ConfigurationProperty(configurationKey, configurationValue, null, null);
            assertThat(configurationProperty.forFingerprint()).isEqualTo("key=value");
        }

        @Test
        void shouldGetKeyValuePairForFingerPrintString() {
            ConfigurationValue configurationValue = new ConfigurationValue("value");
            ConfigurationKey configurationKey = new ConfigurationKey("key");
            ConfigurationProperty configurationProperty = new ConfigurationProperty(configurationKey, configurationValue, null, null);
            assertThat(configurationProperty.forFingerprint()).isEqualTo("key=value");
        }

        @Test
        void shouldBeSameIfConfigurationPropertyValueIsNullOrEmpty() {
            ConfigurationProperty configurationPropertyWithEmptyValue = new ConfigurationProperty(new ConfigurationKey("key1"),
                    new ConfigurationValue(""), null, null);
            ConfigurationProperty configurationPropertyWithNullValue = new ConfigurationProperty(new ConfigurationKey("key1"),
                    null, null, null);

            assertThat(configurationPropertyWithEmptyValue.forFingerprint()).isEqualTo("key1=");
            assertThat(configurationPropertyWithEmptyValue.forFingerprint()).isEqualTo(configurationPropertyWithNullValue.forFingerprint());
        }
    }

    @Test
    void shouldGetEncryptValueWhenConstructedAsSecure() throws CryptoException {
        GoCipher goCipher = mock(GoCipher.class);
        String encryptedText = "encryptedValue";
        when(goCipher.encrypt("secureValue")).thenReturn(encryptedText);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), new ConfigurationValue("secureValue"), new EncryptedConfigurationValue("old-encrypted-text"),
                goCipher);
        property.handleSecureValueConfiguration(true);
        assertThat(property.isSecure()).isTrue();
        assertThat(property.getEncryptedValue()).isEqualTo(encryptedText);
        assertThat(property.getConfigurationKey().getName()).isEqualTo("secureKey");
        assertThat(property.getConfigurationValue()).isNull();
    }

    @Test
    void shouldNotEncryptWhenWhenConstructedAsNotSecure() {
        GoCipher goCipher = mock(GoCipher.class);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), new ConfigurationValue("secureValue"), null, goCipher);
        property.handleSecureValueConfiguration(false);
        assertThat(property.isSecure()).isFalse();
        assertThat(property.getConfigurationKey().getName()).isEqualTo("secureKey");
        assertThat(property.getConfigurationValue().getValue()).isEqualTo("secureValue");
        assertThat(property.getEncryptedConfigurationValue()).isNull();
    }

    @Test
    void shouldNotClearEncryptedValueWhenWhenNewValueNotProvided() {
        GoCipher goCipher = mock(GoCipher.class);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), null, new EncryptedConfigurationValue("secureValue"), goCipher);
        property.handleSecureValueConfiguration(true);
        assertThat(property.isSecure()).isTrue();
        assertThat(property.getConfigurationKey().getName()).isEqualTo("secureKey");
        assertThat(property.getConfigurationValue()).isNull();
        assertThat(property.getEncryptedConfigurationValue()).isNotNull();
        assertThat(property.getEncryptedValue()).isEqualTo("secureValue");
    }

    @Test
    void shouldSetEmptyEncryptedValueWhenValueIsEmptyAndSecure() throws Exception {
        GoCipher goCipher = mock(GoCipher.class);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), new ConfigurationValue(""), new EncryptedConfigurationValue("old"), goCipher);
        property.handleSecureValueConfiguration(true);
        assertThat(property.getEncryptedValue()).isEqualTo("");
        verify(cipher, never()).decrypt(anyString());
    }

    @Test
    void shouldFailValidationIfAPropertyDoesNotHaveValue() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), null, new EncryptedConfigurationValue("invalid-encrypted-value"), new GoCipher());
        property.validate(ConfigSaveValidationContext.forChain(property));
        assertThat(property.errors().isEmpty()).isFalse();
        assertThat(property.errors().getAllOn(ConfigurationProperty.ENCRYPTED_VALUE).contains(
                "Encrypted value for property with key 'secureKey' is invalid. This usually happens when the cipher text is modified to have an invalid value.")).isTrue();
    }

    @Test
    void shouldPassValidationIfBothNameAndValueAreProvided() {
        GoCipher cipher = mock(GoCipher.class);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("name"), new ConfigurationValue("value"), null, cipher);
        property.validate(ConfigSaveValidationContext.forChain(property));
        assertThat(property.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldPassValidationIfBothNameAndEncryptedValueAreProvidedForSecureProperty() throws CryptoException {
        String encrypted = "encrypted";
        String decrypted = "decrypted";
        when(cipher.decrypt(encrypted)).thenReturn(decrypted);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("name"), null, new EncryptedConfigurationValue(encrypted), cipher);
        property.validate(ConfigSaveValidationContext.forChain(property));
        assertThat(property.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldSetConfigAttributesForNonSecureProperty() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        HashMap attributes = new HashMap();
        HashMap keyMap = new HashMap();
        keyMap.put("name", "fooKey");
        attributes.put(ConfigurationProperty.CONFIGURATION_KEY, keyMap);
        HashMap valueMap = new HashMap();
        valueMap.put("value", "fooValue");
        attributes.put(ConfigurationProperty.CONFIGURATION_VALUE, valueMap);

        PackageConfigurations metadata = new PackageConfigurations();
        metadata.addConfiguration(new PackageConfiguration("fooKey", null));
        attributes.put(Configuration.METADATA, metadata);

        configurationProperty.setConfigAttributes(attributes, null);

        assertThat(configurationProperty.getConfigurationKey().getName()).isEqualTo("fooKey");
        assertThat(configurationProperty.getConfigurationValue().getValue()).isEqualTo("fooValue");
    }

    @Test
    void shouldInitializeConfigValueToBlankWhenBothValueAndEncryptedValueIsNull() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("key"), (ConfigurationValue) null);

        configurationProperty.initialize();

        assertThat(configurationProperty.getConfigurationKey().getName()).isEqualTo("key");
        assertThat(configurationProperty.getConfigurationValue()).isNotNull();
        assertThat(configurationProperty.getConfigurationValue().getValue()).isEqualTo("");
        assertThat(configurationProperty.getEncryptedConfigurationValue()).isNull();
        Method initializeMethod = ReflectionUtils.findMethod(ConfigurationProperty.class, "initialize");
        assertThat(initializeMethod.getAnnotation(PostConstruct.class)).isNotNull();
    }

    @Test
    void shouldSetConfigAttributesForSecurePropertyWhenUserChangesIt() throws Exception {
        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        HashMap attributes = new HashMap();
        HashMap keyMap = new HashMap();
        final String secureKey = "fooKey";
        keyMap.put("name", secureKey);
        attributes.put(ConfigurationProperty.CONFIGURATION_KEY, keyMap);
        HashMap valueMap = new HashMap();
        valueMap.put("value", "fooValue");
        attributes.put(ConfigurationProperty.CONFIGURATION_VALUE, valueMap);
        attributes.put(ConfigurationProperty.IS_CHANGED, "0");

        configurationProperty.setConfigAttributes(attributes, new SecureKeyInfoProvider() {
            @Override
            public boolean isSecure(String key) {
                return secureKey.equals(key);
            }
        });

        String encryptedValue = new GoCipher().encrypt("fooValue");

        assertThat(configurationProperty.getConfigurationKey().getName()).isEqualTo(secureKey);
        assertThat(configurationProperty.getConfigurationValue()).isNull();
        assertThat(configurationProperty.getEncryptedValue()).isEqualTo(encryptedValue);
    }

    @Test
    void shouldSetConfigAttributesForSecurePropertyWhenUserDoesNotChangeIt() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        HashMap attributes = new HashMap();
        HashMap keyMap = new HashMap();
        final String secureKey = "fooKey";
        keyMap.put("name", secureKey);
        attributes.put(ConfigurationProperty.CONFIGURATION_KEY, keyMap);
        HashMap valueMap = new HashMap();
        valueMap.put("value", "fooValue");
        attributes.put(ConfigurationProperty.CONFIGURATION_VALUE, valueMap);
        HashMap encryptedValueMap = new HashMap();
        encryptedValueMap.put("value", "encryptedValue");
        attributes.put(ConfigurationProperty.ENCRYPTED_VALUE, encryptedValueMap);

        configurationProperty.setConfigAttributes(attributes, new SecureKeyInfoProvider() {
            @Override
            public boolean isSecure(String key) {
                return secureKey.equals(key);
            }
        });

        assertThat(configurationProperty.getConfigurationKey().getName()).isEqualTo(secureKey);
        assertThat(configurationProperty.getConfigurationValue()).isNull();
        assertThat(configurationProperty.getEncryptedValue()).isEqualTo("encryptedValue");
    }

    @Test
    void shouldSetConfigAttributesWhenMetadataIsNotPassedInMap() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        HashMap attributes = new HashMap();
        HashMap keyMap = new HashMap();
        keyMap.put("name", "fooKey");
        attributes.put(ConfigurationProperty.CONFIGURATION_KEY, keyMap);
        HashMap valueMap = new HashMap();
        valueMap.put("value", "fooValue");
        attributes.put(ConfigurationProperty.CONFIGURATION_VALUE, valueMap);

        configurationProperty.setConfigAttributes(attributes, null);

        assertThat(configurationProperty.getConfigurationKey().getName()).isEqualTo("fooKey");
        assertThat(configurationProperty.getConfigurationValue().getValue()).isEqualTo("fooValue");
        assertThat(configurationProperty.getEncryptedConfigurationValue()).isNull();
    }

    @Test
    void shouldGetValueForSecureProperty() throws Exception {
        when(cipher.decrypt("encrypted-value")).thenReturn("decrypted-value");
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("key"), null, new EncryptedConfigurationValue("encrypted-value"), cipher);
        assertThat(configurationProperty.getValue()).isEqualTo("decrypted-value");
    }

    @Test
    void shouldGetEmptyValueWhenSecurePropertyValueIsNullOrEmpty() throws Exception {
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), null, new EncryptedConfigurationValue(""), cipher).getValue()).isEqualTo("");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), null, new EncryptedConfigurationValue(null), cipher).getValue()).isEqualTo("");
        verify(cipher, never()).decrypt(anyString());
    }

    @Test
    void shouldGetValueForNonSecureProperty() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"), null, cipher);
        assertThat(configurationProperty.getValue()).isEqualTo("value");
    }

    @Test
    void shouldGetEmptyValueForPropertyWhenValueIsNull() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("key"), null, null, cipher);
        assertThat(configurationProperty.getValue()).isEmpty();
    }

    @Test
    void shouldCheckIfSecureValueFieldHasNoErrors() {
        EncryptedConfigurationValue encryptedValue = new EncryptedConfigurationValue("encrypted-value");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), null, encryptedValue, cipher).doesNotHaveErrorsAgainstConfigurationValue()).isTrue();
        encryptedValue.addError("value", "some-error");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), null, encryptedValue, cipher).doesNotHaveErrorsAgainstConfigurationValue()).isFalse();
    }

    @Test
    void shouldCheckIfNonSecureValueFieldHasNoErrors() {
        ConfigurationValue configurationValue = new ConfigurationValue("encrypted-value");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), configurationValue, null, cipher).doesNotHaveErrorsAgainstConfigurationValue()).isTrue();
        configurationValue.addError("value", "some-error");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), configurationValue, null, cipher).doesNotHaveErrorsAgainstConfigurationValue()).isFalse();
    }

    @Test
    void shouldValidateKeyUniqueness() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue());
        HashMap<String, ConfigurationProperty> map = new HashMap<>();
        ConfigurationProperty original = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue());
        map.put("key", original);
        property.validateKeyUniqueness(map, "Repo");
        assertThat(property.errors().isEmpty()).isFalse();
        assertThat(property.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'key' found for Repo")).isTrue();
        assertThat(original.errors().isEmpty()).isFalse();
        assertThat(original.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'key' found for Repo")).isTrue();
    }

    @Test
    void shouldGetMaskedStringIfConfigurationPropertyIsSecure() {
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), new EncryptedConfigurationValue("value")).getDisplayValue()).isEqualTo("****");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")).getDisplayValue()).isEqualTo("value");
    }

    @Nested
    class WithSecretParams {
        @Test
        void shouldParseForSecretsInTheValue() {
            ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("{{SECRET:[secret_config_id][lookup_key]}}"));

            assertThat(property.hasSecretParams()).isTrue();
            assertThat(property.getSecretParams().size()).isEqualTo(1);
            assertThat(property.getSecretParams().get(0)).isEqualTo(new SecretParam("secret_config_id", "lookup_key"));
        }

        @Test
        void shouldParseForSecretsInTheEncryptedValue() throws CryptoException {
            String encrypted = "encrypted";
            String decrypted = "{{SECRET:[secret_config_id][lookup_key]}}";
            when(cipher.decrypt(encrypted)).thenReturn(decrypted);
            ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("name"), null, new EncryptedConfigurationValue(encrypted), cipher);

            assertThat(property.hasSecretParams()).isTrue();
            assertThat(property.getSecretParams().size()).isEqualTo(1);
            assertThat(property.getSecretParams().get(0)).isEqualTo(new SecretParam("secret_config_id", "lookup_key"));
        }

        @Test
        void shouldNotThrowExceptionWhileParsingForSecrets() throws CryptoException {
            String encrypted = "encrypted";
            when(cipher.decrypt(encrypted)).thenThrow(new RuntimeException("Some exception message"));
            final ConfigurationProperty[] property = new ConfigurationProperty[1];
            assertThatCode(() -> property[0] = new ConfigurationProperty(new ConfigurationKey("name"), null, new EncryptedConfigurationValue(encrypted), cipher)).doesNotThrowAnyException();

            assertThat(property[0].hasSecretParams()).isFalse();
        }

        @Test
        void shouldReturnMaskIfSecretParamsArePresent() {
            ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("{{SECRET:[secret_config_id][lookup_key]}}"));

            assertThat(property.getDisplayValue()).isEqualTo("****");
        }

        @Test
        void shouldReturnResolvedValue() {
            ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("{{SECRET:[secret_config_id][lookup_key]}}"));
            property.getSecretParams().get(0).setValue("some-dummy-value");

            assertThat(property.getResolvedValue()).isEqualTo("some-dummy-value");
        }

        @Test
        void shouldThrowErrorIfSecretHasNotBeenResolved() {
            ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("{{SECRET:[secret_config_id][lookup_key]}}"));

            assertThatCode(property::getResolvedValue)
                    .isInstanceOf(UnresolvedSecretParamException.class)
                    .hasMessage("SecretParam 'lookup_key' is used before it is resolved.");
        }
    }
}
