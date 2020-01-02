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
package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ElasticProfileTest {

    private ElasticAgentMetadataStore store = ElasticAgentMetadataStore.instance();
    private ValidationContext validationContext;
    private String clusterProfileId;
    private String pluginId;
    private BasicCruiseConfig config;

    @Before
    public void setUp() throws Exception {
        pluginId = "cd.go.elastic-agent.docker-swarm";
        clusterProfileId = "prod-cluster";
        config = new BasicCruiseConfig();
        config.getElasticConfig().setClusterProfiles(new ClusterProfiles(new ClusterProfile(clusterProfileId, pluginId)));
        validationContext = new ConfigSaveValidationContext(config);
    }

    @After
    public void tearDown() throws Exception {
        store.clear();
    }

    @Test
    public void shouldNotAllowNullId() throws Exception {
        ElasticProfile profile = new ElasticProfile();

        profile.validate(validationContext);
        assertThat(profile.errors().on(ElasticProfile.ID), is("Elastic agent profile cannot have a blank id."));
    }

    @Test
    public void shouldVerifyExistenceOfReferencedClusterProfile() throws Exception {
        ElasticProfile profile = new ElasticProfile("foo", "non-existing-cluster");

        profile.validate(validationContext);
        assertThat(profile.errors().on(ElasticProfile.CLUSTER_PROFILE_ID), is("No Cluster Profile exists with the specified cluster_profile_id 'non-existing-cluster'."));
    }

    @Test
    public void shouldValidateElasticPluginIdPattern() throws Exception {
        ElasticProfile profile = new ElasticProfile("!123", "prod-cluster");
        profile.validate(null);
        assertThat(profile.errors().size(), is(1));
        assertThat(profile.errors().on(ElasticProfile.ID), is("Invalid id '!123'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateConfigPropertyNameUniqueness() throws Exception {
        ConfigurationProperty prop1 = ConfigurationPropertyMother.create("USERNAME");
        ConfigurationProperty prop2 = ConfigurationPropertyMother.create("USERNAME");
        ElasticProfile profile = new ElasticProfile("docker.unit-test", "prod-cluster", prop1, prop2);

        profile.validate(validationContext);

        assertThat(profile.errors().size(), is(0));

        assertThat(prop1.errors().size(), is(1));
        assertThat(prop2.errors().size(), is(1));

        assertThat(prop1.errors().on(ConfigurationProperty.CONFIGURATION_KEY), is("Duplicate key 'USERNAME' found for Elastic agent profile 'docker.unit-test'"));
        assertThat(prop2.errors().on(ConfigurationProperty.CONFIGURATION_KEY), is("Duplicate key 'USERNAME' found for Elastic agent profile 'docker.unit-test'"));
    }

    @Test
    public void shouldNotAddAnyErrorsForAValidClusterProfileReference() {
        ElasticProfile profile = new ElasticProfile("docker.unit-test", "prod-cluster");
        BasicCruiseConfig config = new BasicCruiseConfig();
        config.getElasticConfig().setClusterProfiles(new ClusterProfiles(new ClusterProfile("prod-cluster", "cd.go.elastic-agent.docker")));
        profile.validate(new ConfigSaveValidationContext(config));

        assertThat(profile.errors().size(), is(0));
    }

    @Test
    public void addConfigurations_shouldAddConfigurationsWithValue() throws Exception {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("some_name"));

        ElasticProfile profile = new ElasticProfile("id", "prod-cluster");
        profile.addConfigurations(Arrays.asList(property));

        assertThat(profile.size(), is(1));
        assertThat(profile, contains(new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("some_name"))));
    }

    @Test
    public void addConfigurations_shouldAddConfigurationsWithEncryptedValue() throws Exception {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new EncryptedConfigurationValue("some_name"));

        ElasticProfile profile = new ElasticProfile("id", "prod-cluster");
        profile.addConfigurations(Arrays.asList(property));

        assertThat(profile.size(), is(1));
        assertThat(profile, contains(new ConfigurationProperty(new ConfigurationKey("username"), new EncryptedConfigurationValue("some_name"))));
    }

    @Test
    public void addConfiguration_shouldIgnoreEncryptionInAbsenceOfCorrespondingConfigurationInStore() throws Exception {
        ElasticAgentPluginInfo pluginInfo = new ElasticAgentPluginInfo(pluginDescriptor("plugin_id"), new PluggableInstanceSettings(new ArrayList<>()), null, null, null, null);

        store.setPluginInfo(pluginInfo);
        ElasticProfile profile = new ElasticProfile("id", "prod-cluster");
        profile.addConfigurations(Arrays.asList(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass"))));

        assertThat(profile.size(), is(1));
        assertFalse(profile.first().isSecure());
        assertThat(profile, contains(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass"))));
    }

    @Test
    public void shouldEncryptSecureConfigurations() {
        PluggableInstanceSettings profileSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("password", new Metadata(true, true))));
        ElasticAgentPluginInfo pluginInfo = new ElasticAgentPluginInfo(pluginDescriptor(pluginId), profileSettings, profileSettings, null, null, null);

        store.setPluginInfo(pluginInfo);
        ElasticProfile profile = new ElasticProfile("id", clusterProfileId, new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

        profile.encryptSecureProperties(config);

        assertThat(profile.size(), is(1));
        assertTrue(profile.first().isSecure());
    }

    @Test
    public void postConstruct_shouldIgnoreEncryptionIfPluginInfoIsNotDefined() {
        ElasticProfile profile = new ElasticProfile("id", "prod-cluster", new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

//        profile.encryptSecureConfigurations();

        assertThat(profile.size(), is(1));
        assertFalse(profile.first().isSecure());
    }

    private PluginDescriptor pluginDescriptor(String pluginId) {
        return new PluginDescriptor() {
            @Override
            public String id() {
                return pluginId;
            }

            @Override
            public String version() {
                return null;
            }

            @Override
            public About about() {
                return null;
            }
        };
    }
}
