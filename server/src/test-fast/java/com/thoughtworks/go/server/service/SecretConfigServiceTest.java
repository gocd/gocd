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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.SecretConfigUsage;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.thoughtworks.go.helper.PipelineConfigMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecretConfigServiceTest {
    private GoConfigService goConfigService;
    private SecretConfigService secretConfigService;
    private SecretsExtension secretsExtension;
    private CruiseConfig cruiseConfig;

    @BeforeEach
    void setUp() throws Exception {
        secretsExtension = mock(SecretsExtension.class);
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = mock(CruiseConfig.class);
        secretConfigService = new SecretConfigService(goConfigService, mock(EntityHashingService.class), secretsExtension);
    }

    @Nested
    class SecretConfigUsages {
        private SecretParam secretParam;
        private List<PipelineConfig> allPipelineConfigs;

        @BeforeEach
        void setUp() {
            final SecretConfigs secretConfigs = new SecretConfigs(
                    new SecretConfig("ForDeploy", "file-based"),
                    new SecretConfig("ForTestEnv", "vault-based"),
                    new SecretConfig("kubernetes", "cloud-based")
            );
            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.getSecretConfigs()).thenReturn(secretConfigs);


            secretParam = new SecretParam("ForDeploy", "username");
            allPipelineConfigs = Arrays.asList(
                    templateBasedPipelineWithSecretParams("docker-template", secretParam, "P1", "S1", "Job1", "Job2"),
                    pipelineWithSecretParams(new SecretParam("ForTestEnv", "password"), "P3", "S1", "Job1"),
                    createPipelineConfig("P4", "S1", "Job1", "Job2")
            );
        }

        @Test
        void shouldReturnUsagesForJobsUsingSecretConfig() {
            when(goConfigService.getAllPipelineConfigs()).thenReturn(allPipelineConfigs);

            assertThat(secretConfigService.getUsageInformation("ForDeploy"))
                    .hasSize(2)
                    .contains(
                            new SecretConfigUsage("gocd", "P1", "S1", "Job1", "docker-template"),
                            new SecretConfigUsage("gocd", "P1", "S1", "Job2", "docker-template")
                    );

            assertThat(secretConfigService.getUsageInformation("ForTestEnv"))
                    .hasSize(1)
                    .contains(
                            new SecretConfigUsage("config_repo", "P3", "S1", "Job1")
                    );
        }

        @Test
        void shouldReturnEmptyUsagesWhenNoneOfTheJobMatchesConfigId() {
            SecretParam secretParam = new SecretParam("ForDeploy", "username");
            final List<PipelineConfig> allPipelineConfigs = Arrays.asList(
                    templateBasedPipelineWithSecretParams("docker-template", secretParam, "P1", "S1", "Job1", "Job2"),
                    pipelineWithSecretParams(secretParam, "ecs", "P3", "S1", "Job1"),
                    createPipelineConfig("P4", "S1", "Job1", "Job2")
            );

            when(goConfigService.getAllPipelineConfigs()).thenReturn(allPipelineConfigs);

            final Collection<SecretConfigUsage> jobsUsingSecretParams = secretConfigService.getUsageInformation("kubernetes");

            assertThat(jobsUsingSecretParams).isEmpty();
        }

        @Test
        void shouldErrorOutWhenSecretConfigWithIdDoesNotExist() {
            final RecordNotFoundException recordNotFoundException = assertThrows(RecordNotFoundException.class, () -> secretConfigService.getUsageInformation("unknown-config-id"));

            assertThat(recordNotFoundException.getMessage()).isEqualTo(EntityType.SecretConfig.notFoundMessage("unknown-config-id"));
        }

        @Test
        void shouldReturnUsagesForMaterialUsingSecretConfig() {
            PipelineConfig pipelineConfig = createPipelineConfig("P4", "S1", "Job1", "Job2");
            GitMaterialConfig materialConfig = MaterialConfigsMother.gitMaterialConfig("http://example.com");
            materialConfig.setPassword("{{SECRET:[ForDeploy][placeholder]}}");
            pipelineConfig.addMaterialConfig(materialConfig);

            when(goConfigService.getAllPipelineConfigs()).thenReturn(Arrays.asList(pipelineConfig));

            final Collection<SecretConfigUsage> pipelinesUsingSecretParams = secretConfigService.getUsageInformation("ForDeploy");

            assertThat(pipelinesUsingSecretParams)
                    .hasSize(1)
                    .contains(
                            new SecretConfigUsage("config_repo", "P4")
                    );
        }
    }
}

