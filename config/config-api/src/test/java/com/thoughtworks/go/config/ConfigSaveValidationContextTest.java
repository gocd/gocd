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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.rules.RulesValidationContext;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigSaveValidationContextTest {

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void testShouldReturnTrueIfTemplatesIsAnAncestor() {
        ValidationContext context = ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), new TemplatesConfig(), new PipelineTemplateConfig());
        assertThat(context.isWithinTemplates()).isTrue();
    }

    @Test
    void testShouldReturnFalseIfTemplatesIsNotAnAncestor() {
        ValidationContext context = ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), new PipelineGroups(), new BasicPipelineConfigs(), new PipelineConfig());
        assertThat(context.isWithinTemplates()).isFalse();
    }

    @Test
    void shouldReturnAllMaterialsMatchingTheFingerprint() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        HgMaterialConfig hg = hg("url", null);
        for (int i = 0; i < 10; i++) {
            PipelineConfig pipelineConfig = pipelineConfig("pipeline" + i, new MaterialConfigs(hg));
            cruiseConfig.addPipeline("defaultGroup", pipelineConfig);
        }
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertThat(context.getAllMaterialsByFingerPrint(hg.getFingerprint()).size()).isEqualTo(10);
    }

    @Test
    void shouldReturnEmptyListWhenNoMaterialsMatch() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);
        assertThat(context.getAllMaterialsByFingerPrint("something").isEmpty()).isTrue();
    }

    @Test
    void shouldGetPipelineConfigByName() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);
        assertThat(context.getPipelineConfigByName(new CaseInsensitiveString("p1"))).isEqualTo(cruiseConfig.allPipelines().get(0));
        assertThat(context.getPipelineConfigByName(new CaseInsensitiveString("does_not_exist"))).isNull();
    }

    @Test
    void shouldGetServerSecurityConfig() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);
        assertThat(context.getServerSecurityConfig()).isEqualTo(cruiseConfig.server().security());
    }

    @Test
    void shouldReturnIfTheContextBelongsToPipeline() {
        ValidationContext context = ConfigSaveValidationContext.forChain(new BasicPipelineConfigs());
        assertThat(context.isWithinPipelines()).isTrue();
        assertThat(context.isWithinTemplates()).isFalse();
    }

    @Test
    void shouldReturnIfTheContextBelongsToTemplate() {
        ValidationContext context = ConfigSaveValidationContext.forChain(new TemplatesConfig());
        assertThat(context.isWithinPipelines()).isFalse();
        assertThat(context.isWithinTemplates()).isTrue();
    }

    @Test
    void shouldCheckForExistenceOfTemplate() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("t1")));
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertThat(context.doesTemplateExist(new CaseInsensitiveString("t1"))).isTrue();
        assertThat(context.doesTemplateExist(new CaseInsensitiveString("t2"))).isFalse();
    }

    @Test
    void shouldCheckForExistenceOfSCMS() throws Exception {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(new SCMs(SCMMother.create("scm-id")));
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertThat(context.findScmById("scm-id").getId()).isEqualTo("scm-id");
    }

    @Test
    void shouldCheckForExistenceOfPackage() throws Exception {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setPackageRepositories(new PackageRepositories(PackageRepositoryMother.create("repo-id")));
        cruiseConfig.getPackageRepositories().find("repo-id").setPackages(new Packages(PackageDefinitionMother.create("package-id")));
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertThat(context.findPackageById("package-id").getId()).isEqualTo("repo-id");
    }

    @Test
    void isValidProfileIdShouldBeValidInPresenceOfElasticProfile() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.setProfiles(new ElasticProfiles(new ElasticProfile("docker.unit-test", "prod-cluster")));
        cruiseConfig.setElasticConfig(elasticConfig);
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertTrue(context.isValidProfileId("docker.unit-test"));
    }

    @Test
    void shouldGetAllClusterProfiles() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ElasticConfig elasticConfig = new ElasticConfig();
        ClusterProfiles clusterProfiles = new ClusterProfiles(new ClusterProfile("cluster1", "docker"));
        elasticConfig.setClusterProfiles(clusterProfiles);
        cruiseConfig.setElasticConfig(elasticConfig);
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertThat(context.getClusterProfiles()).isEqualTo(clusterProfiles);
    }

    @Test
    void isValidProfileIdShouldBeInValidInAbsenceOfElasticProfileForTheGivenId() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.setProfiles(new ElasticProfiles(new ElasticProfile("docker.unit-test", "prod-cluster")));
        cruiseConfig.setElasticConfig(elasticConfig);
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertThat(context.isValidProfileId("invalid.profile-id")).isFalse();
    }

    @Test
    void isValidProfileIdShouldBeInValidInAbsenceOfElasticProfiles() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertThat(context.isValidProfileId("docker.unit-test")).isFalse();
    }

    @Nested
    class rulesValidationContext {
        @Test
        void shouldBuildRulesValidationContext() {
            SecretConfig secretConfig = new SecretConfig();
            ConfigSaveValidationContext configSaveValidationContext = ConfigSaveValidationContext.forChain(secretConfig);

            RulesValidationContext rulesValidationContext = configSaveValidationContext.getRulesValidationContext();

            assertThat(rulesValidationContext.getAllowedActions()).isEqualTo(secretConfig.allowedActions());
            assertThat(rulesValidationContext.getAllowedTypes()).isEqualTo(secretConfig.allowedTypes());
        }
    }

    @Nested
    class PolicyValidationContext {
        @Test
        void shouldBuilPolicyValidationContext() {
            RoleConfig roleConfig = new RoleConfig("role");
            ConfigSaveValidationContext configSaveValidationContext = ConfigSaveValidationContext.forChain(roleConfig);

            com.thoughtworks.go.config.policy.PolicyValidationContext policyValidationContext = configSaveValidationContext.getPolicyValidationContext();

            assertThat(policyValidationContext.getAllowedActions()).isEqualTo(roleConfig.allowedActions());
            assertThat(policyValidationContext.getAllowedTypes()).isEqualTo(roleConfig.allowedTypes());

        }
    }

    @Test
    void shouldReturnThePipelineNamesWithTheMaterialFingerprint() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        HgMaterialConfig hg = hg("url", null);
        for (int i = 0; i < 5; i++) {
            PipelineConfig pipelineConfig = pipelineConfig("pipeline" + i, new MaterialConfigs(hg));
            cruiseConfig.addPipeline("defaultGroup", pipelineConfig);
        }
        cruiseConfig.addPipeline("defaultGroup", pipelineConfig("another-pipeline", new MaterialConfigs(hg("url2", "folder"))));
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        Map<CaseInsensitiveString, Boolean> pipelinesWithMaterial = context.getPipelineToMaterialAutoUpdateMapByFingerprint(hg.getFingerprint());
        assertThat(pipelinesWithMaterial.size()).isEqualTo(5);
        assertThat(pipelinesWithMaterial.keySet()).doesNotContain(new CaseInsensitiveString("another-pipeline"));
    }
}
