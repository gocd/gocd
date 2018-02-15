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

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.security.GoCipher;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ConfigurationPropertyBuilderTest {
    @Test
    public void shouldCreateConfigurationPropertyWithEncryptedValueForSecureProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, true);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", null, "enc_value", true);

        assertThat(property.getConfigKeyName(), is("key"));
        assertThat(property.getEncryptedValue(), is("enc_value"));
        assertNull(property.getConfigurationValue());
    }

    @Test
    public void shouldCreateWithEncryptedValueForOnlyPlainTextInputForSecureProperty() throws Exception {
        Property key = new Property("key");
        key.with(Property.SECURE, true);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", null, true);

        assertThat(property.getConfigKeyName(), is("key"));
        assertThat(property.getEncryptedValue(), is(new GoCipher().encrypt("value")));
        assertNull(property.getConfigurationValue());
    }

    @Test
    public void shouldCreatePropertyInAbsenceOfPlainAndEncryptedTextInputForSecureProperty() throws Exception {
        Property key = new Property("key");
        key.with(Property.SECURE, true);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", null, null, true);

        assertThat(property.errors().size(), is(0));
        assertThat(property.getConfigKeyName(), is("key"));
        assertNull(property.getEncryptedConfigurationValue());
        assertNull(property.getConfigurationValue());
    }

    @Test
    public void shouldCreateWithErrorsIfBothPlainAndEncryptedTextInputAreSpecifiedForSecureProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, true);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", "enc_value", true);

        assertThat(property.errors().get("configurationValue").get(0), is("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(property.errors().get("encryptedValue").get(0), is("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(property.getConfigurationValue().getValue(), is("value"));
        assertThat(property.getEncryptedValue(), is("enc_value"));
    }

    @Test
    public void shouldCreateWithErrorsIfBothPlainAndEncryptedTextInputAreSpecifiedForUnSecuredProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, false);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", "enc_value", false);

        assertThat(property.errors().get("configurationValue").get(0), is("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(property.errors().get("encryptedValue").get(0), is("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(property.getConfigurationValue().getValue(), is("value"));
        assertThat(property.getEncryptedValue(), is("enc_value"));
    }

    @Test
    public void shouldCreateWithErrorsInPresenceOfEncryptedTextInputForUnSecuredProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, false);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", null, "enc_value", false);

        assertThat(property.errors().get("encryptedValue").get(0), is("encrypted_value cannot be specified to a unsecured property."));
        assertThat(property.getEncryptedValue(), is("enc_value"));
    }

    @Test
    public void shouldCreateWithValueForAUnsecuredProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, false);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", null, false);

        assertThat(property.getConfigurationValue().getValue(), is("value"));
        assertNull(property.getEncryptedConfigurationValue());
    }

    @Test
    public void shouldCopyOverTheError_WhenErrorArePassedInParam() {
        Property key = new Property("key");
        key.with(Property.SECURE, false);

        final ConfigErrors errors = new ConfigErrors();
        errors.add("key", "Error on property");

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", null, false, errors);

        assertThat(property.getConfigurationValue().getValue(), is("value"));
        assertNull(property.getEncryptedConfigurationValue());
        assertThat(property.errors().on("key"), is("Error on property"));
    }
}