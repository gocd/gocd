/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Deny;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.config.rules.SupportedEntity.ENVIRONMENT;
import static com.thoughtworks.go.helper.GoConfigMother.configWithSecretConfig;
import static com.thoughtworks.go.helper.GoConfigMother.defaultCruiseConfig;
import static com.thoughtworks.go.helper.MaterialsMother.gitMaterial;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class RulesServiceTest {
    @Mock
    private GoConfigService goConfigService;
    private RulesService rulesService;

    @BeforeEach
    void setUp() {
        initMocks(this);

        rulesService = new RulesService(goConfigService);
    }

    @Nested
    class ForMaterials {
        @Test
        void shouldErrorOutIfMaterialDoesNotHavePermissionToReferASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("some_group", up42);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(asList(new CaseInsensitiveString("up42")));
            when(goConfigService.findGroupByPipeline(any())).thenReturn(defaultGroup);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up42"))).thenReturn(up42);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(gitMaterial))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pipeline 'up42' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void forAMaterialReferredInMultiplePipelineGroups_shouldNotErrorOutEvenIfOneOfThePipelineHavePermissionToReferASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfig up43 = PipelineConfigMother.pipelineConfig("up43", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            PipelineConfigs someGroup = PipelineConfigMother.createGroup("some_group", up43);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup, someGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(asList(new CaseInsensitiveString("up42"), new CaseInsensitiveString("up43")));
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("up42"))).thenReturn(defaultGroup);
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("up43"))).thenReturn(someGroup);
            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up42"))).thenReturn(up42);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up43"))).thenReturn(up43);

            assertThat(rulesService.validateSecretConfigReferences(gitMaterial)).isEqualTo(true);
        }

        @Test
        void forAMaterialReferredInMultiplePipelineGroups_shouldErrorOutIfAllThePipelineDontHavePermissionToReferASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            Rules rules = new Rules(new Deny("refer", "pipeline_group", "*"));

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);
            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfig up43 = PipelineConfigMother.pipelineConfig("up43", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            PipelineConfigs someGroup = PipelineConfigMother.createGroup("some_group", up43);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup, someGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(asList(new CaseInsensitiveString("up42"), new CaseInsensitiveString("up43")));
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("up42"))).thenReturn(defaultGroup);
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("up43"))).thenReturn(someGroup);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up42"))).thenReturn(up42);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up43"))).thenReturn(up43);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(gitMaterial))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessageContaining("Pipeline 'up42' is referring to none-existent secret config 'secret_config_id'.")
                    .hasMessageContaining("Pipeline 'up43' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldValidateIfMaterialHasPermissionToReferASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(asList(new CaseInsensitiveString("up42")));
            when(goConfigService.findGroupByPipeline(any())).thenReturn(defaultGroup);
            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up42"))).thenReturn(up42);

            assertThat(rulesService.validateSecretConfigReferences(gitMaterial)).isEqualTo(true);

        }

        @Test
        void shouldErrorOutWhenMaterialIsReferringToNoneExistingSecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = defaultCruiseConfig();
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(Collections.singletonList(new CaseInsensitiveString("up42")));
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("up42"))).thenReturn(defaultGroup);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up42"))).thenReturn(up42);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(gitMaterial))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pipeline 'up42' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldBeValidIfMaterialPasswordIsNotDefinedUsingSecretParams() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("badger");

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = defaultCruiseConfig();
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(asList(new CaseInsensitiveString("up42")));
            when(goConfigService.findGroupByPipeline(any())).thenReturn(defaultGroup);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up42"))).thenReturn(up42);

            assertThat(rulesService.validateSecretConfigReferences(gitMaterial)).isEqualTo(true);
        }
    }

    @Nested
    class ForBuildAssignment {
        @Test
        void shouldBeValidIfJobInBuildAssignmentDoesNotHaveAnythingDefinedUsingSecretParams() {
            JobIdentifier identifier = new JobIdentifier("Up42", 1, "1", "test", "1", "unit_test", 123L);
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Test", "some env", false);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext, identifier);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs());
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = defaultCruiseConfig();
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            rulesService.validateSecretConfigReferences(buildAssigment);

            verifyZeroInteractions(goConfigService);
        }

        @Test
        void shouldValidateIfAJobInBuildAssignmentHasPermissionToReferASecretConfig() {
            Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            JobIdentifier identifier = new JobIdentifier("up42", 1, "1", "test", "1", "unit_test", 123L);
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Token", "{{SECRET:[secret_config_id][password]}}", false);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext, identifier);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs());
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString(identifier.getPipelineName()))).thenReturn(defaultGroup);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(buildAssigment)).isNull();

            verify(goConfigService, atLeastOnce()).findGroupByPipeline(new CaseInsensitiveString("up42"));
        }

        @Test
        void shouldErrorOutIfAJobInBuildAssignmentDoesNotHavePermissionToReferASecretConfig() {
            Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            JobIdentifier identifier = new JobIdentifier("up42", 1, "1", "test", "1", "unit_test", 123L);
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Token", "{{SECRET:[secret_config_id][password]}}", false);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext, identifier);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs());
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("some_group", up42);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString(identifier.getPipelineName()))).thenReturn(defaultGroup);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(buildAssigment))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Job: 'unit_test' in Pipeline: 'up42' and Pipeline Group: 'some_group' does not have " +
                            "permission to refer to secrets using secret config 'secret_config_id'");
        }

        @Test
        void shouldErrorOutWhenJobPlanIsReferringToNoneExistingSecretConfig() {
            JobIdentifier jobIdentifier = new JobIdentifier("up42", 1, null, null, null, "job1");
            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs());
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("some_group", up42);
            CruiseConfig cruiseConfig = defaultCruiseConfig();
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Token", "{{SECRET:[secret_config_id][password]}}", false);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext, jobIdentifier);

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString(jobIdentifier.getPipelineName()))).thenReturn(defaultGroup);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(buildAssigment))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Job: 'job1' in Pipeline: 'up42' and Pipeline Group: 'some_group' is referring to none-existent secret config 'secret_config_id'.");
        }

        private BuildAssignment createAssignment(EnvironmentVariableContext environmentVariableContext, JobIdentifier identifier) {
            ScmMaterial gitMaterial = gitMaterial("https://example.org");
            MaterialRevision gitRevision = new MaterialRevision(gitMaterial, new Modification());
            BuildCause buildCause = BuildCause.createManualForced(new MaterialRevisions(gitRevision), Username.ANONYMOUS);

            JobPlan plan = defaultJobPlan(new EnvironmentVariables(), new EnvironmentVariables(), identifier);
            List<Builder> builders = new ArrayList<>();
            builders.add(new CommandBuilder("ls", "", null, new RunIfConfigs(), new NullBuilder(), ""));
            return BuildAssignment.create(plan, buildCause, builders, null, environmentVariableContext, new ArtifactStores());
        }
    }

    @Nested
    class ForEnvironmentConfig {
        @Test
        void shouldBeValidIfEnvironmentConfigDoesNotHaveEnvironmentVariableDefinedUsingSecretParams() {
            BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("dev"));

            boolean result = rulesService.validateSecretConfigReferences(environmentConfig);

            assertThat(result).isTrue();
        }

        @Test
        void shouldBeValidIfEnvironmentConfigCanReferToASecretConfig() {
            BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("dev"));
            environmentConfig.addEnvironmentVariable("Token", "{{SECRET:[secret_config_id][token]}}");
            Allow allow = new Allow("refer", ENVIRONMENT.getType(), "dev");
            CruiseConfig cruiseConfig = configWithSecretConfig(new SecretConfig("secret_config_id", "cd.go.secret.file", new Rules(allow)));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);

            boolean result = rulesService.validateSecretConfigReferences(environmentConfig);

            assertThat(result).isTrue();
        }

        @Test
        void shouldErrorOutWhenEnvironmentConfigCannotReferToASecretConfig() {
            BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("dev"));
            environmentConfig.addEnvironmentVariable("Token", "{{SECRET:[secret_config_id][token]}}");
            CruiseConfig cruiseConfig = configWithSecretConfig(new SecretConfig("secret_config_id", "cd.go.secret.file"));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(environmentConfig))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Environment 'dev' does not have permission to refer to secrets using secret config 'secret_config_id'");
        }

        @Test
        void shouldErrorOutWhenEnvironmentConfigIsReferringToNoneExistingSecretConfig() {
            BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("dev"));
            environmentConfig.addEnvironmentVariable("Token", "{{SECRET:[secret_config_id][token]}}");

            when(goConfigService.cruiseConfig()).thenReturn(defaultCruiseConfig());

            assertThatCode(() -> rulesService.validateSecretConfigReferences(environmentConfig))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Environment 'dev' is referring to none-existent secret config 'secret_config_id'.");
        }
    }

    private JobPlan defaultJobPlan(EnvironmentVariables variables, EnvironmentVariables triggerVariables, JobIdentifier jobIdentifier) {
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArrayList<>(), -1, jobIdentifier, null,
                variables, triggerVariables, null, null);
    }
}
