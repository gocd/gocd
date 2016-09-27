/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class EnvironmentVariableConfigTest {

    private GoCipher goCipher;

    @Before
    public void setUp() {
        goCipher = mock(GoCipher.class);
    }

    @Test
    public void shouldEncryptValueWhenConstructedAsSecure() throws InvalidCipherTextException {
        GoCipher goCipher = mock(GoCipher.class);
        String encryptedText = "encrypted";
        when(goCipher.encrypt("password")).thenReturn(encryptedText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher);
        HashMap attrs = getAttributeMap("password", "true", "true");

        environmentVariableConfig.setConfigAttributes(attrs);

        assertThat(environmentVariableConfig.getEncryptedValue(), is(encryptedText));
        assertThat(environmentVariableConfig.getName(), is("foo"));
        assertThat(environmentVariableConfig.isSecure(), is(true));
    }

    @Test
    public void shouldAssignNameAndValueForAVanillaEnvironmentVariable() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig((GoCipher) null);
        HashMap attrs = new HashMap();
        attrs.put(EnvironmentVariableConfig.NAME, "foo");
        attrs.put(EnvironmentVariableConfig.VALUE, "password");

        environmentVariableConfig.setConfigAttributes(attrs);

        assertThat(environmentVariableConfig.getValue(), is("password"));
        assertThat(environmentVariableConfig.getName(), is("foo"));
        assertThat(environmentVariableConfig.isSecure(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowUpWhenTheAttributeMapHasBothNameAndValueAreEmpty() throws InvalidCipherTextException {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig((GoCipher) null);
        HashMap attrs = new HashMap();
        attrs.put(EnvironmentVariableConfig.VALUE, "");
        environmentVariableConfig.setConfigAttributes(attrs);
    }

    @Test
    public void shouldGetPlainTextValueFromAnEncryptedValue() throws InvalidCipherTextException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String plainText = "password";
        String cipherText = "encrypted";
        when(mockGoCipher.encrypt(plainText)).thenReturn(cipherText);
        when(mockGoCipher.decrypt(cipherText)).thenReturn(plainText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(mockGoCipher);
        HashMap attrs = getAttributeMap(plainText, "true", "true");

        environmentVariableConfig.setConfigAttributes(attrs);

        assertThat(environmentVariableConfig.getValue(), is(plainText));

        verify(mockGoCipher).decrypt(cipherText);
    }

    @Test
    public void shouldGetPlainTextValue() throws InvalidCipherTextException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String plainText = "password";
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(mockGoCipher);
        HashMap attrs = getAttributeMap(plainText, "false", "1");

        environmentVariableConfig.setConfigAttributes(attrs);

        assertThat(environmentVariableConfig.getValue(), is(plainText));

        verify(mockGoCipher, never()).decrypt(anyString());
        verify(mockGoCipher, never()).encrypt(anyString());
    }

    @Test
    public void shouldReturnEncryptedValueForSecureVariables() throws InvalidCipherTextException {
        when(goCipher.encrypt("bar")).thenReturn("encrypted");
        when(goCipher.decrypt("encrypted")).thenReturn("bar");
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "foo", "bar", true);
        assertThat(environmentVariableConfig.getName(), is("foo"));
        assertThat(environmentVariableConfig.getValue(), is("bar"));
        assertThat(environmentVariableConfig.getValueForDisplay(), is(environmentVariableConfig.getEncryptedValue()));
    }

    @Test
    public void shouldReturnValueForInSecureVariables() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "foo", "bar", false);
        assertThat(environmentVariableConfig.getName(), is("foo"));
        assertThat(environmentVariableConfig.getValue(), is("bar"));
        assertThat(environmentVariableConfig.getValueForDisplay(), is("bar"));
    }

    @Test
    public void shouldEncryptValueWhenChanged() throws InvalidCipherTextException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String plainText = "password";
        String cipherText = "encrypted";
        when(mockGoCipher.encrypt(plainText)).thenReturn(cipherText);
        when(mockGoCipher.decrypt(cipherText)).thenReturn(plainText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(mockGoCipher);
        HashMap firstSubmit = getAttributeMap(plainText, "true", "true");
        environmentVariableConfig.setConfigAttributes(firstSubmit);

        assertThat(environmentVariableConfig.getEncryptedValue(), is(cipherText));
    }

    @Test
    public void shouldRetainEncryptedVariableWhenNotEdited() throws InvalidCipherTextException {
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

        assertThat(environmentVariableConfig.getEncryptedValue(), is(cipherText));
        assertThat(environmentVariableConfig.getName(), is("foo"));
        assertThat(environmentVariableConfig.isSecure(), is(true));

        verify(mockGoCipher, never()).encrypt(cipherText);
    }

    @Test
    public void shouldGetSqlCriteriaForPlainTextEnvironmentVariable() throws InvalidCipherTextException {
        String plainText = "value";
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "key", plainText, false);

        Map<String, Object> sqlCriteria = environmentVariableConfig.getSqlCriteria();
        assertThat((String) sqlCriteria.get("variableName"), is("key"));
        assertThat((String) sqlCriteria.get("variableValue"), is(plainText));
        assertThat((Boolean) sqlCriteria.get("isSecure"), is(false));

        verify(goCipher, never()).encrypt(plainText);
    }

    @Test
    public void shouldGetSqlCriteriaForSecureEnvironmentVariable() throws InvalidCipherTextException {
        String encryptedText = "encrypted";
        String plainText = "plainText";
        when(goCipher.encrypt(plainText)).thenReturn(encryptedText);
        when(goCipher.decrypt(encryptedText)).thenReturn(plainText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "key", plainText, true);

        Map<String, Object> sqlCriteria = environmentVariableConfig.getSqlCriteria();
        assertThat((String) sqlCriteria.get("variableName"), is("key"));
        assertThat((String) sqlCriteria.get("variableValue"), is(plainText));
        assertThat((Boolean) sqlCriteria.get("isSecure"), is(true));

        verify(goCipher).encrypt(plainText);
        verify(goCipher).decrypt(encryptedText);
    }

    @Test
    public void shouldAddPlainTextEnvironmentVariableToContext() {
        String key = "key";
        String plainText = "plainText";
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, key, plainText, false);
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        environmentVariableConfig.addTo(context);

        assertThat(context.getProperty(key), is(plainText));
        assertThat(context.getPropertyForDisplay(key), is(plainText));
    }

    @Test
    public void shouldAddSecureEnvironmentVariableToContext() throws InvalidCipherTextException {
        String key = "key";
        String plainText = "plainText";
        String cipherText = "encrypted";
        when(goCipher.encrypt(plainText)).thenReturn(cipherText);
        when(goCipher.decrypt(cipherText)).thenReturn(plainText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, key, plainText, true);
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        environmentVariableConfig.addTo(context);

        assertThat(context.getProperty(key), is(plainText));
        assertThat(context.getPropertyForDisplay(key), is(EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE));
    }

    @Test
    public void shouldAnnotateEnsureEncryptedMethodWithPostConstruct() throws NoSuchMethodException {
        Class<EnvironmentVariableConfig> klass = EnvironmentVariableConfig.class;
        Method declaredMethods = klass.getDeclaredMethod("ensureEncrypted");
        assertThat(declaredMethods.getAnnotation(PostConstruct.class), is(not(nullValue())));
    }

    @Test
    public void shouldErrorOutOnValidateWhenEncryptedValueIsForceChanged() throws InvalidCipherTextException {
        String plainText = "secure_value";
        String cipherText = "cipherText";
        when(goCipher.encrypt(plainText)).thenReturn(cipherText);
        when(goCipher.decrypt(cipherText)).thenThrow(new DataLengthException("last block incomplete in decryption"));

        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "secure_key", plainText, true);
        environmentVariableConfig.validate(null);
        ConfigErrors error = environmentVariableConfig.errors();
        assertThat(error.isEmpty(), is(false));
        assertThat(error.on(EnvironmentVariableConfig.VALUE),
                is("Encrypted value for variable named 'secure_key' is invalid"));
    }

    @Test
    public void shouldErrorOutOnValidateWhenCipherTextIsChanged() throws InvalidCipherTextException {
        String plainText = "secure_value";
        String cipherText = "cipherText";
        when(goCipher.encrypt(plainText)).thenReturn(cipherText);
        when(goCipher.decrypt(cipherText)).thenThrow(new InvalidCipherTextException("pad block corrupted"));

        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "secure_key", plainText, true);
        environmentVariableConfig.validate(null);
        ConfigErrors error = environmentVariableConfig.errors();
        assertThat(error.isEmpty(), is(false));
        assertThat(error.on(EnvironmentVariableConfig.VALUE),
                is("Encrypted value for variable named 'secure_key' is invalid"));
    }

    @Test
    public void shouldErrorOutOnValidateWhenEncryptedValueIsNull() throws InvalidCipherTextException {
        String plainText = "secure_value";
        String cipherText = "cipherText";
        when(goCipher.encrypt(plainText)).thenReturn(cipherText);
        when(goCipher.decrypt(cipherText)).thenThrow(new NullPointerException());

        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "secure_key", plainText, true);
        environmentVariableConfig.validate(null);
        ConfigErrors error = environmentVariableConfig.errors();
        assertThat(error.isEmpty(), is(false));
        assertThat(error.on(EnvironmentVariableConfig.VALUE),
                is("Please check the log for error details while validating environment variable 'secure_key'."));
    }

    @Test
    public void shouldNotErrorOutWhenValidationIsSuccessfulForSecureVariables() throws InvalidCipherTextException {
        String plainText = "secure_value";
        String cipherText = "cipherText";
        when(goCipher.encrypt(plainText)).thenReturn(cipherText);
        when(goCipher.decrypt(cipherText)).thenReturn(plainText);

        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "secure_key", plainText, true);
        environmentVariableConfig.validate(null);
        assertThat(environmentVariableConfig.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldNotErrorOutWhenValidationIsSuccessfulForPlainTextVariables() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, "plain_key", "plain_value", false);
        environmentVariableConfig.validate(null);
        assertThat(environmentVariableConfig.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldMaskValueIfSecure() {
        EnvironmentVariableConfig secureEnvironmentVariable = new EnvironmentVariableConfig(goCipher, "plain_key", "plain_value", true);
        Assert.assertThat(secureEnvironmentVariable.getDisplayValue(), is("****"));
    }

    @Test
    public void shouldNotMaskValueIfNotSecure() {
        EnvironmentVariableConfig secureEnvironmentVariable = new EnvironmentVariableConfig(goCipher, "plain_key", "plain_value", false);
        Assert.assertThat(secureEnvironmentVariable.getDisplayValue(), is("plain_value"));
    }

    @Test
    public void shouldCopyEnvironmentVariableConfig(){
        EnvironmentVariableConfig secureEnvironmentVariable = new EnvironmentVariableConfig(goCipher, "plain_key", "plain_value", true);
        EnvironmentVariableConfig copy = new EnvironmentVariableConfig(secureEnvironmentVariable);
        assertThat(copy.getName(), is(secureEnvironmentVariable.getName()));
        assertThat(copy.getValue(), is(secureEnvironmentVariable.getValue()));
        assertThat(copy.getEncryptedValue(), is(secureEnvironmentVariable.getEncryptedValue()));
        assertThat(copy.isSecure(), is(secureEnvironmentVariable.isSecure()));
    }

    @Test
    public void shouldNotConsiderErrorsForEqualsCheck(){
        EnvironmentVariableConfig config1 = new EnvironmentVariableConfig("name", "value");
        EnvironmentVariableConfig config2 = new EnvironmentVariableConfig("name", "value");
        config2.addError("name", "errrr");
        assertThat(config1.equals(config2), is(true));
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
    public void shouldDeserializeWithErrorFlagIfAnEncryptedVarialeHasBothClearTextAndCipherText() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", "clearText", true, "c!ph3rt3xt");
        assertThat(variable.errors().getAllOn("value"), is(Arrays.asList("You may only specify `value` or `encrypted_value`, not both!")));
        assertThat(variable.errors().getAllOn("encryptedValue"), is(Arrays.asList("You may only specify `value` or `encrypted_value`, not both!")));
    }

    @Test
    public void shouldDeserializeWithNoErrorFlagIfAnEncryptedVarialeHasClearTextWithSecureTrue() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", "clearText", true, null);
        assertTrue(variable.errors().isEmpty());
    }

    @Test
    public void shouldDeserializeWithNoErrorFlagIfAnEncryptedVarialeHasEitherClearTextWithSecureFalse() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", "clearText", false, null);
        assertTrue(variable.errors().isEmpty());
    }

    @Test
    public void shouldDeserializeWithNoErrorFlagIfAnEncryptedVariableHasCipherTextSetWithSecureTrue() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", null, true, "cipherText");
        assertTrue(variable.errors().isEmpty());
    }

    @Test
    public void shouldErrorOutForEncryptedValueBeingSetWhenSecureIsFalse() throws Exception {
        EnvironmentVariableConfig variable = new EnvironmentVariableConfig();
        variable.deserialize("PASSWORD", null, false, "cipherText");
        variable.validateTree(null);

        assertThat(variable.errors().getAllOn("encryptedValue"), is(Arrays.asList("You may specify encrypted value only when option 'secure' is true.")));
    }
}
