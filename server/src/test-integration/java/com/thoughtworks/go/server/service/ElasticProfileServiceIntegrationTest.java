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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.validators.elastic.ElasticAgentProfileConfigurationValidator;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ElasticProfileServiceIntegrationTest {
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private ElasticProfileService elasticProfileService;
    @Autowired
    private EntityHashingService entityHashingService;

    private GoConfigFileHelper configHelper;
    private String pluginId;
    private ClusterProfile clusterProfile;
    private Username username;
    private String clusterProfileId;
    private ElasticProfile elasticProfile;
    private ElasticProfile newElasticProfile;
    private String elasticProfileId;

    @Mock
    private ElasticAgentProfileConfigurationValidator validator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        pluginId = "aws";
        clusterProfileId = "prod-cluster";
        elasticProfileId = "id";
        username = new Username("Bob");
        configHelper = new GoConfigFileHelper();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
        clusterProfile = new ClusterProfile(clusterProfileId, pluginId);
        elasticProfile = new ElasticProfile(elasticProfileId, clusterProfileId);
        newElasticProfile = new ElasticProfile(elasticProfileId, clusterProfileId, new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1")));

        goConfigService.updateConfig(cruiseConfig -> {
            BasicCruiseConfig basicCruiseConfig = new BasicCruiseConfig();
            basicCruiseConfig.initializeServer();
            ClusterProfiles clusterProfiles = new ClusterProfiles();
            clusterProfiles.add(clusterProfile);
            basicCruiseConfig.getElasticConfig().setClusterProfiles(clusterProfiles);
            return basicCruiseConfig;
        });
        elasticProfileService.setProfileConfigurationValidator(validator);
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldCreateElasticAgentProfile() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);
        ElasticProfile created = elasticProfileService.getPluginProfiles().get(0);
        assertThat(created.getId()).isEqualTo(elasticProfileId);
        assertThat(created.getConfigWithErrorsAsMap()).isEqualTo(new HashMap<>());
    }

    @Test
    public void shouldValidateElasticAgentProfileWithExtensionAtTheTimeOfCreation() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);

        verify(validator, times(1)).validate(elasticProfile, pluginId);
    }

    @Test
    public void shouldFailToCreateElasticAgentProfileWhenReferencedClusterProfileDoesNotExists() {
        clusterProfileId = "non-existing-cluster";
        elasticProfile = new ElasticProfile(elasticProfileId, clusterProfileId);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        assertThat(result.message()).isEqualTo("Validations failed for agentProfile 'id'. Error(s): [No Cluster Profile exists with the specified cluster_profile_id 'non-existing-cluster'.]. Please correct and resubmit.");
    }

    @Test
    public void shouldFailToCreateElasticAgentProfileWhenAnElasticAgentProfileWithSameNameAlreadyExists() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);

        elasticProfileService.create(username, elasticProfile, result);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("Validations failed for agentProfile 'id'. Error(s): [Elastic agent profile id 'id' is not unique, Elastic agent profile id 'id' is not unique]. Please correct and resubmit.");
    }

    @Test
    public void shouldUpdateElasticAgentProfile() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);
        ElasticProfile existing = elasticProfileService.getPluginProfiles().get(0);

        assertThat(existing.getConfigWithErrorsAsMap()).isEmpty();
        elasticProfileService.update(username, entityHashingService.hashForEntity(this.elasticProfile), newElasticProfile, result);
        ElasticProfile updated = elasticProfileService.getPluginProfiles().get(0);
        assertThat(updated.getId()).isEqualTo(elasticProfileId);
        assertThat(updated.get(0)).isEqualTo(new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1")));
    }

    @Test
    public void shouldFailToUpdateElasticAgentProfileWhenMd5DoesNotMatch() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);

        assertThat(elasticProfileService.getPluginProfiles().get(0).getConfigWithErrorsAsMap()).isEmpty();
        elasticProfileService.update(username, "md5", newElasticProfile, result);
        assertThat(elasticProfileService.getPluginProfiles().get(0).getConfigWithErrorsAsMap()).isEmpty();

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("Someone has modified the configuration for elastic agent profile with id 'id'. Please update your copy of the config with the changes.");
    }

    @Test
    public void shouldDeleteElasticAgentProfile() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);

        elasticProfileService.delete(username, elasticProfile, result);
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void shouldFailToDeleteNonExistingElasticAgentProfile() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);

        elasticProfile = new ElasticProfile("non-existing-profile", clusterProfileId);

        elasticProfileService.delete(username, elasticProfile, result);
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("agentProfile 'non-existing-profile' not found.");
    }

    @Test
    public void shouldFindElasticAgentProfile() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        assertThat(elasticProfileService.getPluginProfiles()).hasSize(0);
        elasticProfileService.create(username, elasticProfile, result);
        assertThat(elasticProfileService.getPluginProfiles()).hasSize(1);

        ElasticProfile found = elasticProfileService.findProfile(elasticProfileId);
        assertThat(found.getId()).isEqualTo(elasticProfileId);
        assertThat(found.getConfigWithErrorsAsMap()).isEqualTo(new HashMap<>());
    }
}
