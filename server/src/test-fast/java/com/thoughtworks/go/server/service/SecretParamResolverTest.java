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
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.plugin.domain.secrets.Secret;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.thoughtworks.go.helper.MaterialsMother.gitMaterial;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class SecretParamResolverTest {
    @Mock
    private SecretsExtension secretsExtension;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private RulesService rulesService;
    private SecretParamResolver secretParamResolver;

    @BeforeEach
    void setUp() {
        initMocks(this);

        secretParamResolver = new SecretParamResolver(secretsExtension, goConfigService, rulesService);
    }

    @Nested
    class ResolveSecretsForScmMaterials {
        @Test
        void shouldResolveSecretParams_IfAMaterialCanReferToASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig())
                    .thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(asList("password"))))
                    .thenReturn(asList(new Secret("password", "some-password")));

            secretParamResolver.resolve(gitMaterial);

            verify(rulesService).validateSecretConfigReferences(gitMaterial);
            assertThat(gitMaterial.passwordForCommandLine()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfMaterialsDoNotHavePermissionToReferToASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(gitMaterial);

            assertThatCode(() -> secretParamResolver.resolve(gitMaterial))
                    .isInstanceOf(RuntimeException.class);

            verifyZeroInteractions(goConfigService);
            verifyZeroInteractions(secretsExtension);
        }
    }

    @Nested
    class ResolveSecretsForPluggableScmMaterials {
        @Test
        void shouldResolveSecretParams_IfAMaterialCanReferToASecretConfig() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            SCM scmConfig = material.getScmConfig();
            scmConfig.getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            scmConfig.getConfiguration().get(1).handleSecureValueConfiguration(true);
            material.setSCMConfig(scmConfig);

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig())
                    .thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password"))))
                    .thenReturn(singletonList(new Secret("password", "some-password")));

            assertThat(material.getSecretParams().get(0).isUnresolved()).isTrue();

            secretParamResolver.resolve(material);

            verify(rulesService).validateSecretConfigReferences(material);
            assertThat(material.getSecretParams().get(0).isUnresolved()).isFalse();
            assertThat(material.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfMaterialsDoNotHavePermissionToReferToASecretConfig() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(material);

            assertThatCode(() -> secretParamResolver.resolve(material))
                    .isInstanceOf(RuntimeException.class);

            verifyZeroInteractions(goConfigService);
            verifyZeroInteractions(secretsExtension);
        }
    }

    @Nested
    class ResolveSecretsForBuildAssignment {
        @Test
        void shouldResolveSecretParams_IfBuildAssignmentCanReferSecretConfig() {
            EnvironmentVariables jobEnvironmentVariables = new EnvironmentVariables();
            jobEnvironmentVariables.add("Token", "{{SECRET:[secret_config_id][password]}}");
            BuildAssignment buildAssigment = createAssignment(null, jobEnvironmentVariables);

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig())
                    .thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(asList("password"))))
                    .thenReturn(asList(new Secret("password", "some-password")));

            secretParamResolver.resolve(buildAssigment);

            verify(rulesService).validateSecretConfigReferences(buildAssigment);
            assertThat(buildAssigment.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfBuildAssignmentDoNotHavePermissionToReferToASecretConfig() {
            EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
            environmentVariableContext.setProperty("Token", "{{SECRET:[secret_config_id][password]}}", false);

            BuildAssignment buildAssigment = createAssignment(environmentVariableContext);

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(buildAssigment);

            assertThatCode(() -> secretParamResolver.resolve(buildAssigment))
                    .isInstanceOf(RuntimeException.class);

            verifyZeroInteractions(goConfigService);
            verifyZeroInteractions(secretsExtension);
        }

        private BuildAssignment createAssignment(EnvironmentVariableContext environmentVariableContext) {
            return createAssignment(environmentVariableContext, new EnvironmentVariables());
        }

        private BuildAssignment createAssignment(EnvironmentVariableContext environmentVariableContext, EnvironmentVariables jobEnvironmentVariables) {
            ScmMaterial gitMaterial = gitMaterial("https://example.org");
            MaterialRevision gitRevision = new MaterialRevision(gitMaterial, new Modification());
            BuildCause buildCause = BuildCause.createManualForced(new MaterialRevisions(gitRevision), Username.ANONYMOUS);

            JobPlan plan = defaultJobPlan(jobEnvironmentVariables, new EnvironmentVariables());
            List<Builder> builders = new ArrayList<>();
            builders.add(new CommandBuilder("ls", "", null, new RunIfConfigs(), new NullBuilder(), ""));
            return BuildAssignment.create(plan, buildCause, builders, null, environmentVariableContext, new ArtifactStores());
        }
    }

    @Nested
    class ResolveEnvironmentConfig {
        @Test
        void shouldResolveSecretParams_IfJobPlanCanReferSecretConfig() {
            BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("dev"));
            environmentConfig.addEnvironmentVariable("key", "{{SECRET:[secret_config_id][password]}}");

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig())
                    .thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(asList("password"))))
                    .thenReturn(asList(new Secret("password", "some-password")));

            secretParamResolver.resolve(environmentConfig);

            verify(rulesService).validateSecretConfigReferences(environmentConfig);
            assertThat(environmentConfig.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfJobPlanDoNotHavePermissionToReferToASecretConfig() {
            BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("dev"));
            environmentConfig.addEnvironmentVariable("key", "{{SECRET:[secret_config_id][password]}}");

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(environmentConfig);

            assertThatCode(() -> secretParamResolver.resolve(environmentConfig))
                    .isInstanceOf(RuntimeException.class);

            verifyZeroInteractions(goConfigService);
            verifyZeroInteractions(secretsExtension);
        }
    }

    @Test
    void shouldResolveSecretParamsOfVariousSecretConfig() {
        final SecretParams allSecretParams = new SecretParams(
                new SecretParam("secret_config_id_1", "username"),
                new SecretParam("secret_config_id_1", "password"),
                new SecretParam("secret_config_id_2", "access_key"),
                new SecretParam("secret_config_id_2", "secret_key")
        );
        final SecretConfig fileBasedSecretConfig = new SecretConfig("secret_config_id_1", "cd.go.file");
        final SecretConfig awsBasedSecretConfig = new SecretConfig("secret_config_id_2", "cd.go.aws");
        when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(fileBasedSecretConfig, awsBasedSecretConfig));
        when(secretsExtension.lookupSecrets(fileBasedSecretConfig.getPluginId(), fileBasedSecretConfig, new HashSet<>(asList("username", "password"))))
                .thenReturn(asList(new Secret("username", "some-username"), new Secret("password", "some-password")));
        when(secretsExtension.lookupSecrets(awsBasedSecretConfig.getPluginId(), awsBasedSecretConfig, new HashSet<>(asList("access_key", "secret_key"))))
                .thenReturn(asList(new Secret("access_key", "ABCDEFGHIJ1D"), new Secret("secret_key", "xyzdfjsdlwdoasd;q")));


        assertThat(allSecretParams).hasSize(4);
        assertThat(allSecretParams.get(0).getValue()).isNull();
        assertThat(allSecretParams.get(1).getValue()).isNull();
        assertThat(allSecretParams.get(2).getValue()).isNull();
        assertThat(allSecretParams.get(3).getValue()).isNull();

        secretParamResolver.resolve(allSecretParams);

        assertThat(allSecretParams).hasSize(4);
        assertThat(allSecretParams.get(0).getValue()).isEqualTo("some-username");
        assertThat(allSecretParams.get(1).getValue()).isEqualTo("some-password");
        assertThat(allSecretParams.get(2).getValue()).isEqualTo("ABCDEFGHIJ1D");
        assertThat(allSecretParams.get(3).getValue()).isEqualTo("xyzdfjsdlwdoasd;q");
    }

    @Test
    void shouldResolveValueInAllParamsEvenIfListContainsDuplicatedSecretParams() {
        final SecretParams allSecretParams = new SecretParams(
                new SecretParam("secret_config_id_1", "username"),
                new SecretParam("secret_config_id_1", "username")
        );

        final SecretConfig fileBasedSecretConfig = new SecretConfig("secret_config_id_1", "cd.go.file");
        when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(fileBasedSecretConfig));
        when(secretsExtension.lookupSecrets(fileBasedSecretConfig.getPluginId(), fileBasedSecretConfig, singleton("username")))
                .thenReturn(singletonList(new Secret("username", "some-username")));

        secretParamResolver.resolve(allSecretParams);

        assertThat(allSecretParams).hasSize(2);
        assertThat(allSecretParams.get(0).getValue()).isEqualTo("some-username");
        assertThat(allSecretParams.get(1).getValue()).isEqualTo("some-username");
    }

    private JobPlan defaultJobPlan(EnvironmentVariables variables, EnvironmentVariables triggerVariables) {
        JobIdentifier identifier = new JobIdentifier("Up42", 1, "1", "test", "1", "unit_test", 123L);
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), -1, identifier, null,
                variables, triggerVariables, null, null);
    }
}
