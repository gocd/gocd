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

import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.util.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PipelineConfigSaveValidationContextTest {

    private PipelineConfig pipelineConfig;
    private PipelineConfigSaveValidationContext pipelineContext;

    @BeforeEach
    void setUp() throws Exception {
        pipelineConfig = mock(PipelineConfig.class);
        pipelineContext = PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig);
    }

    @Test
    void shouldCreatePipelineValidationContext() {
        assertThat(pipelineContext.getPipeline()).isEqualTo(pipelineConfig);
        assertThat(pipelineContext.getStage()).isNull();
        assertThat(pipelineContext.getJob()).isNull();
    }

    @Test
    void shouldCreateStageValidationContextBasedOnParent() {
        StageConfig stageConfig = mock(StageConfig.class);
        PipelineConfigSaveValidationContext stageContext = PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, stageConfig);

        assertThat(stageContext.getPipeline()).isEqualTo(pipelineConfig);
        assertThat(stageContext.getStage()).isEqualTo(stageConfig);
        assertThat(stageContext.getJob()).isNull();
    }

    @Test
    void shouldCreateJobValidationContextBasedOnParent() {
        StageConfig stageConfig = mock(StageConfig.class);
        JobConfig jobConfig = mock(JobConfig.class);
        PipelineConfigSaveValidationContext jobContext = PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, stageConfig, jobConfig);
        assertThat(jobContext.getPipeline()).isEqualTo(pipelineConfig);
        assertThat(jobContext.getStage()).isEqualTo(stageConfig);
        assertThat(jobContext.getJob()).isEqualTo(jobConfig);
    }

    @Test
    void shouldGetAllMaterialsByFingerPrint() throws Exception {
        CruiseConfig cruiseConfig = new GoConfigMother().cruiseConfigWithPipelineUsingTwoMaterials();
        MaterialConfig expectedMaterial = MaterialConfigsMother.multipleMaterialConfigs().get(1);
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);
        MaterialConfigs allMaterialsByFingerPrint = context.getAllMaterialsByFingerPrint(expectedMaterial.getFingerprint());
        assertThat(allMaterialsByFingerPrint.size()).isEqualTo(1);
        assertThat(allMaterialsByFingerPrint.first()).isEqualTo(expectedMaterial);
    }

    @Test
    void shouldReturnNullIfMatchingMaterialConfigIsNotFound() throws Exception {
        CruiseConfig cruiseConfig = new GoConfigMother().cruiseConfigWithPipelineUsingTwoMaterials();
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);
        assertThat(context.getAllMaterialsByFingerPrint("does_not_exist")).isNull();
    }

    @Test
    void shouldGetDependencyMaterialsForPipelines() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2", "p3");
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("stage")));
        PipelineConfig p3 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p3"));
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"), new CaseInsensitiveString("stage")));
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);


        assertThat(context.getDependencyMaterialsFor(new CaseInsensitiveString("p1")).getDependencies().isEmpty()).isTrue();
        assertThat(context.getDependencyMaterialsFor(new CaseInsensitiveString("p2")).getDependencies()).containsExactly(new Node.DependencyNode(new CaseInsensitiveString("p1"), new CaseInsensitiveString("stage")));
        assertThat(context.getDependencyMaterialsFor(new CaseInsensitiveString("p3")).getDependencies()).containsExactly(new Node.DependencyNode(new CaseInsensitiveString("p2"), new CaseInsensitiveString("stage")));
        assertThat(context.getDependencyMaterialsFor(new CaseInsensitiveString("junk")).getDependencies().isEmpty()).isTrue();
    }

    @Test
    void shouldGetParentDisplayName() {
        assertThat(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()).getParentDisplayName()).isEqualTo("pipeline");
        assertThat(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), new StageConfig()).getParentDisplayName()).isEqualTo("stage");
        assertThat(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), new StageConfig(), new JobConfig()).getParentDisplayName()).isEqualTo("job");
    }

    @Test
    void shouldFindPipelineByName() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig(new CaseInsensitiveString("p2"), new MaterialConfigs()));
        assertThat(context.getPipelineConfigByName(new CaseInsensitiveString("p1"))).isEqualTo(cruiseConfig.allPipelines().get(0));
    }

    @Test
    void shouldReturnNullWhenNoMatchingPipelineIsFound() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig(new CaseInsensitiveString("p2"), new MaterialConfigs()));
        assertThat(context.getPipelineConfigByName(new CaseInsensitiveString("does_not_exist"))).isNull();
    }


    @Test
    void shouldGetPipelineGroupForPipelineInContext() {
        String pipelineName = "p1";
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines(pipelineName);
        PipelineConfig p1 = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, PipelineConfigs.DEFAULT_GROUP, cruiseConfig, p1);
        assertThat(context.getPipelineGroup()).isEqualTo(cruiseConfig.findGroup(PipelineConfigs.DEFAULT_GROUP));
    }

    @Test
    void shouldGetServerSecurityContext() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("admin")));
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("super-admin")));
        cruiseConfig.server().useSecurity(securityConfig);
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);
        assertThat(context.getServerSecurityConfig()).isEqualTo(securityConfig);
    }

    @Test
    void shouldReturnIfTheContextBelongsToPipeline() {
        ValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig());
        assertThat(context.isWithinPipelines()).isTrue();
        assertThat(context.isWithinTemplates()).isFalse();
    }

    @Test
    void shouldCheckForExistenceOfTemplate() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("t1")));
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig());

        assertThat(context.doesTemplateExist(new CaseInsensitiveString("t1"))).isTrue();
        assertThat(context.doesTemplateExist(new CaseInsensitiveString("t2"))).isFalse();
    }

    @Test
    void shouldCheckForExistenceOfSCM() throws Exception {
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
        ValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig());

        assertTrue(context.isValidProfileId("docker.unit-test"));
    }

    @Test
    void isValidProfileIdShouldBeInValidInAbsenceOfElasticProfileForTheGivenId() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.setProfiles(new ElasticProfiles(new ElasticProfile("docker.unit-test", "prod-cluster")));
        cruiseConfig.setElasticConfig(elasticConfig);
        ValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig());

        assertThat(context.isValidProfileId("invalid.profile-id")).isFalse();
    }

    @Test
    void isValidProfileIdShouldBeInValidInAbsenceOfElasticProfiles() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig());

        assertThat(context.isValidProfileId("docker.unit-test")).isFalse();
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
