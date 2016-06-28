package com.thoughtworks.go.config.builder;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.security.GoCipher;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ConfigurationPropertyBuilderTest {
    @Test
    public void shouldCreateConfigurationPropertyWithEncyrptedValueForSecureProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, true);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", null, "enc_value", key);

        assertThat(property.getConfigKeyName(), is("key"));
        assertThat(property.getEncryptedValue().getValue(), is("enc_value"));
        assertNull(property.getConfigurationValue());
    }

    @Test
    public void shouldCreateWithEncyrptedValueForOnlyPlainTextInputForSecureProperty() throws Exception {
        Property key = new Property("key");
        key.with(Property.SECURE, true);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", null, key);

        assertThat(property.getConfigKeyName(), is("key"));
        assertThat(property.getEncryptedValue().getValue(), is(new GoCipher().encrypt("value")));
        assertNull(property.getConfigurationValue());
    }

    @Test
    public void shouldCreatePropertyInAbsenceOfPlainAndEncryptedTextInputForSecureProperty() throws Exception {
        Property key = new Property("key");
        key.with(Property.SECURE, true);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", null, null, key);

        assertThat(property.getConfigKeyName(), is("key"));
        assertNull(property.getEncryptedValue());
        assertNull(property.getConfigurationValue());
    }

    @Test
    public void shouldCreateWithErrorsIfBothPlainAndEncryptedTextInputAreSpecifiedForSecureProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, true);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", "enc_value", key);

        assertThat(property.errors().get("configurationValue").get(0), is("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(property.errors().get("encryptedValue").get(0), is("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(property.getConfigurationValue().getValue(), is("value"));
        assertThat(property.getEncryptedValue().getValue(), is("enc_value"));
    }

    @Test
    public void shouldCreateWithErrorsIfBothPlainAndEncryptedTextInputAreSpecifiedForUnSecuredProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, false);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", "enc_value", key);

        assertThat(property.errors().get("configurationValue").get(0), is("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(property.errors().get("encryptedValue").get(0), is("You may only specify `value` or `encrypted_value`, not both!"));
        assertThat(property.getConfigurationValue().getValue(), is("value"));
        assertThat(property.getEncryptedValue().getValue(), is("enc_value"));
    }

    @Test
    public void shouldCreateWithErrorsInPresenceOfEncryptedTextInputForUnSecuredProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, false);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", null, "enc_value", key);

        assertThat(property.errors().get("encryptedValue").get(0), is("encrypted_value cannot be specified to a unsecured property."));
        assertThat(property.getEncryptedValue().getValue(), is("enc_value"));
    }

    @Test
    public void shouldCreateWithValueForAUnsecuredProperty() {
        Property key = new Property("key");
        key.with(Property.SECURE, false);

        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", null, key);

        assertThat(property.getConfigurationValue().getValue(), is("value"));
        assertNull(property.getEncryptedValue());
    }

    @Test
    public void shouldCreateInAbsenceOfProperty() {
        ConfigurationProperty property = new ConfigurationPropertyBuilder().create("key", "value", "enc_value", null);

        assertThat(property.getConfigurationValue().getValue(), is("value"));
        assertThat(property.getEncryptedValue().getValue(), is("enc_value"));
    }

}
