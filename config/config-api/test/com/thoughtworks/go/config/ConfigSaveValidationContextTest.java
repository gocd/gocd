/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class ConfigSaveValidationContextTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testShouldReturnTrueIfTemplatesIsAnAncestor() {
        ValidationContext context = ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), new TemplatesConfig(), new PipelineTemplateConfig());
        assertThat(context.isWithinTemplates(), is(true));
    }

    @Test
    public void testShouldReturnFalseIfTemplatesIsNotAnAncestor() {
        ValidationContext context = ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), new PipelineGroups(), new BasicPipelineConfigs(), new PipelineConfig());
        assertThat(context.isWithinTemplates(), is(false));
    }

    @Test
    public void shouldReturnAllMaterialsMatchingTheFingerprint() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        HgMaterialConfig hg = new HgMaterialConfig("url", null);
        for (int i=0; i<10; i++) {
            PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline" + i, new MaterialConfigs(hg));
            cruiseConfig.addPipeline("defaultGroup", pipelineConfig);
        }
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertThat(context.getAllMaterialsByFingerPrint(hg.getFingerprint()).size(), is(10));
    }

    @Test
    public void shouldReturnEmptyListWhenNoMaterialsMatch() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);
        assertThat(context.getAllMaterialsByFingerPrint("something").isEmpty(), is(true));
    }

    @Test
    public void shouldGetPipelineConfigByName(){
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);
        assertThat(context.getPipelineConfigByName(new CaseInsensitiveString("p1")), is(cruiseConfig.allPipelines().get(0)));
        assertThat(context.getPipelineConfigByName(new CaseInsensitiveString("does_not_exist")), is(nullValue()));
    }

    @Test
    public void shouldGetServerSecurityConfig(){
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        GoConfigMother.enableSecurityWithPasswordFile(cruiseConfig);
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);
        assertThat(context.getServerSecurityConfig(), is(cruiseConfig.server().security()));
    }

    @Test
    public void shouldReturnIfTheContextBelongsToPipeline(){
        ValidationContext context = ConfigSaveValidationContext.forChain(new BasicPipelineConfigs());
        assertThat(context.isWithinPipelines(), is(true));
        assertThat(context.isWithinTemplates(), is(false));
    }

    @Test
    public void shouldReturnIfTheContextBelongsToTemplate() {
        ValidationContext context = ConfigSaveValidationContext.forChain(new TemplatesConfig());
        assertThat(context.isWithinPipelines(), is(false));
        assertThat(context.isWithinTemplates(), is(true));
    }

    @Test
    public void shouldCheckForExistenceOfTemplate(){
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("t1")));
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        MatcherAssert.assertThat(context.doesTemplateExist(new CaseInsensitiveString("t1")), is(true));
        MatcherAssert.assertThat(context.doesTemplateExist(new CaseInsensitiveString("t2")), is(false));
    }

    @Test
    public void shouldCheckForExistenceOfSCMS() throws Exception {
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
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertTrue(context.isValidProfileId("docker.unit-test"));
    }

    @Test
    public void isValidProfileIdShouldBeInValidInAbsenceOfElasticProfileForTheGivenId() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.setProfiles(new ElasticProfiles(new ElasticProfile("docker.unit-test", "docker")));
        cruiseConfig.setServerConfig(new ServerConfig(elasticConfig));
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertFalse(context.isValidProfileId("invalid.profile-id"));
    }

    @Test
    public void isValidProfileIdShouldBeInValidInAbsenceOfElasticProfiles() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ValidationContext context = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertFalse(context.isValidProfileId("docker.unit-test"));
    }
}
