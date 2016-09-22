/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.util.Node;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class PipelineConfigSaveValidationContextTest {

    private PipelineConfig pipelineConfig;
    private PipelineConfigSaveValidationContext pipelineContext;

    @Before
    public void setUp() throws Exception {
        pipelineConfig = mock(PipelineConfig.class);
        pipelineContext = PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig);
    }

    @Test
    public void shouldCreatePipelineValidationContext() {
        assertThat(pipelineContext.getPipeline(), is(pipelineConfig));
        assertThat(pipelineContext.getStage(), is(nullValue()));
        assertThat(pipelineContext.getJob(), is(nullValue()));
    }

    @Test
    public void shouldCreateStageValidationContextBasedOnParent() {
        StageConfig stageConfig = mock(StageConfig.class);
        PipelineConfigSaveValidationContext stageContext = PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, stageConfig);

        assertThat(stageContext.getPipeline(), is(pipelineConfig));
        assertThat(stageContext.getStage(), is(stageConfig));
        assertThat(stageContext.getJob(), is(nullValue()));
    }

    @Test
    public void shouldCreateJobValidationContextBasedOnParent() {
        StageConfig stageConfig = mock(StageConfig.class);
        JobConfig jobConfig = mock(JobConfig.class);
        PipelineConfigSaveValidationContext jobContext = PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, stageConfig, jobConfig);
        assertThat(jobContext.getPipeline(), is(pipelineConfig));
        assertThat(jobContext.getStage(), is(stageConfig));
        assertThat(jobContext.getJob(), is(jobConfig));
    }

    @Test
    public void shouldGetAllMaterialsByFingerPrint() throws Exception {
        CruiseConfig cruiseConfig = new GoConfigMother().cruiseConfigWithPipelineUsingTwoMaterials();
        MaterialConfig expectedMaterial = MaterialConfigsMother.multipleMaterialConfigs().get(1);
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);
        MaterialConfigs allMaterialsByFingerPrint = context.getAllMaterialsByFingerPrint(expectedMaterial.getFingerprint());
        assertThat(allMaterialsByFingerPrint.size(), is(1));
        assertThat(allMaterialsByFingerPrint.first(), is(expectedMaterial));
    }
    @Test
    public void shouldReturnNullIfMatchingMaterialConfigIsNotFound() throws Exception {
        CruiseConfig cruiseConfig = new GoConfigMother().cruiseConfigWithPipelineUsingTwoMaterials();
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);
        assertThat(context.getAllMaterialsByFingerPrint("does_not_exist"), is(nullValue()));
    }

    @Test
    public void shouldGetDependencyMaterialsForPipelines(){
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2", "p3");
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"),new CaseInsensitiveString("stage") ));
        PipelineConfig p3 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p3"));
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"),new CaseInsensitiveString("stage") ));
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);


        assertThat(context.getDependencyMaterialsFor(new CaseInsensitiveString("p1")).getDependencies().isEmpty(), is(true));
        assertThat(context.getDependencyMaterialsFor(new CaseInsensitiveString("p2")).getDependencies(), contains(new Node.DependencyNode(new CaseInsensitiveString("p1"),new CaseInsensitiveString("stage"))));
        assertThat(context.getDependencyMaterialsFor(new CaseInsensitiveString("p3")).getDependencies(), contains(new Node.DependencyNode(new CaseInsensitiveString("p2"),new CaseInsensitiveString("stage"))));
        assertThat(context.getDependencyMaterialsFor(new CaseInsensitiveString("junk")).getDependencies().isEmpty(), is(true));
    }

    @Test
    public void shouldGetParentDisplayName(){
        assertThat(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()).getParentDisplayName(), is("pipeline"));
        assertThat(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), new StageConfig()).getParentDisplayName(), is("stage"));
        assertThat(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), new StageConfig(), new JobConfig()).getParentDisplayName(), is("job"));
    }

    @Test
    public void shouldFindPipelineByName(){
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig(new CaseInsensitiveString("p2"), new MaterialConfigs()));
        assertThat(context.getPipelineConfigByName(new CaseInsensitiveString("p1")), is(cruiseConfig.allPipelines().get(0)));
    }

    @Test
    public void shouldReturnNullWhenNoMatchingPipelineIsFound() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig(new CaseInsensitiveString("p2"), new MaterialConfigs()));
        assertThat(context.getPipelineConfigByName(new CaseInsensitiveString("does_not_exist")), is(nullValue()));
    }


    @Test
    public void shouldGetPipelineGroupForPipelineInContext(){
        String pipelineName = "p1";
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines(pipelineName);
        PipelineConfig p1 = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, PipelineConfigs.DEFAULT_GROUP, cruiseConfig, p1);
        assertThat(context.getPipelineGroup(), is(cruiseConfig.findGroup(PipelineConfigs.DEFAULT_GROUP)));
    }

    @Test
    public void shouldGetServerSecurityContext() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.addRole(new Role(new CaseInsensitiveString("admin")));
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("super-admin")));
        cruiseConfig.server().useSecurity(securityConfig);
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);
        Assert.assertThat(context.getServerSecurityConfig(), is(securityConfig));
    }

    @Test
    public void shouldReturnIfTheContextBelongsToPipeline(){
        ValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig());
        Assert.assertThat(context.isWithinPipelines(), is(true));
        Assert.assertThat(context.isWithinTemplates(), is(false));
    }

    @Test
    public void shouldCheckForExistenceOfTemplate(){
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("t1")));
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig());

        assertThat(context.doesTemplateExist(new CaseInsensitiveString("t1")), is(true));
        assertThat(context.doesTemplateExist(new CaseInsensitiveString("t2")), is(false));
    }

    @Test
    public void shouldCheckForExistenceOfSCM() throws Exception {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(new SCMs(SCMMother.create("scm-id")));
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        MatcherAssert.assertThat(context.findScmById("scm-id").getId(), is("scm-id"));

    }

    @Test
    public void shouldCheckForExistenceOfPackage() throws Exception {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setPackageRepositories(new PackageRepositories(PackageRepositoryMother.create("repo-id")));
        cruiseConfig.getPackageRepositories().find("repo-id").setPackages(new Packages(PackageDefinitionMother.create("package-id")));
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        MatcherAssert.assertThat(context.findPackageById("package-id").getId(), is("repo-id"));
    }

    @Test
    public void isValidProfileIdShouldBeValidInPresenceOfElasticProfile() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.setProfiles(new ElasticProfiles(new ElasticProfile("docker.unit-test", "docker")));
        cruiseConfig.setServerConfig(new ServerConfig(elasticConfig));
        ValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig());

        assertTrue(context.isValidProfileId("docker.unit-test"));
    }

    @Test
    public void isValidProfileIdShouldBeInValidInAbsenceOfElasticProfileForTheGivenId() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.setProfiles(new ElasticProfiles(new ElasticProfile("docker.unit-test", "docker")));
        cruiseConfig.setServerConfig(new ServerConfig(elasticConfig));
        ValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig());

        assertFalse(context.isValidProfileId("invalid.profile-id"));
    }

    @Test
    public void isValidProfileIdShouldBeInValidInAbsenceOfElasticProfiles() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, new PipelineConfig());

        assertFalse(context.isValidProfileId("docker.unit-test"));
    }
}