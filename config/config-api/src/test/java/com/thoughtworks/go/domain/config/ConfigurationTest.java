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

import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConfigurationTest {

    @Test
    void shouldCheckForEqualityForConfiguration() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        Configuration configuration = new Configuration(configurationProperty);
        assertThat(configuration).isEqualTo(new Configuration(configurationProperty));
    }

    @Test
    void shouldGetConfigForDisplay() {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, null);
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, null);

        Configuration config = new Configuration(property1, property2);

        assertThat(config.forDisplay(asList(property1))).isEqualTo("[key1=value1]");
        assertThat(config.forDisplay(asList(property1, property2))).isEqualTo("[key1=value1, key2=value2]");
    }

    @Test
    void shouldNotGetValuesOfSecureKeysInConfigForDisplay() {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, null);
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, null);
        ConfigurationProperty property3 = new ConfigurationProperty(new ConfigurationKey("secure"), null, new EncryptedConfigurationValue("secured-value"), null);

        Configuration config = new Configuration(property1, property2, property3);

        assertThat(config.forDisplay(asList(property1, property2, property3))).isEqualTo("[key1=value1, key2=value2]");
    }

    @Test
    void shouldGetConfigurationKeysAsList() {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, null);
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, null);
        Configuration config = new Configuration(property1, property2);
        assertThat(config.listOfConfigKeys()).isEqualTo(asList("key1", "key2"));
    }

    @Test
    void shouldGetConfigPropertyForGivenKey() {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, null);
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, null);
        Configuration config = new Configuration(property1, property2);
        assertThat(config.getProperty("key2")).isEqualTo(property2);
    }

    @Test
    void shouldGetNullIfPropertyNotFoundForGivenKey() {
        Configuration config = new Configuration();
        assertThat(config.getProperty("key2")).isNull();
    }

    @Test
    void shouldClearConfigurationsWhichAreEmptyAndNoErrors() throws Exception {
        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(new ConfigurationKey("name-one"), new ConfigurationValue()));
        configuration.add(new ConfigurationProperty(new ConfigurationKey("name-two"), new EncryptedConfigurationValue()));
        configuration.add(new ConfigurationProperty(new ConfigurationKey("name-three"), null, new EncryptedConfigurationValue(), null));

        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("name-four"), null, new EncryptedConfigurationValue(), null);
        configurationProperty.addErrorAgainstConfigurationValue("error");
        configuration.add(configurationProperty);

        configuration.clearEmptyConfigurations();

        assertThat(configuration.size()).isEqualTo(1);
        assertThat(configuration.get(0).getConfigurationKey().getName()).isEqualTo("name-four");

    }

    @Test
    void shouldValidateUniqueKeysAreAddedToConfiguration() {
        ConfigurationProperty one = new ConfigurationProperty(new ConfigurationKey("one"), new ConfigurationValue("value1"));
        ConfigurationProperty duplicate1 = new ConfigurationProperty(new ConfigurationKey("ONE"), new ConfigurationValue("value2"));
        ConfigurationProperty duplicate2 = new ConfigurationProperty(new ConfigurationKey("ONE"), new ConfigurationValue("value3"));
        ConfigurationProperty two = new ConfigurationProperty(new ConfigurationKey("two"), new ConfigurationValue());
        Configuration configuration = new Configuration(one, duplicate1, duplicate2, two);

        configuration.validateUniqueness("Entity");
        assertThat(one.errors().isEmpty()).isFalse();
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Entity")).isTrue();
        assertThat(duplicate1.errors().isEmpty()).isFalse();
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Entity")).isTrue();
        assertThat(duplicate2.errors().isEmpty()).isFalse();
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Entity")).isTrue();
        assertThat(two.errors().isEmpty()).isTrue();
    }

    @Test
    void validateTreeShouldValidateAllConfigurationProperties() {
        ConfigurationProperty outputDirectory = mock(ConfigurationProperty.class);
        ConfigurationProperty inputDirectory = mock(ConfigurationProperty.class);

        Configuration configuration = new Configuration(outputDirectory, inputDirectory);

        configuration.validateTree();

        verify(outputDirectory).validate(null);
        verify(inputDirectory).validate(null);
    }

    @Test
    void hasErrorsShouldVerifyIfAnyConfigurationPropertyHasErrors() {
        ConfigurationProperty outputDirectory = mock(ConfigurationProperty.class);
        ConfigurationProperty inputDirectory = mock(ConfigurationProperty.class);

        when(outputDirectory.hasErrors()).thenReturn(false);
        when(inputDirectory.hasErrors()).thenReturn(true);

        Configuration configuration = new Configuration(outputDirectory, inputDirectory);

        assertThat(configuration.hasErrors()).isTrue();

        verify(outputDirectory).hasErrors();
        verify(inputDirectory).hasErrors();
    }

    @Test
    void shouldReturnConfigWithErrorsAsMap() {
        ConfigurationProperty configurationProperty = ConfigurationPropertyMother.create("key", false, "value");
        configurationProperty.addError("key", "invalid key");
        Configuration configuration = new Configuration(configurationProperty);
        Map<String, Map<String, String>> configWithErrorsAsMap = configuration.getConfigWithErrorsAsMap();
        HashMap<Object, Object> expectedMap = new HashMap<>();
        HashMap<Object, Object> errorsMap = new HashMap<>();
        errorsMap.put("value", "value");
        ConfigErrors configErrors = new ConfigErrors();
        configErrors.add("key", "invalid key");
        errorsMap.put("errors", configErrors.getAll().toString());
        expectedMap.put("key", errorsMap);

        assertThat(configWithErrorsAsMap).isEqualTo(expectedMap);
    }

    @Test
    void shouldReturnConfigWithMetadataAsMap() throws CryptoException {
        ConfigurationProperty configurationProperty1 = ConfigurationPropertyMother.create("property1", false, "value");
        ConfigurationProperty configurationProperty2 = ConfigurationPropertyMother.create("property2", false, null);
        String password = new GoCipher().encrypt("password");
        ConfigurationProperty configurationProperty3 = ConfigurationPropertyMother.create("property3");
        configurationProperty3.setEncryptedValue(new EncryptedConfigurationValue(password));
        Configuration configuration = new Configuration(configurationProperty1, configurationProperty2, configurationProperty3);
        Map<String, Map<String, Object>> metadataAndValuesAsMap = configuration.getPropertyMetadataAndValuesAsMap();
        HashMap<Object, Object> expectedMap = new HashMap<>();
        expectedMap.put("property1", buildPropertyMap(false, "value", "value"));
        expectedMap.put("property2", buildPropertyMap(false, null, null));
        expectedMap.put("property3", buildPropertyMap(true, password, "****"));

        assertThat(metadataAndValuesAsMap).isEqualTo(expectedMap);
    }


    @Nested
    class HasSecretParams {
        @Test
        void shouldBeTrueIfAnyOfThePropertyHasSecretParam() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            Configuration configuration = new Configuration(k1, k2);

            assertThat(configuration.hasSecretParams()).isTrue();
        }

        @Test
        void shouldBeFalseIfNoneOfThePropertyDoesNotHaveSecretParams() {
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            Configuration configuration = new Configuration(k2);

            assertThat(configuration.hasSecretParams()).isFalse();
        }
    }

    @Nested
    class GetSecretParams {
        @Test
        void shouldReturnAListOfSecretParams() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "k1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            Configuration configuration = new Configuration(k1, k2);

            assertThat(configuration.getSecretParams().size()).isEqualTo(1);
            assertThat(configuration.getSecretParams().get(0)).isEqualTo(new SecretParam("secret_config_id", "lookup_password"));
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsInConfigs() {
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            Configuration configuration = new Configuration(k2);

            assertThat(configuration.getSecretParams()).isEmpty();
        }
    }

    private Map<String, Object> buildPropertyMap(boolean isSecure, String value, String displayValue) {
        Map<String, Object> metadataMap2 = new HashMap<>();
        metadataMap2.put("displayValue", displayValue);
        metadataMap2.put("value", value);
        metadataMap2.put("isSecure", isSecure);
        return metadataMap2;
    }
}
