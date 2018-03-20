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
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ConfigurationPropertyBuilderTest {

    @Test
    public void shouldCreateConfigPropertyWithPlainTextValue() {
        final ConfigurationProperty configurationProperty = ConfigurationProperty.builder("key-1")
                .value("value-1")
                .build();

        assertThat(configurationProperty.getConfigKeyName(), is("key-1"));
        assertThat(configurationProperty.getConfigurationValue().getValue(), is("value-1"));
        assertThat(configurationProperty.getEncryptedValue(), nullValue());
    }

    @Test
    public void shouldCreateConfigPropertyWithPlainTextValueAndEncryptWhenPropertyIsMarkedAsSecure() throws InvalidCipherTextException {
        final ConfigurationProperty configurationProperty = ConfigurationProperty.builder("key-1")
                .value("value-1")
                .secure(true)
                .build();

        assertThat(configurationProperty.getConfigKeyName(), is("key-1"));
        assertThat(configurationProperty.getConfigurationValue(), nullValue());
        assertThat(configurationProperty.getEncryptedValue(), is(new GoCipher().encrypt("value-1")));

        assertThat(configurationProperty.errors().size(), is(0));
    }

    @Test
    public void shouldIgnoreNullValueWhenCreateConfigPropertyWithPlainText() {
        final ConfigurationProperty configurationProperty = ConfigurationProperty.builder("key-1")
                .value(null)
                .build();

        assertThat(configurationProperty.getConfigKeyName(), is("key-1"));
        assertThat(configurationProperty.getConfigurationValue().getValue(), nullValue());
        assertThat(configurationProperty.getEncryptedConfigurationValue(), nullValue());

        assertThat(configurationProperty.errors().size(), is(0));
    }

    @Test
    public void shouldIgnoreNullValueWhenCreateConfigPropertyWithPlainTextAndPropertyMarkedAsSecure() {
        final ConfigurationProperty configurationProperty = ConfigurationProperty.builder("key-1")
                .value(null)
                .secure(true)
                .build();

        assertThat(configurationProperty.getConfigKeyName(), is("key-1"));
        assertThat(configurationProperty.getConfigurationValue(), nullValue());
        assertThat(configurationProperty.getEncryptedConfigurationValue().getValue(), nullValue());

        assertThat(configurationProperty.errors().size(), is(0));
    }

    @Test
    public void shouldIgnoreEmptyValueWhenCreateConfigPropertyWithPlainTextAndPropertyMarkedAsSecure() {
        final ConfigurationProperty configurationProperty = ConfigurationProperty.builder("key-1")
                .value("")
                .secure(true)
                .build();

        assertThat(configurationProperty.getConfigKeyName(), is("key-1"));
        assertThat(configurationProperty.getConfigurationValue(), nullValue());
        assertThat(configurationProperty.getEncryptedConfigurationValue().getValue(), nullValue());

        assertThat(configurationProperty.errors().size(), is(0));
    }

    @Test
    public void shouldIgnoreNullValueWhenCreateConfigPropertyWithEncryptedValue() {
        final ConfigurationProperty configurationProperty = ConfigurationProperty.builder("key-1")
                .encryptedValue(null)
                .build();

        assertThat(configurationProperty.getConfigKeyName(), is("key-1"));
        assertThat(configurationProperty.getConfigurationValue(), nullValue());
        assertThat(configurationProperty.getEncryptedConfigurationValue().getValue(), nullValue());

        assertThat(configurationProperty.errors().size(), is(0));
    }

    @Test
    public void shouldCreateConfigPropertyUsingEncryptedValue() throws InvalidCipherTextException {
        final String encrypt = new GoCipher().encrypt("value-1");

        final ConfigurationProperty configurationProperty = ConfigurationProperty.builder("key-1")
                .encryptedValue(encrypt)
                .build();

        assertThat(configurationProperty.getConfigKeyName(), is("key-1"));
        assertThat(configurationProperty.getConfigurationValue(), nullValue());
        assertThat(configurationProperty.getValue(), is("value-1"));

        assertThat(configurationProperty.errors().size(), is(0));
    }

    @Test
    public void shouldAddErrorWhenInvalidEncryptedValueSpecified() {
        final ConfigurationProperty configurationProperty = ConfigurationProperty.builder("key-1")
                .encryptedValue("foo")
                .build();

        assertThat(configurationProperty.getConfigKeyName(), is("key-1"));
        assertThat(configurationProperty.getConfigurationValue(), nullValue());
        assertThat(configurationProperty.getEncryptedValue(), is("foo"));

        assertThat(configurationProperty.errors().size(), is(1));
        assertThat(configurationProperty.errors().on("encryptedValue"), is("Invalid encrypted value specified. This usually happens when the value is encrypted using different cipher text."));
    }
}