/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
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
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.rules.SupportedEntity.ENVIRONMENT;
import static com.thoughtworks.go.helper.GoConfigMother.configWithSecretConfig;
import static com.thoughtworks.go.helper.GoConfigMother.defaultCruiseConfig;
import static com.thoughtworks.go.helper.MaterialsMother.gitMaterial;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RulesServiceTest {
    @Mock
    private GoConfigService goConfigService;
    private RulesService rulesService;

    @BeforeEach
    void setUp() {
        rulesService = new RulesService(goConfigService);
    }

    @Nested
    class ForScmMaterials {
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

            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(singletonList(new CaseInsensitiveString("up42")));
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

            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(singletonList(new CaseInsensitiveString("up42")));
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

            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(singletonList(new CaseInsensitiveString("up42")));
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

            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(singletonList(new CaseInsensitiveString("up42")));
            when(goConfigService.findGroupByPipeline(any())).thenReturn(defaultGroup);
            when(goConfigService.findPipelineByName(new CaseInsensitiveString("up42"))).thenReturn(up42);

            assertThat(rulesService.validateSecretConfigReferences(gitMaterial)).isEqualTo(true);
        }
    }

    @Nested
    class ForPluggableScmMaterials {
        @Test
        void shouldErrorOutIfScmDoesNotHavePermissionToReferASecretConfig() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            Rules rules = new Rules(new Allow("refer", "environment", "scm-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(material.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("some_group", up42);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pluggable SCM 'scm-name' does not have permission to refer to secrets using secret config 'secret_config_id'.");
        }

        @Test
        void shouldValidateIfScmHasPermissionToReferASecretConfig() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            Rules rules = new Rules(new Allow("refer", "pluggable_scm", "scm-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(material.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .doesNotThrowAnyException();

            verify(goConfigService, times(1)).getSecretConfigById(anyString());
        }

        @Test
        void shouldErrorOutWhenScmIsReferringToNoneExistingSecretConfig() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(material.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = defaultCruiseConfig();
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pluggable SCM 'scm-name' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldBeValidIfScmConfigIsNotDefinedUsingSecretParams() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(material.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = defaultCruiseConfig();
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldAddErrorForASecretConfigIdOnlyOnce() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(material.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = defaultCruiseConfig();
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pluggable SCM 'scm-name' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldConcatenateMultipleErrorsWithNewLineChar() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[unknown_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            Rules rules = new Rules(new Allow("refer", "pluggable_scm", "scm-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(material.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pluggable SCM 'scm-name' is referring to none-existent secret config 'secret_config_id'.\nPluggable SCM 'scm-name' is referring to none-existent secret config 'unknown_id'.");
        }
    }

    @Nested
    class ForPluggableScmConfig {
        @Test
        void shouldErrorOutIfScmDoesNotHavePermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));

            Rules rules = new Rules(new Allow("refer", "environment", "scm-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(scm))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pluggable SCM 'scm-name' does not have permission to refer to secrets using secret config 'secret_config_id'.");
        }

        @Test
        void shouldValidateIfScmHasPermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));

            Rules rules = new Rules(new Allow("refer", "pluggable_scm", "scm-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(scm))
                    .doesNotThrowAnyException();

            verify(goConfigService, times(1)).getSecretConfigById(anyString());
        }

        @Test
        void shouldErrorOutWhenScmIsReferringToNoneExistingSecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(scm))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pluggable SCM 'scm-name' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldBeValidIfScmConfigIsNotDefinedUsingSecretParams() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(scm))
                    .doesNotThrowAnyException();

            verify(goConfigService, never()).getSecretConfigById(anyString());
        }

        @Test
        void shouldAddErrorForASecretConfigIdOnlyOnce() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(scm))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pluggable SCM 'scm-name' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldConcatenateMultipleErrorsWithNewLineChar() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[unknown_id][lookup_password]}}");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));


            Rules rules = new Rules(new Allow("refer", "environment", "scm-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(scm))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Pluggable SCM 'scm-name' does not have permission to refer to secrets using secret config 'secret_config_id'.\nPluggable SCM 'scm-name' is referring to none-existent secret config 'unknown_id'.");
        }
    }

    @Nested
    class ForPackageMaterials {
        @Test
        void shouldErrorOutIfPackageRepositoryDoesNotHavePermissionToReferASecretConfig() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));

            Rules rules = new Rules(new Allow("refer", "package_repository", "repo1-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Material 'repo-name' does not have permission to refer to secrets using secret config 'secret_config_id'.");
        }

        @Test
        void shouldValidateIfPackageRepoHasPermissionToReferASecretConfig() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));

            Rules rules = new Rules(new Allow("refer", "package_repository", "repo-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .doesNotThrowAnyException();

            verify(goConfigService, times(1)).getSecretConfigById(anyString());
        }

        @Test
        void shouldErrorOutWhenPackageMaterialIsReferringToNoneExistingSecretConfig() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[unknown_id][password]}}"));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Material 'repo-name' is referring to none-existent secret config 'unknown_id'.");
        }

        @Test
        void shouldBeValidIfPackageIsNotDefinedUsingSecretParams() {
            PackageMaterial material = MaterialsMother.packageMaterial();

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .doesNotThrowAnyException();
            verifyNoInteractions(goConfigService);
        }

        @Test
        void shouldAddErrorForASecretConfigIdOnlyOnce() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getPackageDefinition().getRepository().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][token]}}"));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Material 'repo-name' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldConcatenateMultipleErrorsWithNewLineChar() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getPackageDefinition().getRepository().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[unknown_id][password]}}"));

            Rules rules = new Rules(new Allow("refer", "package_repository", "abc-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(material))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Material 'repo-name' does not have permission to refer to secrets using secret config 'secret_config_id'.\nPackage Material 'repo-name' is referring to none-existent secret config 'unknown_id'.");
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
            when(goConfigService.findGroupByPipeline(any(CaseInsensitiveString.class))).thenReturn(defaultGroup);

            rulesService.validateSecretConfigReferences(buildAssigment);
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

    @Nested
    class ForPackageRepository {
        @Test
        void shouldErrorOutIfItDoesNotHavePermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1, k2));

            Rules rules = new Rules(new Allow("refer", "package_repository", "abc-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(repository))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Repository 'repo-name' does not have permission to refer to secrets using secret config 'secret_config_id'.");
        }

        @Test
        void shouldValidateHasPermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1, k2));

            Rules rules = new Rules(new Allow("refer", "package_repository", "repo-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(repository))
                    .doesNotThrowAnyException();

            verify(goConfigService, times(1)).getSecretConfigById(anyString());
        }

        @Test
        void shouldErrorOutWhenReferringToNoneExistingSecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1, k2));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(repository))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Repository 'repo-name' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldBeValidIfNotDefinedUsingSecretParams() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1, k2));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(repository))
                    .doesNotThrowAnyException();

            verify(goConfigService, never()).getSecretConfigById(anyString());
        }

        @Test
        void shouldAddErrorForASecretConfigIdOnlyOnce() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1, k2));

            assertThatCode(() -> rulesService.validateSecretConfigReferences(repository))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Repository 'repo-name' is referring to none-existent secret config 'secret_config_id'.");
        }
    }

    @Nested
    class ForPackageDefinition {
        @Test
        void shouldErrorOutIfItDoesNotHavePermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1));
            PackageDefinition packageDefinition = new PackageDefinition("pkg-id", "pkg-name", new Configuration(k2));
            packageDefinition.setRepository(repository);

            Rules rules = new Rules(new Allow("refer", "package_repository", "abc-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(packageDefinition))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Repository 'repo-name' does not have permission to refer to secrets using secret config 'secret_config_id'.");
        }

        @Test
        void shouldValidateHasPermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1));
            PackageDefinition packageDefinition = new PackageDefinition("pkg-id", "pkg-name", new Configuration(k2));
            packageDefinition.setRepository(repository);

            Rules rules = new Rules(new Allow("refer", "package_repository", "repo-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(packageDefinition))
                    .doesNotThrowAnyException();

            verify(goConfigService, times(1)).getSecretConfigById(anyString());
        }

        @Test
        void shouldErrorOutWhenReferringToNoneExistingSecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1));
            PackageDefinition packageDefinition = new PackageDefinition("pkg-id", "pkg-name", new Configuration(k2));
            packageDefinition.setRepository(repository);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(repository))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Package Repository 'repo-name' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldBeValidIfNotDefinedUsingSecretParams() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1));
            PackageDefinition packageDefinition = new PackageDefinition("pkg-id", "pkg-name", new Configuration(k2));
            packageDefinition.setRepository(repository);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(repository))
                    .doesNotThrowAnyException();

            verify(goConfigService, never()).getSecretConfigById(anyString());
        }
    }

    @Nested
    class ForClusterProfile {
        @Test
        void shouldErrorOutIfItDoesNotHavePermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ClusterProfile clusterProfile = new ClusterProfile("cluster-id", "plugin-id", k1, k2);

            Rules rules = new Rules(new Allow("refer", "cluster_profile", "abc-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(clusterProfile))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Cluster Profile 'cluster-id' does not have permission to refer to secrets using secret config 'secret_config_id'.");
        }

        @Test
        void shouldValidateHasPermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ClusterProfile clusterProfile = new ClusterProfile("cluster-id", "plugin-id", k1, k2);

            Rules rules = new Rules(new Allow("refer", "cluster_profile", "cluster-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(clusterProfile))
                    .doesNotThrowAnyException();

            verify(goConfigService, times(1)).getSecretConfigById(anyString());
        }

        @Test
        void shouldErrorOutWhenReferringToNoneExistingSecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ClusterProfile clusterProfile = new ClusterProfile("cluster-id", "plugin-id", k1, k2);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(clusterProfile))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Cluster Profile 'cluster-id' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldBeValidIfNotDefinedUsingSecretParams() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            ClusterProfile clusterProfile = new ClusterProfile("cluster-id", "plugin-id", k1, k2);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(clusterProfile))
                    .doesNotThrowAnyException();

            verify(goConfigService, never()).getSecretConfigById(anyString());
        }
    }

    @Nested
    class ForElasticProfile {
        @Test
        void shouldErrorOutIfItDoesNotHavePermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ElasticProfile elasticProfile = new ElasticProfile("elastic-id", "cluster-profile-id", k1, k2);

            Rules rules = new Rules(new Allow("refer", "cluster_profile", "abc-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(elasticProfile))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Cluster Profile 'cluster-profile-id' does not have permission to refer to secrets using secret config 'secret_config_id'.");
        }

        @Test
        void shouldValidateHasPermissionToReferASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ElasticProfile elasticProfile = new ElasticProfile("elastic-id", "cluster-profile-id", k1, k2);

            Rules rules = new Rules(new Allow("refer", "cluster_profile", "cluster-*"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            when(goConfigService.getSecretConfigById("secret_config_id")).thenReturn(secretConfig);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(elasticProfile))
                    .doesNotThrowAnyException();

            verify(goConfigService, times(1)).getSecretConfigById(anyString());
        }

        @Test
        void shouldErrorOutWhenReferringToNoneExistingSecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ElasticProfile elasticProfile = new ElasticProfile("elastic-id", "cluster-profile-id", k1, k2);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(elasticProfile))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Cluster Profile 'cluster-profile-id' is referring to none-existent secret config 'secret_config_id'.");
        }

        @Test
        void shouldBeValidIfNotDefinedUsingSecretParams() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            ElasticProfile elasticProfile = new ElasticProfile("elastic-id", "cluster-profile-id", k1, k2);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(elasticProfile))
                    .doesNotThrowAnyException();

            verify(goConfigService, never()).getSecretConfigById(anyString());
        }
    }

    private JobPlan defaultJobPlan(EnvironmentVariables variables, EnvironmentVariables triggerVariables, JobIdentifier jobIdentifier) {
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), -1, jobIdentifier, null,
                variables, triggerVariables, null, null);
    }
}
