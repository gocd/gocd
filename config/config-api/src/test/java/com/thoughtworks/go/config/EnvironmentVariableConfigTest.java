/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class EnvironmentVariableConfigTest {

    private GoCipher goCipher;

    @BeforeEach
    void setUp() {
        goCipher = mock(GoCipher.class);
    }

    @Test
    void shouldEncryptValueWhenConstructedAsSecure() throws CryptoException {
        GoCipher goCipher = mock(GoCipher.class);
        String encryptedText = "encrypted";
        when(goCipher.encrypt("password")).thenReturn(encryptedText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher);
        HashMap attrs = getAttributeMap("password", "true", "true");

        environmentVariableConfig.setConfigAttributes(attrs);

        assertThat(environmentVariableConfig.getEncryptedValue()).isEqualTo(encryptedText);
        assertThat(environmentVariableConfig.getName()).isEqualTo("foo");
        assertThat(environmentVariableConfig.isSecure()).isTrue();
    }

    @Test
    void shouldAssignNameAndValueForAVanillaEnvironmentVariable() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig((GoCipher) null);
        HashMap attrs = new HashMap();
        attrs.put(EnvironmentVariableConfig.NAME, "foo");
        attrs.put(EnvironmentVariableConfig.VALUE, "password");

        environmentVariableConfig.setConfigAttributes(attrs);

        assertThat(environmentVariableConfig.getValue()).isEqualTo("password");
        assertThat(environmentVariableConfig.getName()).isEqualTo("foo");
        assertThat(environmentVariableConfig.isSecure()).isFalse();
    }

    @Test
    void shouldThrowUpWhenTheAttributeMapHasBothNameAndValueAreEmpty() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig((GoCipher) null);
        HashMap attrs = new HashMap();
        attrs.put(EnvironmentVariableConfig.VALUE, "");

        assertThatCode(() -> environmentVariableConfig.setConfigAttributes(attrs))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldGetPlainTextValueFromAnEncryptedValue() throws CryptoException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String plainText = "password";
        String cipherText = "encrypted";
        when(mockGoCipher.encrypt(plainText)).thenReturn(cipherText);
        when(mockGoCipher.decrypt(cipherText)).thenReturn(plainText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(mockGoCipher);
        HashMap attrs = getAttributeMap(plainText, "true", "true");

        environmentVariableConfig.setConfigAttributes(attrs);

        assertThat(environmentVariableConfig.getValue()).isEqualTo(plainText);

        verify(mockGoCipher).decrypt(cipherText);
    }

    @Test
    void shouldGetPlainTextValue() throws CryptoException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String plainText = "password";
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(mockGoCipher);
        HashMap attrs = getAttributeMap(plainText, "false", "1");

        environmentVariableConfig.setConfigAttributes(attrs);

        assertThat(environmentVariableConfig.getValue()).isEqualTo(plainText);

        verify(mockGoCipher, never()).decrypt(anyString());
        verify(mockGoCipher, never()).encrypt(anyString());
    }

    @Test
    void shouldReturnEncryptedValueForSecureVariables() throws CryptoException {
        when(goCipher.encrypt("bar")).thenReturn("encrypted");
        when(goCipher.decrypt("encrypted")).thenReturn("bar");
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "foo", "bar", true);
        assertThat(environmentVariableConfig.getName()).isEqualTo("foo");
        assertThat(environmentVariableConfig.getValue()).isEqualTo("bar");
        assertThat(environmentVariableConfig.getValueForDisplay()).isEqualTo(environmentVariableConfig.getEncryptedValue());
    }

    @Test
    void shouldReturnValueForInSecureVariables() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "foo", "bar", false);
        assertThat(environmentVariableConfig.getName()).isEqualTo("foo");
        assertThat(environmentVariableConfig.getValue()).isEqualTo("bar");
        assertThat(environmentVariableConfig.getValueForDisplay()).isEqualTo("bar");
    }

    @Test
    void shouldEncryptValueWhenChanged() throws CryptoException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String plainText = "password";
        String cipherText = "encrypted";
        when(mockGoCipher.encrypt(plainText)).thenReturn(cipherText);
        when(mockGoCipher.decrypt(cipherText)).thenReturn(plainText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(mockGoCipher);
        HashMap firstSubmit = getAttributeMap(plainText, "true", "true");
        environmentVariableConfig.setConfigAttributes(firstSubmit);

        assertThat(environmentVariableConfig.getEncryptedValue()).isEqualTo(cipherText);
    }

    @Test
    void shouldRetainEncryptedVariableWhenNotEdited() throws CryptoException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String plainText = "password";
        String cipherText = "encrypted";
        when(mockGoCipher.encrypt(plainText)).thenReturn(cipherText);
        when(mockGoCipher.decrypt(cipherText)).thenReturn(plainText);
        when(mockGoCipher.encrypt(cipherText)).thenReturn("SHOULD NOT DO THIS");
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(mockGoCipher);
        HashMap firstSubmit = getAttributeMap(plainText, "true", "true");
        environmentVariableConfig.setConfigAttributes(firstSubmit);

        HashMap secondSubmit = getAttributeMap(cipherText, "true", "false");
        environmentVariableConfig.setConfigAttributes(secondSubmit);

        assertThat(environmentVariableConfig.getEncryptedValue()).isEqualTo(cipherText);
        assertThat(environmentVariableConfig.getName()).isEqualTo("foo");
        assertThat(environmentVariableConfig.isSecure()).isTrue();

        verify(mockGoCipher, never()).encrypt(cipherText);
    }

    @Test
    void shouldAddPlainTextEnvironmentVariableToContext() {
        String key = "key";
        String plainText = "plainText";
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, key, plainText, false);
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        environmentVariableConfig.addTo(context);

        assertThat(context.getProperty(key)).isEqualTo(plainText);
        assertThat(context.getPropertyForDisplay(key)).isEqualTo(plainText);
    }

    @Test
    void shouldAddSecureEnvironmentVariableToContext() throws CryptoException {
        String key = "key";
        String plainText = "plainText";
        String cipherText = "encrypted";
        when(goCipher.encrypt(plainText)).thenReturn(cipherText);
        when(goCipher.decrypt(cipherText)).thenReturn(plainText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, key, plainText, true);
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        environmentVariableConfig.addTo(context);

        assertThat(context.getProperty(key)).isEqualTo(plainText);
        assertThat(context.getPropertyForDisplay(key)).isEqualTo(EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE);
    }

    @Test
    void shouldAnnotateEnsureEncryptedMethodWithPostConstruct() throws NoSuchMethodException {
        Class<EnvironmentVariableConfig> klass = EnvironmentVariableConfig.class;
        Method declaredMethods = klass.getDeclaredMethod("ensureEncrypted");
        assertThat(declaredMethods.getAnnotation(PostConstruct.class)).isNotNull();
    }

    @Nested
    class validate {


        @Test
        void shouldErrorOutOnValidateWhenEncryptedValueIsForceChanged() throws CryptoException {
            String plainText = "secure_value";
            String cipherText = "cipherText";
            when(goCipher.encrypt(plainText)).thenReturn(cipherText);
            when(goCipher.decrypt(cipherText)).thenThrow(new CryptoException("last block incomplete in decryption"));

            EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "secure_key", plainText, true);
            environmentVariableConfig.validate(null);
            ConfigErrors error = environmentVariableConfig.errors();
            assertThat(error.isEmpty()).isFalse();
            assertThat(error.on(EnvironmentVariableConfig.VALUE)).isEqualTo("Encrypted value for variable named 'secure_key' is invalid. This usually happens when the cipher text is modified to have an invalid value.");
        }

        @Test
        void shouldNotErrorOutWhenValidationIsSuccessfulForSecureVariables() throws CryptoException {
            String plainText = "secure_value";
            String cipherText = "cipherText";
            when(goCipher.encrypt(plainText)).thenReturn(cipherText);
            when(goCipher.decrypt(cipherText)).thenReturn(plainText);

            EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "secure_key", plainText, true);
            environmentVariableConfig.validate(null);
            assertThat(environmentVariableConfig.errors().isEmpty()).isTrue();
        }

        @Test
        void shouldNotErrorOutWhenValidationIsSuccessfulForPlainTextVariables() {
            EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "plain_key", "plain_value", false);
            environmentVariableConfig.validate(null);
            assertThat(environmentVariableConfig.errors().isEmpty()).isTrue();
        }

        @Test
        void shouldBeInvalidIfSecretParamContainsANonExistentSecretConfigId() {
            ValidationContext validationContext = mock(ValidationContext.class);
            CruiseConfig cruiseConfig = mock(CruiseConfig.class);
            EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "plain_key", "{{SECRET:[secret_config_id][token]}}", false);

            when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.getSecretConfigs()).thenReturn(new SecretConfigs());

            environmentVariableConfig.validate(validationContext);

            assertThat(environmentVariableConfig.errors()).isNotEmpty();
            assertThat(environmentVariableConfig.errors().get(EnvironmentVariableConfig.VALUE))
                    .contains("Secret config with ids `secret_config_id` does not exist.");
        }

        @Test
        void shouldBeValidIfSecretParamContainsAExistentSecretConfigId() {
            ValidationContext validationContext = mock(ValidationContext.class);
            CruiseConfig cruiseConfig = mock(CruiseConfig.class);
            EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "plain_key", "{{SECRET:[secret_config_id][token]}}", false);

            when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.getSecretConfigs()).thenReturn(new SecretConfigs(new SecretConfig("secret_config_id", "cd.go.secret.file")));

            environmentVariableConfig.validate(validationContext);

            assertThat(environmentVariableConfig.errors()).isEmpty();
        }
    }

    @Test
    void shouldMaskValueIfSecure() {
        EnvironmentVariableConfig secureEnvironmentVariable = new EnvironmentVariableConfig(goCipher, "plain_key", "plain_value", true);
        assertThat(secureEnvironmentVariable.getDisplayValue()).isEqualTo("****");
    }

    @Test
    void shouldNotMaskValueIfNotSecure() {
        EnvironmentVariableConfig secureEnvironmentVariable = new EnvironmentVariableConfig(goCipher, "plain_key", "plain_value", false);
        assertThat(secureEnvironmentVariable.getDisplayValue()).isEqualTo("plain_value");
    }

    @Test
    void shouldCopyEnvironmentVariableConfig() {
        EnvironmentVariableConfig secureEnvironmentVariable = new EnvironmentVariableConfig(goCipher, "plain_key", "plain_value", true);
        EnvironmentVariableConfig copy = new EnvironmentVariableConfig(secureEnvironmentVariable);
        assertThat(copy.getName()).isEqualTo(secureEnvironmentVariable.getName());
        assertThat(copy.getValue()).isEqualTo(secureEnvironmentVariable.getValue());
        assertThat(copy.getEncryptedValue()).isEqualTo(secureEnvironmentVariable.getEncryptedValue());
        assertThat(copy.isSecure()).isEqualTo(secureEnvironmentVariable.isSecure());
    }

    @Test
    void shouldNotConsiderErrorsForEqualsCheck() {
        EnvironmentVariableConfig config1 = new EnvironmentVariableConfig("name", "value");
        EnvironmentVariableConfig config2 = new EnvironmentVariableConfig("name", "value");
        config2.addError("name", "errrr");
        assertThat(config1.equals(config2)).isTrue();
    }

    private HashMap getAttributeMap(String value, final String secure, String isChanged) {
        HashMap attrs;
        attrs = new HashMap();
        attrs.put(EnvironmentVariableConfig.NAME, "foo");
        attrs.put(EnvironmentVariableConfig.VALUE, value);
        attrs.put(EnvironmentVariableConfig.SECURE, secure);
        attrs.put(EnvironmentVariableConfig.ISCHANGED, isChanged);
        return attrs;
    }

    @Test
    void shouldDeserializeWithErrorFlagIfAnEncryptedVarialeHasBothClearTextAndCipherText() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", "clearText", true, "c!ph3rt3xt");
        assertThat(variable.errors().getAllOn("value")).isEqualTo(Arrays.asList("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(variable.errors().getAllOn("encryptedValue")).isEqualTo(Arrays.asList("You may only specify `value` or `encrypted_value`, not both!"));
    }

    @Test
    void shouldDeserializeWithNoErrorFlagIfAnEncryptedVarialeHasClearTextWithSecureTrue() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", "clearText", true, null);
        assertThat(variable.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldDeserializeWithNoErrorFlagIfAnEncryptedVarialeHasEitherClearTextWithSecureFalse() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", "clearText", false, null);
        assertThat(variable.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldDeserializeWithNoErrorFlagIfAnEncryptedVariableHasCipherTextSetWithSecureTrue() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", null, true, "cipherText");
        assertThat(variable.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldErrorOutForEncryptedValueBeingSetWhenSecureIsFalse() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", null, false, "cipherText");
        variable.validateTree(null);

        assertThat(variable.errors().getAllOn("encryptedValue")).isEqualTo(Arrays.asList("You may specify encrypted value only when option 'secure' is true."));
    }
}
