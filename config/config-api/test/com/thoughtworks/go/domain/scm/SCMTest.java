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

package com.thoughtworks.go.domain.scm;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.helper.ConfigurationHolder;
import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.security.GoCipher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;


import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;

import static com.thoughtworks.go.plugin.access.scm.SCMConfiguration.PART_OF_IDENTITY;
import static com.thoughtworks.go.plugin.access.scm.SCMConfiguration.SECURE;

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
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfiguration);

        Configuration configuration = new Configuration(create("key1", false, "value1"), create("key2", false, "value2"), create("key3", true, "value3"), create("key4", true, "value4"), create("key5", false, "value5"));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay(), is("SCM: [key1=value1, key5=value5]"));
    }

    @Test
    public void shouldConvertKeysToLowercaseInGetConfigForDisplay() throws Exception {
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", new SCMConfigurations());

        Configuration configuration = new Configuration(create("kEY1", false, "vALue1"), create("KEY_MORE_2", false, "VALUE_2"), create("key_3", false, "value3"));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay(), is("SCM: [key1=vALue1, key_more_2=VALUE_2, key_3=value3]"));
    }

    @Test
    public void shouldNotDisplayEmptyValuesInGetConfigForDisplay() throws Exception {
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", new SCMConfigurations());

        Configuration configuration = new Configuration(create("rk1", false, ""), create("rk2", false, "some-non-empty-value"), create("rk3", false, null));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay(), is("SCM: [rk2=some-non-empty-value]"));
    }

    @Test
    public void shouldDisplayAllNonSecureFieldsInGetConfigForDisplayWhenPluginDoesNotExist() {
        Configuration configuration = new Configuration(create("key1", false, "value1"), create("key2", true, "value2"), create("key3", false, "value3"));
        SCM scm = SCMMother.create("scm", "scm-name", "some-plugin-which-does-not-exist", "1.0", configuration);

        assertThat(scm.getConfigForDisplay(), is("WARNING! Plugin missing for SCM: [key1=value1, key3=value3]"));
    }

    @Test
    public void shouldMakeConfigurationSecureBasedOnMetadata() throws Exception {
        GoCipher goCipher = new GoCipher();

        //meta data of SCM
        SCMConfigurations scmConfiguration = new SCMConfigurations();
        scmConfiguration.add(new SCMConfiguration("key1").with(SECURE, true));
        scmConfiguration.add(new SCMConfiguration("key2").with(SECURE, false));
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfiguration);

        /*secure property is set based on metadata*/
        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        SCM scm = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(secureProperty, nonSecureProperty));

        scm.applyPluginMetadata();

        //assert SCM properties
        assertThat(secureProperty.isSecure(), is(true));
        assertThat(secureProperty.getEncryptedValue(), is(notNullValue()));
        assertThat(secureProperty.getEncryptedValue().getValue(), is(goCipher.encrypt("value1")));

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

        assertThat(secureProperty.getEncryptedValue(), is(notNullValue()));
        assertThat(secureProperty.getConfigurationValue(), is(nullValue()));

        assertThat(nonSecureProperty.getConfigurationValue(), is(notNullValue()));
        assertThat(nonSecureProperty.getEncryptedValue(), is(nullValue()));
    }

    @Test
    public void shouldSetConfigAttributesAsAvailable() throws Exception {
        String pluginId = "plugin-id";
        //metadata setup
        SCMConfigurations scmConfiguration = new SCMConfigurations();
        scmConfiguration.add(new SCMConfiguration("url"));
        scmConfiguration.add(new SCMConfiguration("username"));
        scmConfiguration.add(new SCMConfiguration("password").with(SECURE, true));
        scmConfiguration.add(new SCMConfiguration("secureKeyNotChanged").with(SECURE, true));
        SCMMetadataStore.getInstance().addMetadataFor(pluginId, scmConfiguration);

        String scmId = "scm-id";
        String name = "scm-name";
        ConfigurationHolder url = new ConfigurationHolder("url", "http://test.com");
        ConfigurationHolder username = new ConfigurationHolder("username", "user");
        String oldEncryptedValue = "oldEncryptedValue";
        ConfigurationHolder password = new ConfigurationHolder("password", "pass", oldEncryptedValue, true, "1");
        ConfigurationHolder secureKeyNotChanged = new ConfigurationHolder("secureKeyNotChanged", "pass", oldEncryptedValue, true, "0");
        Map attributes = createSCMConfiguration(scmId, name, pluginId, url, username, password, secureKeyNotChanged);

        SCM scm = new SCM();
        scm.setConfigAttributes(attributes);

        assertThat(scm.getId(), is(scmId));
        assertThat(scm.getName(), is(name));
        assertThat(scm.getPluginConfiguration().getId(), is(pluginId));

        assertThat(scm.getConfiguration().get(0).getConfigurationKey().getName(), is(url.name));
        assertThat(scm.getConfiguration().get(0).getConfigurationValue().getValue(), is(url.value));

        assertThat(scm.getConfiguration().get(1).getConfigurationKey().getName(), is(username.name));
        assertThat(scm.getConfiguration().get(1).getConfigurationValue().getValue(), is(username.value));

        assertThat(scm.getConfiguration().get(2).getConfigurationKey().getName(), is(password.name));
        assertThat(scm.getConfiguration().get(2).getEncryptedValue().getValue(), is(new GoCipher().encrypt(password.value)));
        assertThat(scm.getConfiguration().get(2).getConfigurationValue(), is(nullValue()));

        assertThat(scm.getConfiguration().get(3).getConfigurationKey().getName(), is(secureKeyNotChanged.name));
        assertThat(scm.getConfiguration().get(3).getEncryptedValue().getValue(), is(oldEncryptedValue));
        assertThat(scm.getConfiguration().get(3).getConfigurationValue(), is(nullValue()));
    }

    @Test
    public void shouldValidateIfNameIsMissing() {
        SCM scm = new SCM();
        scm.validate(new ValidationContext(new CruiseConfig(), null));

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

        scm.validate(new ValidationContext(null));

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
    public void shouldGenerateIdIfNotAssigned() {
        SCM scm = new SCM();
        scm.ensureIdExists();
        assertThat(scm.getId(), is(notNullValue()));

        scm = new SCM();
        scm.setId("id");
        scm.ensureIdExists();
        assertThat(scm.getId(), is("id"));
    }

    private Map createSCMConfiguration(String scmId, String name, String pluginId, ConfigurationHolder... configurations) {
        Map attributes = new HashMap();
        attributes.put(SCM.SCM_ID, scmId);
        attributes.put(SCM.NAME, name);

        HashMap pluginConfiguration = new HashMap();
        pluginConfiguration.put(PluginConfiguration.ID, pluginId);
        attributes.put(SCM.PLUGIN_CONFIGURATION, pluginConfiguration);

        createConfigurationKeyValueMap(attributes, configurations);

        return attributes;
    }

    private void createConfigurationKeyValueMap(Map attributes, ConfigurationHolder[] configurations) {
        Map configurationMap = new LinkedHashMap();
        for (int i = 0; i < configurations.length; i++) {
            ConfigurationHolder currentConfiguration = configurations[i];

            HashMap config = new HashMap();
            HashMap firstConfigKey = new HashMap();
            firstConfigKey.put(ConfigurationKey.NAME, currentConfiguration.name);
            config.put(ConfigurationProperty.CONFIGURATION_KEY, firstConfigKey);

            HashMap firstConfigValue = new HashMap();
            firstConfigValue.put(ConfigurationValue.VALUE, currentConfiguration.value);
            config.put(ConfigurationProperty.CONFIGURATION_VALUE, firstConfigValue);

            if (currentConfiguration.isChanged()) {
                config.put(ConfigurationProperty.IS_CHANGED, "1");
            }
            if (currentConfiguration.isSecure) {
                HashMap encryptedValue = new HashMap();
                encryptedValue.put(EncryptedConfigurationValue.VALUE, currentConfiguration.encryptedValue);
                config.put(ConfigurationProperty.ENCRYPTED_VALUE, encryptedValue);
            }
            configurationMap.put(String.valueOf(i), config);
        }
        attributes.put(Configuration.CONFIGURATION, configurationMap);
    }
}
