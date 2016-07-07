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

import java.lang.reflect.Method;
import java.util.HashMap;
import javax.annotation.PostConstruct;

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigurationPropertyTest {

    private GoCipher cipher;

    @Before
    public void setUp() throws Exception {
        cipher = mock(GoCipher.class);
    }

    @Test
    public void shouldCheckForConfigurationPropertyEquality() {
        ConfigurationValue configurationValue = new ConfigurationValue();
        ConfigurationKey configurationKey = new ConfigurationKey();
        ConfigurationProperty configurationProperty = new ConfigurationProperty(configurationKey, configurationValue, null, null);
        assertThat(configurationProperty, is(new ConfigurationProperty(configurationKey, configurationValue, null, null)));
    }

    @Test
    public void shouldGetPropertyForFingerprint() {
        ConfigurationValue configurationValue = new ConfigurationValue("value");
        ConfigurationKey configurationKey = new ConfigurationKey("key");
        ConfigurationProperty configurationProperty = new ConfigurationProperty(configurationKey, configurationValue, null, null);
        assertThat(configurationProperty.forFingerprint(), is("key=value"));
    }

    @Test
    public void shouldGetKeyValuePairForFingerPrintString() {
        ConfigurationValue configurationValue = new ConfigurationValue("value");
        ConfigurationKey configurationKey = new ConfigurationKey("key");
        ConfigurationProperty configurationProperty = new ConfigurationProperty(configurationKey, configurationValue, null, null);
        assertThat(configurationProperty.forFingerprint(), is("key=value"));
    }

    @Test
    public void shouldGetEncryptValueWhenConstructedAsSecure() throws InvalidCipherTextException {
        GoCipher goCipher = mock(GoCipher.class);
        String encryptedText = "encryptedValue";
        when(goCipher.encrypt("secureValue")).thenReturn(encryptedText);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), new ConfigurationValue("secureValue"), new EncryptedConfigurationValue("old-encrypted-text"),
                goCipher);
        property.handleSecureValueConfiguration(true);
        assertThat(property.isSecure(), is(true));
        assertThat(property.getEncryptedValue(), is(encryptedText));
        assertThat(property.getConfigurationKey().getName(), is("secureKey"));
        assertThat(property.getConfigurationValue(), is(nullValue()));
    }

    @Test
    public void shouldNotEncryptWhenWhenConstructedAsNotSecure() throws InvalidCipherTextException {
        GoCipher goCipher = mock(GoCipher.class);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), new ConfigurationValue("secureValue"), null, goCipher);
        property.handleSecureValueConfiguration(false);
        assertThat(property.isSecure(), is(false));
        assertThat(property.getConfigurationKey().getName(), is("secureKey"));
        assertThat(property.getConfigurationValue().getValue(), is("secureValue"));
        assertThat(property.getEncryptedConfigurationValue(), is(nullValue()));
    }

    @Test
    public void shouldNotClearEncryptedValueWhenWhenNewValueNotProvided() throws InvalidCipherTextException {
        GoCipher goCipher = mock(GoCipher.class);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), null, new EncryptedConfigurationValue("secureValue"), goCipher);
        property.handleSecureValueConfiguration(true);
        assertThat(property.isSecure(), is(true));
        assertThat(property.getConfigurationKey().getName(), is("secureKey"));
        assertThat(property.getConfigurationValue(), is(nullValue()));
        assertThat(property.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(property.getEncryptedValue(), is("secureValue"));
    }

    @Test
    public void shouldSetEmptyEncryptedValueWhenValueIsEmptyAndSecure() throws Exception {
        GoCipher goCipher = mock(GoCipher.class);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), new ConfigurationValue(""), new EncryptedConfigurationValue("old"), goCipher);
        property.handleSecureValueConfiguration(true);
        assertThat(property.getEncryptedValue(), is(""));
        verify(cipher, never()).decrypt(anyString());
    }

    @Test
    public void shouldFailValidationIfAPropertyDoesNotHaveValue() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("secureKey"), null, new EncryptedConfigurationValue("invalid-encrypted-value"), new GoCipher());
        property.validate(ConfigSaveValidationContext.forChain(property));
        assertThat(property.errors().isEmpty(), is(false));
        assertThat(property.errors().getAllOn(ConfigurationProperty.ENCRYPTED_VALUE).contains(
                "Encrypted value for property with key 'secureKey' is invalid. This usually happens when the cipher text is modified to have an invalid value."), is(true));
    }

    @Test
    public void shouldPassValidationIfBothNameAndValueAreProvided() throws InvalidCipherTextException {
        GoCipher cipher = mock(GoCipher.class);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("name"), new ConfigurationValue("value"), null, cipher);
        property.validate(ConfigSaveValidationContext.forChain(property));
        assertThat(property.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldPassValidationIfBothNameAndEncryptedValueAreProvidedForSecureProperty() throws InvalidCipherTextException {
        String encrypted = "encrypted";
        String decrypted = "decrypted";
        when(cipher.decrypt(encrypted)).thenReturn(decrypted);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("name"), null, new EncryptedConfigurationValue(encrypted), cipher);
        property.validate(ConfigSaveValidationContext.forChain(property));
        assertThat(property.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldSetConfigAttributesForNonSecureProperty() throws Exception {
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

        configurationProperty.setConfigAttributes(attributes,null);

        assertThat(configurationProperty.getConfigurationKey().getName(), is("fooKey"));
        assertThat(configurationProperty.getConfigurationValue().getValue(), is("fooValue"));
    }

    @Test
    public void shouldInitializeConfigValueToBlankWhenBothValueAndEncryptedValueIsNull() throws Exception {
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("key"), (ConfigurationValue) null);

        configurationProperty.initialize();

        assertThat(configurationProperty.getConfigurationKey().getName(), is("key"));
        assertThat(configurationProperty.getConfigurationValue(), is(notNullValue()));
        assertThat(configurationProperty.getConfigurationValue().getValue(), is(""));
        assertThat(configurationProperty.getEncryptedConfigurationValue(), is(nullValue()));
        Method initializeMethod = ReflectionUtils.findMethod(ConfigurationProperty.class, "initialize");
        assertThat(initializeMethod.getAnnotation(PostConstruct.class), is(notNullValue()));
    }

    @Test
    public void shouldSetConfigAttributesForSecurePropertyWhenUserChangesIt() throws Exception {
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

        configurationProperty.setConfigAttributes(attributes,new SecureKeyInfoProvider() {
            @Override
            public boolean isSecure(String key) {
                return secureKey.equals(key);
            }
        });

        String encryptedValue = new GoCipher().encrypt("fooValue");

        assertThat(configurationProperty.getConfigurationKey().getName(), is(secureKey));
        assertThat(configurationProperty.getConfigurationValue(), is(nullValue()));
        assertThat(configurationProperty.getEncryptedValue(), is(encryptedValue));
    }

    @Test
    public void shouldSetConfigAttributesForSecurePropertyWhenUserDoesNotChangeIt() throws Exception {
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

        configurationProperty.setConfigAttributes(attributes,new SecureKeyInfoProvider() {
            @Override
            public boolean isSecure(String key) {
                return secureKey.equals(key);
            }
        });

        assertThat(configurationProperty.getConfigurationKey().getName(), is(secureKey));
        assertThat(configurationProperty.getConfigurationValue(), is(nullValue()));
        assertThat(configurationProperty.getEncryptedValue(), is("encryptedValue"));
    }

    @Test
    public void shouldSetConfigAttributesWhenMetadataIsNotPassedInMap() throws Exception {
        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        HashMap attributes = new HashMap();
        HashMap keyMap = new HashMap();
        keyMap.put("name", "fooKey");
        attributes.put(ConfigurationProperty.CONFIGURATION_KEY, keyMap);
        HashMap valueMap = new HashMap();
        valueMap.put("value", "fooValue");
        attributes.put(ConfigurationProperty.CONFIGURATION_VALUE, valueMap);

        configurationProperty.setConfigAttributes(attributes,null);

        assertThat(configurationProperty.getConfigurationKey().getName(), is("fooKey"));
        assertThat(configurationProperty.getConfigurationValue().getValue(), is("fooValue"));
        assertThat(configurationProperty.getEncryptedConfigurationValue(), is(nullValue()));
    }

    @Test
    public void shouldGetValueForSecureProperty() throws Exception {
        when(cipher.decrypt("encrypted-value")).thenReturn("decrypted-value");
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("key"), null, new EncryptedConfigurationValue("encrypted-value"), cipher);
        assertThat(configurationProperty.getValue(), is("decrypted-value"));
    }

    @Test
    public void shouldGetEmptyValueWhenSecurePropertyValueIsNullOrEmpty() throws Exception {
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), null, new EncryptedConfigurationValue(""), cipher).getValue(), is(""));
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), null, new EncryptedConfigurationValue(null), cipher).getValue(), is(""));
        verify(cipher, never()).decrypt(anyString());
    }

    @Test
    public void shouldGetValueForNonSecureProperty() throws Exception {
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"), null, cipher);
        assertThat(configurationProperty.getValue(), is("value"));
    }

    @Test
    public void shouldGetNullValueForPropertyWhenValueIsNull() throws Exception {
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("key"), null, null, cipher);
        assertThat(configurationProperty.getValue(), is(nullValue()));
    }

    @Test
    public void shouldCheckIfSecureValueFieldHasNoErrors() throws Exception {
        EncryptedConfigurationValue encryptedValue = new EncryptedConfigurationValue("encrypted-value");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), null, encryptedValue, cipher).doesNotHaveErrorsAgainstConfigurationValue(), is(true));
        encryptedValue.addError("value", "some-error");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), null, encryptedValue, cipher).doesNotHaveErrorsAgainstConfigurationValue(), is(false));
    }

    @Test
    public void shouldCheckIfNonSecureValueFieldHasNoErrors() throws Exception {
        ConfigurationValue configurationValue = new ConfigurationValue("encrypted-value");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), configurationValue, null, cipher).doesNotHaveErrorsAgainstConfigurationValue(), is(true));
        configurationValue.addError("value", "some-error");
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), configurationValue, null, cipher).doesNotHaveErrorsAgainstConfigurationValue(), is(false));
    }

    @Test
    public void shouldValidateKeyUniqueness(){
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue());
        HashMap<String, ConfigurationProperty> map = new HashMap<String, ConfigurationProperty>();
        ConfigurationProperty original = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue());
        map.put("key", original);
        property.validateKeyUniqueness(map, "Repo");
        assertThat(property.errors().isEmpty(), is(false));
        assertThat(property.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'key' found for Repo"), is(true));
        assertThat(original.errors().isEmpty(), is(false));
        assertThat(original.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'key' found for Repo"), is(true));
    }

    @Test
    public void shouldGetMaskedStringIfConfigurationPropertyIsSecure() throws Exception {
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), new EncryptedConfigurationValue("value")).getDisplayValue(), is("****"));
        assertThat(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")).getDisplayValue(), is("value"));
    }
}
