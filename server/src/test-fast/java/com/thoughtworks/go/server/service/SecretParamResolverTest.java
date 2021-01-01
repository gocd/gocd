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
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
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

import static com.thoughtworks.go.helper.MaterialsMother.*;
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
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password"))))
                    .thenReturn(singletonList(new Secret("password", "some-password")));

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

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
        }
    }

    @Nested
    class ResolveSecretsForPluggableScmMaterials {
        @Test
        void shouldResolveSecretParams_IfAMaterialCanReferToASecretConfig() {
            PluggableSCMMaterial material = pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

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
            PluggableSCMMaterial material = pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(material);

            assertThatCode(() -> secretParamResolver.resolve(material))
                    .isInstanceOf(RuntimeException.class);

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
        }
    }

    @Nested
    class ResolveSecretsForPluggableScmConfig {
        @Test
        void shouldResolveSecretParams_IfConfigCanReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][password]}}");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password"))))
                    .thenReturn(singletonList(new Secret("password", "some-password")));

            assertThat(scm.getSecretParams().get(0).isUnresolved()).isTrue();

            secretParamResolver.resolve(scm);

            verify(rulesService).validateSecretConfigReferences(scm);
            assertThat(scm.getSecretParams().get(0).isUnresolved()).isFalse();
            assertThat(scm.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfConfigDoesNotHavePermissionToReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
            SCM scm = new SCM("scm-id", "scm-name");
            scm.getConfiguration().addAll(asList(k1, k2));


            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(scm);

            assertThatCode(() -> secretParamResolver.resolve(scm))
                    .isInstanceOf(RuntimeException.class);

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
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
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password"))))
                    .thenReturn(singletonList(new Secret("password", "some-password")));

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

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
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
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password"))))
                    .thenReturn(singletonList(new Secret("password", "some-password")));

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

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
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

    @Nested
    class ResolveForList {
        @Test
        void shouldResolveListOfMaterials() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");
            PluggableSCMMaterial pluggableSCMMaterial = pluggableSCMMaterial();
            pluggableSCMMaterial.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][token]}}"));

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password")))).thenReturn(singletonList(new Secret("password", "some-password")));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("token")))).thenReturn(singletonList(new Secret("token", "some-token")));

            secretParamResolver.resolve(asList(gitMaterial, pluggableSCMMaterial));

            verify(rulesService).validateSecretConfigReferences(gitMaterial);
            verify(rulesService).validateSecretConfigReferences(pluggableSCMMaterial);

            assertThat(gitMaterial.passwordForCommandLine()).isEqualTo("some-password");
            assertThat(pluggableSCMMaterial.getSecretParams().get(0).getValue()).isEqualTo("some-token");
        }

        @Test
        void shouldOnlyResolveScmAndPluggableScmAndPackageMaterials() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");
            DependencyMaterial dependencyMaterial = dependencyMaterial("{{SECRET:[secret_id][pipeline]}}", "defaultStage");
            PackageMaterial packageMaterial = packageMaterial();
            packageMaterial.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][package_token]}}"));
            PluggableSCMMaterial pluggableSCMMaterial = pluggableSCMMaterial();
            pluggableSCMMaterial.getScmConfig().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][token]}}"));

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password")))).thenReturn(singletonList(new Secret("password", "some-password")));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("token")))).thenReturn(singletonList(new Secret("token", "some-token")));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("package_token")))).thenReturn(singletonList(new Secret("package_token", "some-package-token")));

            secretParamResolver.resolve(asList(gitMaterial, dependencyMaterial, packageMaterial, pluggableSCMMaterial));

            verify(rulesService).validateSecretConfigReferences(gitMaterial);
            verify(rulesService).validateSecretConfigReferences(pluggableSCMMaterial);
            verify(rulesService).validateSecretConfigReferences(packageMaterial);
            verifyNoMoreInteractions(rulesService);
        }
    }

    @Nested
    class ResolveSecretsForPackageMaterials {
        @Test
        void shouldResolveSecretParams_IfAMaterialCanReferToASecretConfig() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");

            when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password")))).thenReturn(singletonList(new Secret("password", "some-password")));

            assertThat(material.getSecretParams().get(0).isUnresolved()).isTrue();

            secretParamResolver.resolve(material);

            verify(rulesService).validateSecretConfigReferences(material);
            assertThat(material.getSecretParams().get(0).isUnresolved()).isFalse();
            assertThat(material.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfMaterialsDoNotHavePermissionToReferToASecretConfig() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][password]}}"));

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(material);

            assertThatCode(() -> secretParamResolver.resolve(material))
                    .isInstanceOf(RuntimeException.class);

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
        }
    }

    @Nested
    class ResolveSecretsForPackageRepository {
        @Test
        void shouldResolveSecretParams_IfConfigCanReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][password]}}");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1, k2));

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password")))).thenReturn(singletonList(new Secret("password", "some-password")));

            assertThat(repository.getSecretParams().get(0).isUnresolved()).isTrue();

            secretParamResolver.resolve(repository);

            verify(rulesService).validateSecretConfigReferences(repository);
            assertThat(repository.getSecretParams().get(0).isUnresolved()).isFalse();
            assertThat(repository.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfConfigDoesNotHavePermissionToReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1, k2));

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(repository);

            assertThatCode(() -> secretParamResolver.resolve(repository))
                    .isInstanceOf(RuntimeException.class);

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
        }
    }

    @Nested
    class ResolveSecretsForPackageDefinition {
        @Test
        void shouldResolveSecretParams_IfConfigCanReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][password]}}");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1));
            PackageDefinition packageDefinition = new PackageDefinition("pkg-id", "pkg-name", new Configuration(k2));
            packageDefinition.setRepository(repository);

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password")))).thenReturn(singletonList(new Secret("password", "some-password")));

            assertThat(packageDefinition.getSecretParams().get(0).isUnresolved()).isTrue();

            secretParamResolver.resolve(packageDefinition);

            verify(rulesService).validateSecretConfigReferences(packageDefinition);
            assertThat(packageDefinition.getSecretParams().get(0).isUnresolved()).isFalse();
            assertThat(packageDefinition.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfConfigDoesNotHavePermissionToReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            PackageRepository repository = new PackageRepository("repo-id", "repo-name", new PluginConfiguration(), new Configuration(k1));
            PackageDefinition packageDefinition = new PackageDefinition("pkg-id", "pkg-name", new Configuration(k2));
            packageDefinition.setRepository(repository);

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(packageDefinition);

            assertThatCode(() -> secretParamResolver.resolve(packageDefinition))
                    .isInstanceOf(RuntimeException.class);

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
        }
    }

    @Nested
    class ResolveSecretsForClusterProfile {
        @Test
        void shouldResolveSecretParams_IfConfigCanReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][password]}}");
            ClusterProfile clusterProfile = new ClusterProfile("cluster-id", "plugin-id", k1, k2);

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password")))).thenReturn(singletonList(new Secret("password", "some-password")));

            assertThat(clusterProfile.getSecretParams().get(0).isUnresolved()).isTrue();

            secretParamResolver.resolve(clusterProfile);

            verify(rulesService).validateSecretConfigReferences(clusterProfile);
            assertThat(clusterProfile.getSecretParams().get(0).isUnresolved()).isFalse();
            assertThat(clusterProfile.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfConfigDoesNotHavePermissionToReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ClusterProfile clusterProfile = new ClusterProfile("cluster-id", "plugin-id", k1, k2);

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(clusterProfile);

            assertThatCode(() -> secretParamResolver.resolve(clusterProfile))
                    .isInstanceOf(RuntimeException.class);

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
        }
    }

    @Nested
    class ResolveSecretsForElasticProfile {
        @Test
        void shouldResolveSecretParams_IfConfigCanReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][password]}}");
            ElasticProfile elasticProfile = new ElasticProfile("elastic-id", "cluster-profile-id", k1, k2);

            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file");
            when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(secretConfig));
            when(secretsExtension.lookupSecrets("cd.go.file", secretConfig, new HashSet<>(singletonList("password")))).thenReturn(singletonList(new Secret("password", "some-password")));

            assertThat(elasticProfile.getSecretParams().get(0).isUnresolved()).isTrue();

            secretParamResolver.resolve(elasticProfile);

            verify(rulesService).validateSecretConfigReferences(elasticProfile);
            assertThat(elasticProfile.getSecretParams().get(0).isUnresolved()).isFalse();
            assertThat(elasticProfile.getSecretParams().get(0).getValue()).isEqualTo("some-password");
        }

        @Test
        void shouldErrorOut_IfConfigDoesNotHavePermissionToReferToASecretConfig() {
            ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
            ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
            ElasticProfile elasticProfile = new ElasticProfile("elastic-id", "cluster-profile-id", k1, k2);

            doThrow(new RuntimeException()).when(rulesService).validateSecretConfigReferences(elasticProfile);

            assertThatCode(() -> secretParamResolver.resolve(elasticProfile))
                    .isInstanceOf(RuntimeException.class);

            verifyNoInteractions(goConfigService);
            verifyNoInteractions(secretsExtension);
        }
    }

    private JobPlan defaultJobPlan(EnvironmentVariables variables, EnvironmentVariables triggerVariables) {
        JobIdentifier identifier = new JobIdentifier("Up42", 1, "1", "test", "1", "unit_test", 123L);
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), -1, identifier, null,
                variables, triggerVariables, null, null);
    }
}
