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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.update.ElasticAgentProfileCreateCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileDeleteCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileUpdateCommand;
import com.thoughtworks.go.domain.ElasticProfileUsage;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.validators.elastic.ElasticAgentProfileConfigurationValidator;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.helper.PipelineConfigMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ElasticProfileServiceTest {
    private GoConfigService goConfigService;
    private ElasticProfileService elasticProfileService;
    private ElasticAgentExtension elasticAgentExtension;
    private String clusterProfileId;
    private String pluginId;
    private ElasticAgentProfileConfigurationValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        pluginId = "cd.go.elastic.ecs";
        clusterProfileId = "prod-cluster";
        elasticAgentExtension = mock(ElasticAgentExtension.class);
        EntityHashingService hashingService = mock(EntityHashingService.class);
        goConfigService = mock(GoConfigService.class);
        elasticProfileService = new ElasticProfileService(goConfigService, hashingService, elasticAgentExtension);
        validator = mock(ElasticAgentProfileConfigurationValidator.class);
        elasticProfileService.setProfileConfigurationValidator(validator);
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.getClusterProfiles().add(new ClusterProfile(clusterProfileId, pluginId));
        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setElasticConfig(elasticConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
    }

    @Test
    void shouldReturnAnEmptyMapIfNoElasticProfiles() {
        ElasticConfig elasticConfig = new ElasticConfig();

        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);

        assertThat(elasticProfileService.listAll()).isEmpty();
    }

    @Test
    void shouldReturnAMapOfElasticProfiles() {
        ElasticConfig elasticConfig = new ElasticConfig();
        ElasticProfile elasticProfile = new ElasticProfile("ecs", clusterProfileId);
        elasticConfig.getProfiles().add(elasticProfile);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setElasticConfig(elasticConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);

        HashMap<String, ElasticProfile> expectedMap = new HashMap<>();
        expectedMap.put("ecs", elasticProfile);
        Map<String, ElasticProfile> elasticProfiles = elasticProfileService.listAll();
        assertThat(elasticProfiles).hasSize(1);
        assertThat(elasticProfiles).isEqualTo(expectedMap);
    }

    @Test
    void shouldReturnNullWhenProfileWithGivenIdDoesNotExist() {
        when(goConfigService.getElasticConfig()).thenReturn(new ElasticConfig());

        assertThat(elasticProfileService.findProfile("non-existent-id")).isNull();
    }

    @Test
    void shouldReturnElasticProfileWithGivenIdWhenPresent() {
        ElasticConfig elasticConfig = new ElasticConfig();
        ElasticProfile elasticProfile = new ElasticProfile("ecs", "prod-cluster");
        elasticConfig.getProfiles().add(elasticProfile);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setElasticConfig(elasticConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);

        assertThat(elasticProfileService.findProfile("ecs")).isEqualTo(elasticProfile);
    }

    @Test
    void shouldAddElasticProfileToConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "prod-cluster");

        Username username = new Username("username");
        elasticProfileService.create(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileCreateCommand.class), eq(username));
    }

    @Test
    void shouldPerformPluginValidationsBeforeAddingElasticProfile() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", clusterProfileId, create("key", false, "value"));

        Username username = new Username("username");
        elasticProfileService.create(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(validator).validate(elasticProfile, pluginId);
    }

    @Test
    void shouldUpdateExistingElasticProfileInConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "prod-cluster");

        Username username = new Username("username");
        elasticProfileService.update(username, "md5", elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileUpdateCommand.class), eq(username));
    }

    @Test
    void shouldPerformPluginValidationsBeforeUpdatingElasticProfile() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", clusterProfileId, create("key", false, "value"));

        Username username = new Username("username");
        elasticProfileService.update(username, "md5", elasticProfile, new HttpLocalizedOperationResult());

        validator.validate(elasticProfile, pluginId);
    }

    @Test
    void shouldDeleteExistingElasticProfileInConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "prod-cluster");

        Username username = new Username("username");
        elasticProfileService.delete(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileDeleteCommand.class), eq(username));
    }

    @Test
    void shouldNotPerformPluginValidationsWhileDeletingElasticProfile() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "prod-cluster");

        Username username = new Username("username");
        elasticProfileService.delete(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(validator, never()).validate(eq(elasticProfile), anyString());
    }

    @Nested
    class GetJobsUsingElasticProfile {
        @BeforeEach
        void setUp() {
            final ElasticConfig elasticConfig = new ElasticConfig();
            elasticConfig.getProfiles().add(new ElasticProfile("docker", "prod-cluster"));
            elasticConfig.getProfiles().add(new ElasticProfile("ecs", "prod-cluster"));
            elasticConfig.getProfiles().add(new ElasticProfile("kubernetes", "prod-cluster"));
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.setElasticConfig(elasticConfig);
            when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        }

        @Test
        void shouldReturnJobsUsingElasticProfile() {
            final List<PipelineConfig> allPipelineConfigs = Arrays.asList(
                    templateBasedPipelineWithElasticJobs("docker-template", "docker", "P1", "S1", "Job1", "Job2"),
                    pipelineWithElasticJobs("ecs", "P3", "S1", "Job1"),
                    createPipelineConfig("P4", "S1", "Job1", "Job2")
            );

            when(goConfigService.getAllPipelineConfigs()).thenReturn(allPipelineConfigs);

            assertThat(elasticProfileService.getUsageInformation("docker"))
                    .hasSize(2)
                    .contains(
                            new ElasticProfileUsage("P1", "S1", "Job1", "docker-template", "gocd"),
                            new ElasticProfileUsage("P1", "S1", "Job2", "docker-template", "gocd")
                    );

            assertThat(elasticProfileService.getUsageInformation("ecs"))
                    .hasSize(1)
                    .contains(
                            new ElasticProfileUsage("P3", "S1", "Job1", null, "config_repo")
                    );
        }

        @Test
        void shouldReturnEmptyWhenNoneOfTheJobMatchesProfileId() {
            final List<PipelineConfig> allPipelineConfigs = Arrays.asList(
                    templateBasedPipelineWithElasticJobs("docker-template", "docker", "P1", "S1", "Job1", "Job2"),
                    pipelineWithElasticJobs("ecs", "P3", "S1", "Job1"),
                    createPipelineConfig("P4", "S1", "Job1", "Job2")
            );

            when(goConfigService.getAllPipelineConfigs()).thenReturn(allPipelineConfigs);

            final Collection<ElasticProfileUsage> jobsUsingElasticProfile = elasticProfileService.getUsageInformation("kubernetes");

            assertThat(jobsUsingElasticProfile).isEmpty();
        }

        @Test
        void shouldErrorOutWhenElasticProfileWithIdDoesNotExist() {
            final RecordNotFoundException recordNotFoundException = assertThrows(RecordNotFoundException.class, () -> elasticProfileService.getUsageInformation("unknown-profile-id"));

            assertThat(recordNotFoundException.getMessage()).isEqualTo(EntityType.ElasticProfile.notFoundMessage("unknown-profile-id"));
        }
    }
}

