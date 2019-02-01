/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.update.ElasticAgentProfileCreateCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileDeleteCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileUpdateCommand;
import com.thoughtworks.go.domain.ElasticProfileUsage;
import com.thoughtworks.go.plugin.access.PluginNotFoundException;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.helper.PipelineConfigMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ElasticProfileServiceTest {
    private GoConfigService goConfigService;
    private ElasticProfileService elasticProfileService;
    private ElasticAgentExtension elasticAgentExtension;

    @BeforeEach
    void setUp() throws Exception {
        elasticAgentExtension = mock(ElasticAgentExtension.class);
        EntityHashingService hashingService = mock(EntityHashingService.class);
        goConfigService = mock(GoConfigService.class);
        elasticProfileService = new ElasticProfileService(goConfigService, hashingService, elasticAgentExtension);
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
        ElasticProfile elasticProfile = new ElasticProfile("ecs", "cd.go.elatic.ecs");
        elasticConfig.getProfiles().add(elasticProfile);

        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);

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
        ElasticProfile elasticProfile = new ElasticProfile("ecs", "cd.go.elatic.ecs");
        elasticConfig.getProfiles().add(elasticProfile);

        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);

        assertThat(elasticProfileService.findProfile("ecs")).isEqualTo(elasticProfile);
    }

    @Test
    void shouldAddElasticProfileToConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap");

        Username username = new Username("username");
        elasticProfileService.create(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileCreateCommand.class), eq(username));
    }

    @Test
    void shouldPerformPluginValidationsBeforeAddingElasticProfile() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap", create("key", false, "value"));

        Username username = new Username("username");
        elasticProfileService.create(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(elasticAgentExtension).validate(elasticProfile.getPluginId(), elasticProfile.getConfigurationAsMap(true));
    }

    @Test
    void shouldAddPluginNotFoundErrorOnConfigForANonExistentPluginIdWhileCreating() {
        ElasticProfile elasticProfile = new ElasticProfile("some-id", "non-existent-plugin", create("key", false, "value"));

        Username username = new Username("username");
        when(elasticAgentExtension.validate(elasticProfile.getPluginId(), elasticProfile.getConfigurationAsMap(true))).thenThrow(new PluginNotFoundException("some error"));

        elasticProfileService.create(username, elasticProfile, new HttpLocalizedOperationResult());

        assertThat(elasticProfile.errors()).isNotEmpty();
        assertThat(elasticProfile.errors().on("pluginId")).isEqualTo("Plugin with id `non-existent-plugin` is not found.");
    }

    @Test
    void shouldUpdateExistingElasticProfileInConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap");

        Username username = new Username("username");
        elasticProfileService.update(username, "md5", elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileUpdateCommand.class), eq(username));
    }

    @Test
    void shouldPerformPluginValidationsBeforeUpdatingElasticProfile() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap", create("key", false, "value"));

        Username username = new Username("username");
        elasticProfileService.update(username, "md5", elasticProfile, new HttpLocalizedOperationResult());

        verify(elasticAgentExtension).validate(elasticProfile.getPluginId(), elasticProfile.getConfigurationAsMap(true));
    }

    @Test
    void shouldAddPluginNotFoundErrorOnConfigForANonExistentPluginIdWhileUpdating() {
        ElasticProfile elasticProfile = new ElasticProfile("some-id", "non-existent-plugin", create("key", false, "value"));

        Username username = new Username("username");
        when(elasticAgentExtension.validate(elasticProfile.getPluginId(), elasticProfile.getConfigurationAsMap(true))).thenThrow(new PluginNotFoundException("some error"));

        elasticProfileService.update(username, "md5", elasticProfile, new HttpLocalizedOperationResult());

        assertThat(elasticProfile.errors()).isNotEmpty();
        assertThat(elasticProfile.errors().on("pluginId")).isEqualTo("Plugin with id `non-existent-plugin` is not found.");
    }

    @Test
    void shouldDeleteExistingElasticProfileInConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap");

        Username username = new Username("username");
        elasticProfileService.delete(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileDeleteCommand.class), eq(username));
    }

    @Nested
    class GetJobsUsingElasticProfile {
        @BeforeEach
        void setUp() {
            final ElasticConfig elasticConfig = new ElasticConfig();
            elasticConfig.getProfiles().add(new ElasticProfile("docker", "cd.go.docker"));
            elasticConfig.getProfiles().add(new ElasticProfile("ecs", "cd.go.ecs"));
            elasticConfig.getProfiles().add(new ElasticProfile("kubernetes", "cd.go.k8s"));
            when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);
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

            assertThat(recordNotFoundException.getMessage()).isEqualTo("Elastic profile with id 'unknown-profile-id' does not exist.");
        }
    }
}

