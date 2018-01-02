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

package com.thoughtworks.go.domain.scm;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMPreference;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.DataStructureUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;

import static com.thoughtworks.go.plugin.access.scm.SCMConfiguration.PART_OF_IDENTITY;
import static com.thoughtworks.go.plugin.access.scm.SCMConfiguration.SECURE;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SCMTest {
    @Before
    public void setup() throws Exception {
        SCMMetadataStore.getInstance().clear();
    }

    @After
    public void tearDown() throws Exception {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldCheckEqualityOfSCM() {
        Configuration configuration = new Configuration();
        SCM scm = SCMMother.create("id", "name", "plugin-id", "version", configuration);
        assertThat(scm, is(SCMMother.create("id", "name", "plugin-id", "version", configuration)));
    }

    @Test
    public void shouldCheckForFieldAssignments() {
        Configuration configuration = new Configuration();
        SCM scm = SCMMother.create("id", "name", "plugin-id", "version", configuration);
        assertThat(scm.getId(), is("id"));
        assertThat(scm.getName(), is("name"));
        assertThat(scm.getPluginConfiguration().getId(), is("plugin-id"));
        assertThat(scm.getPluginConfiguration().getVersion(), is("version"));
        assertThat(scm.getConfiguration().listOfConfigKeys().isEmpty(), is(true));
    }

    @Test
    public void shouldOnlyDisplayFieldsWhichAreNonSecureAndPartOfIdentityInGetConfigForDisplayWhenPluginExists() throws Exception {
        SCMConfigurations scmConfiguration = new SCMConfigurations();
        scmConfiguration.add(new SCMConfiguration("key1").with(PART_OF_IDENTITY, true).with(SECURE, false));
        scmConfiguration.add(new SCMConfiguration("key2").with(PART_OF_IDENTITY, false).with(SECURE, false));
        scmConfiguration.add(new SCMConfiguration("key3").with(PART_OF_IDENTITY, true).with(SECURE, true));
        scmConfiguration.add(new SCMConfiguration("key4").with(PART_OF_IDENTITY, false).with(SECURE, true));
        scmConfiguration.add(new SCMConfiguration("key5").with(PART_OF_IDENTITY, true).with(SECURE, false));
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfiguration, null);

        Configuration configuration = new Configuration(create("key1", false, "value1"), create("key2", false, "value2"), create("key3", true, "value3"), create("key4", true, "value4"), create("key5", false, "value5"));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay(), is("[key1=value1, key5=value5]"));
    }

    @Test
    public void shouldConvertKeysToLowercaseInGetConfigForDisplay() throws Exception {
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", new SCMConfigurations(), null);

        Configuration configuration = new Configuration(create("kEY1", false, "vALue1"), create("KEY_MORE_2", false, "VALUE_2"), create("key_3", false, "value3"));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay(), is("[key1=vALue1, key_more_2=VALUE_2, key_3=value3]"));
    }

    @Test
    public void shouldNotDisplayEmptyValuesInGetConfigForDisplay() throws Exception {
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", new SCMConfigurations(), null);

        Configuration configuration = new Configuration(create("rk1", false, ""), create("rk2", false, "some-non-empty-value"), create("rk3", false, null));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay(), is("[rk2=some-non-empty-value]"));
    }

    @Test
    public void shouldDisplayAllNonSecureFieldsInGetConfigForDisplayWhenPluginDoesNotExist() {
        Configuration configuration = new Configuration(create("key1", false, "value1"), create("key2", true, "value2"), create("key3", false, "value3"));
        SCM scm = SCMMother.create("scm", "scm-name", "some-plugin-which-does-not-exist", "1.0", configuration);

        assertThat(scm.getConfigForDisplay(), is("WARNING! Plugin missing. [key1=value1, key3=value3]"));
    }

    @Test
    public void shouldMakeConfigurationSecureBasedOnMetadata() throws Exception {
        GoCipher goCipher = new GoCipher();

        //meta data of SCM
        SCMConfigurations scmConfiguration = new SCMConfigurations();
        scmConfiguration.add(new SCMConfiguration("key1").with(SECURE, true));
        scmConfiguration.add(new SCMConfiguration("key2").with(SECURE, false));
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfiguration, null);

        /*secure property is set based on metadata*/
        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        SCM scm = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(secureProperty, nonSecureProperty));

        scm.applyPluginMetadata();

        //assert SCM properties
        assertThat(secureProperty.isSecure(), is(true));
        assertThat(secureProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureProperty.getEncryptedValue(), is(goCipher.encrypt("value1")));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));
    }

    @Test
    public void shouldNotUpdateSecurePropertyWhenPluginIsMissing() {
        GoCipher goCipher = new GoCipher();

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), null, new EncryptedConfigurationValue("value"), goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value1"), null, goCipher);
        SCM scm = SCMMother.create("scm-id", "scm-name", "plugin-id", "version", new Configuration(secureProperty, nonSecureProperty));

        scm.applyPluginMetadata();

        assertThat(secureProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureProperty.getConfigurationValue(), is(nullValue()));

        assertThat(nonSecureProperty.getConfigurationValue(), is(notNullValue()));
        assertThat(nonSecureProperty.getEncryptedConfigurationValue(), is(nullValue()));
    }

    @Test
    public void shouldThrowUpOnSetConfigAttributesIfPluginIsNotAvailable() throws Exception {
        try {
            Map<String, String> attributeMap = DataStructureUtils.m(SCM.SCM_ID, "scm-id", SCM.NAME, "scm-name", SCM.AUTO_UPDATE, "false", "url", "http://localhost");
            SCM scm = new SCM(null, new PluginConfiguration("plugin-id", "1"), new Configuration());
            scm.setConfigAttributes(attributeMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e, instanceOf(RuntimeException.class));
            assertThat(e.getMessage(), is("metadata unavailable for plugin: plugin-id"));
        }
    }

    @Test
    public void shouldSetConfigAttributesAsAvailable() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("url"));
        scmConfigurations.add(new SCMConfiguration("username"));
        scmConfigurations.add(new SCMConfiguration("password").with(SECURE, true));
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfigurations, null);

        Map<String, String> attributeMap = DataStructureUtils.m(SCM.SCM_ID, "scm-id", SCM.NAME, "scm-name", SCM.AUTO_UPDATE, "false", "url", "http://localhost", "username", "user", "password", "pass");
        SCM scm = new SCM(null, new PluginConfiguration("plugin-id", "1"), new Configuration());
        scm.setConfigAttributes(attributeMap);

        assertThat(scm.getId(), is("scm-id"));
        assertThat(scm.getName(), is("scm-name"));
        assertThat(scm.isAutoUpdate(), is(false));
        assertThat(scm.getPluginConfiguration().getId(), is("plugin-id"));

        assertThat(scm.getConfigAsMap().get("url").get(SCM.VALUE_KEY), is("http://localhost"));
        assertThat(scm.getConfigAsMap().get("username").get(SCM.VALUE_KEY), is("user"));
        assertThat(scm.getConfigAsMap().get("password").get(SCM.VALUE_KEY), is("pass"));

        assertThat(scm.getConfiguration().getProperty("password").getConfigurationValue(), is(nullValue()));
        assertThat(scm.getConfiguration().getProperty("password").getEncryptedConfigurationValue(), is(not(nullValue())));
    }

    @Test
    public void shouldPopulateItselfFromConfigAttributesMap() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("KEY1"));
        scmConfigurations.add(new SCMConfiguration("Key2"));

        SCMPreference scmPreference = mock(SCMPreference.class);
        when(scmPreference.getScmConfigurations()).thenReturn(scmConfigurations);
        SCMMetadataStore.getInstance().setPreferenceFor("plugin-id", scmPreference);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("Key2"));
        SCM scm = new SCM("scm-id", new PluginConfiguration("plugin-id", "1"), configuration);

        Map<String, String> attributeMap = DataStructureUtils.m("KEY1", "value1", "Key2", "value2");
        scm.setConfigAttributes(attributeMap);

        assertThat(scm.getConfigAsMap().get("KEY1").get(SCM.VALUE_KEY), is("value1"));
        assertThat(scm.getConfigAsMap().get("Key2").get(SCM.VALUE_KEY), is("value2"));
    }

    @Test
    public void shouldNotOverwriteValuesIfTheyAreNotAvailableInConfigAttributesMap() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("KEY1"));
        scmConfigurations.add(new SCMConfiguration("Key2"));

        SCMPreference scmPreference = mock(SCMPreference.class);
        when(scmPreference.getScmConfigurations()).thenReturn(scmConfigurations);
        SCMMetadataStore.getInstance().setPreferenceFor("plugin-id", scmPreference);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("Key2"));
        SCM scm = new SCM("scm-id", new PluginConfiguration("plugin-id", "1"), configuration);

        Map<String, String> attributeMap = DataStructureUtils.m("KEY1", "value1");
        scm.setConfigAttributes(attributeMap);

        assertThat(scm.getConfigAsMap().get("KEY1").get(SCM.VALUE_KEY), is("value1"));
        assertThat(scm.getConfigAsMap().get("Key2").get(SCM.VALUE_KEY), is(nullValue()));
    }

    @Test
    public void shouldIgnoreKeysPresentInConfigAttributesMapButNotPresentInConfigStore() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("KEY1"));

        SCMPreference scmPreference = mock(SCMPreference.class);
        when(scmPreference.getScmConfigurations()).thenReturn(scmConfigurations);
        SCMMetadataStore.getInstance().setPreferenceFor("plugin-id", scmPreference);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM scm = new SCM("scm-id", new PluginConfiguration("plugin-id", "1"), configuration);

        Map<String, String> attributeMap = DataStructureUtils.m("KEY1", "value1", "Key2", "value2");
        scm.setConfigAttributes(attributeMap);

        assertThat(scm.getConfigAsMap().get("KEY1").get(SCM.VALUE_KEY), is("value1"));
        assertFalse(scm.getConfigAsMap().containsKey("Key2"));
    }

    @Test
    public void shouldAddPropertyComingFromAttributesMapIfPresentInConfigStoreEvenIfItISNotPresentInCurrentConfiguration() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("KEY1"));
        scmConfigurations.add(new SCMConfiguration("Key2"));

        SCMPreference scmPreference = mock(SCMPreference.class);
        when(scmPreference.getScmConfigurations()).thenReturn(scmConfigurations);
        SCMMetadataStore.getInstance().setPreferenceFor("plugin-id", scmPreference);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM scm = new SCM("scm-id", new PluginConfiguration("plugin-id", "1"), configuration);

        Map<String, String> attributeMap = DataStructureUtils.m("KEY1", "value1", "Key2", "value2");
        scm.setConfigAttributes(attributeMap);

        assertThat(scm.getConfigAsMap().get("KEY1").get(SCM.VALUE_KEY), is("value1"));
        assertThat(scm.getConfigAsMap().get("Key2").get(SCM.VALUE_KEY), is("value2"));
    }

    @Test
    public void shouldValidateIfNameIsMissing() {
        SCM scm = new SCM();
        scm.validate(new ConfigSaveValidationContext(new BasicCruiseConfig(), null));

        assertThat(scm.errors().getAllOn(SCM.NAME), is(asList("Please provide name")));
    }

    @Test
    public void shouldClearConfigurationsWhichAreEmptyAndNoErrors() throws Exception {
        SCM scm = new SCM();
        scm.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-one"), new ConfigurationValue()));
        scm.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-two"), new EncryptedConfigurationValue()));
        scm.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-three"), null, new EncryptedConfigurationValue(), null));

        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("name-four"), null, new EncryptedConfigurationValue(), null);
        configurationProperty.addErrorAgainstConfigurationValue("error");
        scm.getConfiguration().add(configurationProperty);

        scm.clearEmptyConfigurations();

        assertThat(scm.getConfiguration().size(), is(1));
        assertThat(scm.getConfiguration().get(0).getConfigurationKey().getName(), is("name-four"));
    }

    @Test
    public void shouldValidateName() throws Exception {
        SCM scm = new SCM();
        scm.setName("some name");

        scm.validate(new ConfigSaveValidationContext(null));

        assertThat(scm.errors().isEmpty(), is(false));
        assertThat(scm.errors().getAllOn(SCM.NAME).get(0),
                is("Invalid SCM name 'some name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateUniqueKeysInConfiguration() {
        ConfigurationProperty one = create("one", false, "value1");
        ConfigurationProperty duplicate1 = create("ONE", false, "value2");
        ConfigurationProperty duplicate2 = create("ONE", false, "value3");
        ConfigurationProperty two = create("two", false, null);

        SCM scm = new SCM();
        scm.setConfiguration(new Configuration(one, duplicate1, duplicate2, two));
        scm.setName("git");

        scm.validate(null);

        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for SCM 'git'"), is(true));
        assertThat(duplicate1.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for SCM 'git'"), is(true));
        assertThat(duplicate2.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for SCM 'git'"), is(true));
        assertThat(two.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldGetConfigAsMap() throws Exception {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("test-plugin-id", "13.4");

        GoCipher cipher = new GoCipher();
        List<String> keys = Arrays.asList("Avengers 1", "Avengers 2", "Avengers 3", "Avengers 4");
        List<String> values = Arrays.asList("Iron man", "Hulk", "Thor", "Captain America");

        Configuration configuration = new Configuration(
                new ConfigurationProperty(new ConfigurationKey(keys.get(0)), new ConfigurationValue(values.get(0))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(1)), new ConfigurationValue(values.get(1))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(2)), new ConfigurationValue(values.get(2))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(3)), new ConfigurationValue(values.get(3)),
                        new EncryptedConfigurationValue(cipher.encrypt(values.get(3))), cipher));

        SCM scm = new SCM("scm-id", pluginConfiguration, configuration);

        Map<String, Map<String, String>> configMap = scm.getConfigAsMap();

        assertThat(configMap.keySet().size(), is(keys.size()));
        assertThat(configMap.values().size(), is(values.size()));
        assertThat(configMap.keySet().containsAll(keys), is(true));
        for (int i = 0; i < keys.size(); i++) {
            assertThat(configMap.get(keys.get(i)).get(SCM.VALUE_KEY), is(values.get(i)));
        }
    }

    @Test
    public void shouldGenerateIdIfNotAssigned() {
        SCM scm = new SCM();
        scm.ensureIdExists();
        assertThat(scm.getId(), is(notNullValue()));

        scm = new SCM();
        scm.setId("id");
        scm.ensureIdExists();
        assertThat(scm.getId(), is("id"));
    }

    @Test
    public void shouldAddConfigurationPropertiesForAnyPlugin() {
        List<ConfigurationProperty> configurationProperties = Arrays.asList(ConfigurationPropertyMother.create("key", "value", "encValue"));
        Configuration configuration = new Configuration();
        SCM scm = SCMMother.create("id", "name", "does_not_exist", "1.1", configuration);

        assertThat(configuration.size(), is(0));

        scm.addConfigurations(configurationProperties);

        assertThat(configuration.size(), is(1));
    }

    @Test
    public void shouldGetSCMTypeCorrectly() {
        SCM scm = SCMMother.create("scm-id");
        assertThat(scm.getSCMType(), is("pluggable_material_plugin"));

        scm.setPluginConfiguration(new PluginConfiguration("plugin-id-2", "1"));
        assertThat(scm.getSCMType(), is("pluggable_material_plugin_id_2"));
    }
}
