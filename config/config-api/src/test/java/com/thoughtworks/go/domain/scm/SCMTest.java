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
package com.thoughtworks.go.domain.scm;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMPreference;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.DataStructureUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.plugin.access.scm.SCMConfiguration.PART_OF_IDENTITY;
import static com.thoughtworks.go.plugin.access.scm.SCMConfiguration.SECURE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SCMTest {
    @BeforeEach
    void setup() {
        SCMMetadataStore.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    void shouldCheckEqualityOfSCM() {
        Configuration configuration = new Configuration();
        SCM scm = SCMMother.create("id", "name", "plugin-id", "version", configuration);
        assertThat(scm).isEqualTo(SCMMother.create("id", "name", "plugin-id", "version", configuration));
    }

    @Test
    void shouldCheckForFieldAssignments() {
        Configuration configuration = new Configuration();
        SCM scm = SCMMother.create("id", "name", "plugin-id", "version", configuration);
        assertThat(scm.getId()).isEqualTo("id");
        assertThat(scm.getName()).isEqualTo("name");
        assertThat(scm.getPluginConfiguration().getId()).isEqualTo("plugin-id");
        assertThat(scm.getPluginConfiguration().getVersion()).isEqualTo("version");
        assertThat(scm.getConfiguration().listOfConfigKeys().isEmpty()).isTrue();
    }

    @Test
    void shouldOnlyDisplayFieldsWhichAreNonSecureAndPartOfIdentityInGetConfigForDisplayWhenPluginExists() {
        SCMConfigurations scmConfiguration = new SCMConfigurations();
        scmConfiguration.add(new SCMConfiguration("key1").with(PART_OF_IDENTITY, true).with(SECURE, false));
        scmConfiguration.add(new SCMConfiguration("key2").with(PART_OF_IDENTITY, false).with(SECURE, false));
        scmConfiguration.add(new SCMConfiguration("key3").with(PART_OF_IDENTITY, true).with(SECURE, true));
        scmConfiguration.add(new SCMConfiguration("key4").with(PART_OF_IDENTITY, false).with(SECURE, true));
        scmConfiguration.add(new SCMConfiguration("key5").with(PART_OF_IDENTITY, true).with(SECURE, false));
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfiguration, null);

        Configuration configuration = new Configuration(create("key1", false, "value1"), create("key2", false, "value2"), create("key3", true, "value3"), create("key4", true, "value4"), create("key5", false, "value5"));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay()).isEqualTo("[key1=value1, key5=value5]");
    }

    @Test
    void shouldConvertKeysToLowercaseInGetConfigForDisplay() {
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", new SCMConfigurations(), null);

        Configuration configuration = new Configuration(create("kEY1", false, "vALue1"), create("KEY_MORE_2", false, "VALUE_2"), create("key_3", false, "value3"));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay()).isEqualTo("[key1=vALue1, key_more_2=VALUE_2, key_3=value3]");
    }

    @Test
    void shouldNotDisplayEmptyValuesInGetConfigForDisplay() {
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", new SCMConfigurations(), null);

        Configuration configuration = new Configuration(create("rk1", false, ""), create("rk2", false, "some-non-empty-value"), create("rk3", false, null));
        SCM scm = SCMMother.create("scm", "scm-name", "plugin-id", "1.0", configuration);

        assertThat(scm.getConfigForDisplay()).isEqualTo("[rk2=some-non-empty-value]");
    }

    @Test
    void shouldDisplayAllNonSecureFieldsInGetConfigForDisplayWhenPluginDoesNotExist() {
        Configuration configuration = new Configuration(create("key1", false, "value1"), create("key2", true, "value2"), create("key3", false, "value3"));
        SCM scm = SCMMother.create("scm", "scm-name", "some-plugin-which-does-not-exist", "1.0", configuration);

        assertThat(scm.getConfigForDisplay()).isEqualTo("WARNING! Plugin missing. [key1=value1, key3=value3]");
    }

    @Test
    void shouldMakeConfigurationSecureBasedOnMetadata() throws Exception {
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
        assertThat(secureProperty.isSecure()).isTrue();
        assertThat(secureProperty.getEncryptedConfigurationValue()).isNotNull();
        assertThat(secureProperty.getEncryptedValue()).isEqualTo(goCipher.encrypt("value1"));

        assertThat(nonSecureProperty.isSecure()).isFalse();
        assertThat(nonSecureProperty.getValue()).isEqualTo("value2");
    }

    @Test
    void shouldNotUpdateSecurePropertyWhenPluginIsMissing() {
        GoCipher goCipher = new GoCipher();

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), null, new EncryptedConfigurationValue("value"), goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value1"), null, goCipher);
        SCM scm = SCMMother.create("scm-id", "scm-name", "plugin-id", "version", new Configuration(secureProperty, nonSecureProperty));

        scm.applyPluginMetadata();

        assertThat(secureProperty.getEncryptedConfigurationValue()).isNotNull();
        assertThat(secureProperty.getConfigurationValue()).isNull();

        assertThat(nonSecureProperty.getConfigurationValue()).isNotNull();
        assertThat(nonSecureProperty.getEncryptedConfigurationValue()).isNull();
    }

    @Test
    void shouldThrowUpOnSetConfigAttributesIfPluginIsNotAvailable() {
        try {
            Map<String, String> attributeMap = DataStructureUtils.m(SCM.SCM_ID, "scm-id", SCM.NAME, "scm-name", SCM.AUTO_UPDATE, "false", "url", "http://localhost");
            SCM scm = new SCM(null, new PluginConfiguration("plugin-id", "1"), new Configuration());
            scm.setConfigAttributes(attributeMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).isEqualTo("metadata unavailable for plugin: plugin-id");
        }
    }

    @Test
    void shouldSetConfigAttributesAsAvailable() {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("url"));
        scmConfigurations.add(new SCMConfiguration("username"));
        scmConfigurations.add(new SCMConfiguration("password").with(SECURE, true));
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfigurations, null);

        Map<String, String> attributeMap = DataStructureUtils.m(SCM.SCM_ID, "scm-id", SCM.NAME, "scm-name", SCM.AUTO_UPDATE, "false", "url", "http://localhost", "username", "user", "password", "pass");
        SCM scm = new SCM(null, new PluginConfiguration("plugin-id", "1"), new Configuration());
        scm.setConfigAttributes(attributeMap);

        assertThat(scm.getId()).isEqualTo("scm-id");
        assertThat(scm.getName()).isEqualTo("scm-name");
        assertThat(scm.isAutoUpdate()).isFalse();
        assertThat(scm.getPluginConfiguration().getId()).isEqualTo("plugin-id");

        assertThat(scm.getConfigAsMap().get("url").get(SCM.VALUE_KEY)).isEqualTo("http://localhost");
        assertThat(scm.getConfigAsMap().get("username").get(SCM.VALUE_KEY)).isEqualTo("user");
        assertThat(scm.getConfigAsMap().get("password").get(SCM.VALUE_KEY)).isEqualTo("pass");

        assertThat(scm.getConfiguration().getProperty("password").getConfigurationValue()).isNull();
        assertThat(scm.getConfiguration().getProperty("password").getEncryptedConfigurationValue()).isNotNull();
    }

    @Test
    void shouldPopulateItselfFromConfigAttributesMap() {
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

        assertThat(scm.getConfigAsMap().get("KEY1").get(SCM.VALUE_KEY)).isEqualTo("value1");
        assertThat(scm.getConfigAsMap().get("Key2").get(SCM.VALUE_KEY)).isEqualTo("value2");
    }

    @Test
    void shouldNotOverwriteValuesIfTheyAreNotAvailableInConfigAttributesMap() {
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

        assertThat(scm.getConfigAsMap().get("KEY1").get(SCM.VALUE_KEY)).isEqualTo("value1");
        assertThat(scm.getConfigAsMap().get("Key2").get(SCM.VALUE_KEY)).isNull();
    }

    @Test
    void shouldIgnoreKeysPresentInConfigAttributesMapButNotPresentInConfigStore() {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("KEY1"));

        SCMPreference scmPreference = mock(SCMPreference.class);
        when(scmPreference.getScmConfigurations()).thenReturn(scmConfigurations);
        SCMMetadataStore.getInstance().setPreferenceFor("plugin-id", scmPreference);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM scm = new SCM("scm-id", new PluginConfiguration("plugin-id", "1"), configuration);

        Map<String, String> attributeMap = DataStructureUtils.m("KEY1", "value1", "Key2", "value2");
        scm.setConfigAttributes(attributeMap);

        assertThat(scm.getConfigAsMap().get("KEY1").get(SCM.VALUE_KEY)).isEqualTo("value1");
        assertThat(scm.getConfigAsMap().containsKey("Key2")).isFalse();
    }

    @Test
    void shouldAddPropertyComingFromAttributesMapIfPresentInConfigStoreEvenIfItISNotPresentInCurrentConfiguration() {
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

        assertThat(scm.getConfigAsMap().get("KEY1").get(SCM.VALUE_KEY)).isEqualTo("value1");
        assertThat(scm.getConfigAsMap().get("Key2").get(SCM.VALUE_KEY)).isEqualTo("value2");
    }

    @Test
    void shouldValidateIfNameIsMissing() {
        SCM scm = new SCM();
        scm.validate(new ConfigSaveValidationContext(new BasicCruiseConfig(), null));

        assertThat(scm.errors().getAllOn(SCM.NAME)).isEqualTo(asList("Please provide name"));
    }

    @Test
    void shouldClearConfigurationsWhichAreEmptyAndNoErrors() {
        SCM scm = new SCM();
        scm.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-one"), new ConfigurationValue()));
        scm.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-two"), new EncryptedConfigurationValue()));
        scm.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-three"), null, new EncryptedConfigurationValue(), null));

        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("name-four"), null, new EncryptedConfigurationValue(), null);
        configurationProperty.addErrorAgainstConfigurationValue("error");
        scm.getConfiguration().add(configurationProperty);

        scm.clearEmptyConfigurations();

        assertThat(scm.getConfiguration().size()).isEqualTo(1);
        assertThat(scm.getConfiguration().get(0).getConfigurationKey().getName()).isEqualTo("name-four");
    }

    @Test
    void shouldValidateName() {
        SCM scm = new SCM();
        scm.setName("some name");

        scm.validate(new ConfigSaveValidationContext(null));

        assertThat(scm.errors().isEmpty()).isFalse();
        assertThat(scm.errors().getAllOn(SCM.NAME).get(0)).isEqualTo("Invalid SCM name 'some name'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    void shouldValidateUniqueKeysInConfiguration() {
        ConfigurationProperty one = create("one", false, "value1");
        ConfigurationProperty duplicate1 = create("ONE", false, "value2");
        ConfigurationProperty duplicate2 = create("ONE", false, "value3");
        ConfigurationProperty two = create("two", false, null);

        SCM scm = new SCM();
        scm.setConfiguration(new Configuration(one, duplicate1, duplicate2, two));
        scm.setName("git");

        scm.validate(null);

        assertThat(one.errors().isEmpty()).isFalse();
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for SCM 'git'")).isTrue();
        assertThat(duplicate1.errors().isEmpty()).isFalse();
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for SCM 'git'")).isTrue();
        assertThat(duplicate2.errors().isEmpty()).isFalse();
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for SCM 'git'")).isTrue();
        assertThat(two.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldGetConfigAsMap() throws Exception {
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

        assertThat(configMap.keySet().size()).isEqualTo(keys.size());
        assertThat(configMap.values().size()).isEqualTo(values.size());
        assertThat(configMap.keySet().containsAll(keys)).isTrue();
        for (int i = 0; i < keys.size(); i++) {
            assertThat(configMap.get(keys.get(i)).get(SCM.VALUE_KEY)).isEqualTo(values.get(i));
        }
    }

    @Test
    void shouldGenerateIdIfNotAssigned() {
        SCM scm = new SCM();
        scm.ensureIdExists();
        assertThat(scm.getId()).isNotNull();

        scm = new SCM();
        scm.setId("id");
        scm.ensureIdExists();
        assertThat(scm.getId()).isEqualTo("id");
    }

    @Test
    void shouldAddConfigurationPropertiesForAnyPlugin() {
        List<ConfigurationProperty> configurationProperties = Arrays.asList(ConfigurationPropertyMother.create("key", "value", "encValue"));
        Configuration configuration = new Configuration();
        SCM scm = SCMMother.create("id", "name", "does_not_exist", "1.1", configuration);

        assertThat(configuration.size()).isEqualTo(0);

        scm.addConfigurations(configurationProperties);

        assertThat(configuration.size()).isEqualTo(1);
    }

    @Test
    void shouldGetSCMTypeCorrectly() {
        SCM scm = SCMMother.create("scm-id");
        assertThat(scm.getSCMType()).isEqualTo("pluggable_material_plugin");

        scm.setPluginConfiguration(new PluginConfiguration("plugin-id-2", "1"));
        assertThat(scm.getSCMType()).isEqualTo("pluggable_material_plugin_id_2");
    }

    @Nested
    class HasSecretParams {
        @Test
        void shouldBeTrueIfScmConfigHasSecretParam() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));

            assertThat(scm.hasSecretParams()).isTrue();
        }

        @Test
        void shouldBeFalseIfScmCOnfigDoesNotHaveSecretParams() {
            SCM scm = new SCM("scm-id", "scm-name");

            assertThat(scm.hasSecretParams()).isFalse();
        }
    }

    @Nested
    class GetSecretParams {
        @Test
        void shouldReturnAListOfSecretParams() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));

            assertThat(scm.getSecretParams().size()).isEqualTo(2);
            assertThat(scm.getSecretParams().get(0)).isEqualTo(new SecretParam("secret_config_id", "lookup_username"));
            assertThat(scm.getSecretParams().get(1)).isEqualTo(new SecretParam("secret_config_id", "lookup_password"));
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsInScmConfig() {
            SCM scm = new SCM("scm-id", "scm-name");

            assertThat(scm.getSecretParams()).isEmpty();
        }
    }
}
