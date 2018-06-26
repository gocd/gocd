/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.config;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigurationTest {

    @Test
    public void shouldCheckForEqualityForConfiguration() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        Configuration configuration = new Configuration(configurationProperty);
        assertThat(configuration, is(new Configuration(configurationProperty)));
    }

    @Test
    public void shouldGetConfigForDisplay() {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, null);
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, null);

        Configuration config = new Configuration(property1, property2);

        assertThat(config.forDisplay(asList(property1)), is("[key1=value1]"));
        assertThat(config.forDisplay(asList(property1, property2)), is("[key1=value1, key2=value2]"));
    }

    @Test
    public void shouldNotGetValuesOfSecureKeysInConfigForDisplay() {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, null);
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, null);
        ConfigurationProperty property3 = new ConfigurationProperty(new ConfigurationKey("secure"), null, new EncryptedConfigurationValue("secured-value"), null);

        Configuration config = new Configuration(property1, property2, property3);

        assertThat(config.forDisplay(asList(property1, property2, property3)), is("[key1=value1, key2=value2]"));
    }

    @Test
    public void shouldGetConfigurationKeysAsList() {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, null);
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, null);
        Configuration config = new Configuration(property1, property2);
        assertThat(config.listOfConfigKeys(), is(asList("key1", "key2")));
    }

    @Test
    public void shouldGetConfigPropertyForGivenKey() {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, null);
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, null);
        Configuration config = new Configuration(property1, property2);
        assertThat(config.getProperty("key2"), is(property2));
    }

    @Test
    public void shouldGetNullIfPropertyNotFoundForGivenKey() {
        Configuration config = new Configuration();
        assertThat(config.getProperty("key2"), is(nullValue()));
    }

    @Test
    public void shouldClearConfigurationsWhichAreEmptyAndNoErrors() throws Exception {
        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(new ConfigurationKey("name-one"), new ConfigurationValue()));
        configuration.add(new ConfigurationProperty(new ConfigurationKey("name-two"), new EncryptedConfigurationValue()));
        configuration.add(new ConfigurationProperty(new ConfigurationKey("name-three"), null, new EncryptedConfigurationValue(), null));

        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("name-four"), null, new EncryptedConfigurationValue(), null);
        configurationProperty.addErrorAgainstConfigurationValue("error");
        configuration.add(configurationProperty);

        configuration.clearEmptyConfigurations();

        assertThat(configuration.size(), is(1));
        assertThat(configuration.get(0).getConfigurationKey().getName(), is("name-four"));

    }

    @Test
    public void shouldValidateUniqueKeysAreAddedToConfiguration(){
        ConfigurationProperty one = new ConfigurationProperty(new ConfigurationKey("one"), new ConfigurationValue("value1"));
        ConfigurationProperty duplicate1 = new ConfigurationProperty(new ConfigurationKey("ONE"), new ConfigurationValue("value2"));
        ConfigurationProperty duplicate2 = new ConfigurationProperty(new ConfigurationKey("ONE"), new ConfigurationValue("value3"));
        ConfigurationProperty two = new ConfigurationProperty(new ConfigurationKey("two"), new ConfigurationValue());
        Configuration configuration = new Configuration(one, duplicate1, duplicate2, two);

        configuration.validateUniqueness("Entity");
        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Entity"), is(true));
        assertThat(duplicate1.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Entity"), is(true));
        assertThat(duplicate2.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Entity"), is(true));
        assertThat(two.errors().isEmpty(), is(true));
    }

    @Test
    public void validateTreeShouldValidateAllConfigurationProperties() {
        ConfigurationProperty outputDirectory = mock(ConfigurationProperty.class);
        ConfigurationProperty inputDirectory = mock(ConfigurationProperty.class);

        Configuration configuration = new Configuration(outputDirectory, inputDirectory);

        configuration.validateTree();

        verify(outputDirectory).validate(null);
        verify(inputDirectory).validate(null);
    }

    @Test
    public void hasErrorsShouldVerifyIfAnyConfigurationPropertyHasErrors() {
        ConfigurationProperty outputDirectory = mock(ConfigurationProperty.class);
        ConfigurationProperty inputDirectory = mock(ConfigurationProperty.class);

        when(outputDirectory.hasErrors()).thenReturn(false);
        when(inputDirectory.hasErrors()).thenReturn(true);

        Configuration configuration = new Configuration(outputDirectory, inputDirectory);

        assertTrue(configuration.hasErrors());

        verify(outputDirectory).hasErrors();
        verify(inputDirectory).hasErrors();
    }
}
