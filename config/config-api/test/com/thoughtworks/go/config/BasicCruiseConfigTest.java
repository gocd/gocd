/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.domain.CaseInsensitiveStringTest;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.PiplineConfigVisitor;
import com.thoughtworks.go.helper.*;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BasicCruiseConfigTest extends CruiseConfigTestBase {

    @Before
    public void setup() throws Exception {
        pipelines = new BasicPipelineConfigs("existing_group", new Authorization());
        cruiseConfig = new BasicCruiseConfig(pipelines);
        goConfigMother = new GoConfigMother();
    }

    @Override
    protected BasicCruiseConfig createCruiseConfig(BasicPipelineConfigs pipelineConfigs) {
        return new BasicCruiseConfig(pipelineConfigs);
    }

    @Override
    protected BasicCruiseConfig createCruiseConfig() {
        return new BasicCruiseConfig();
    }

    @Test
    public void getAllLocalPipelineConfigs_shouldReturnOnlyLocalPipelinesWhenNoRemotes() {
        PipelineConfig pipeline1 = createPipelineConfig("local-pipe-1", "stage1");
        cruiseConfig.getGroups().addPipeline("existing_group", pipeline1);

        List<PipelineConfig> localPipelines = cruiseConfig.getAllLocalPipelineConfigs(false);
        assertThat(localPipelines.size(), is(1));
        assertThat(localPipelines, hasItem(pipeline1));
    }

    @Test
    public void shouldGenerateAMapOfAllPipelinesAndTheirParentDependencies() {
        /*
        *    -----+ p2 --> p4
        *  p1
        *    -----+ p3
        *
        * */
        PipelineConfig p1 = createPipelineConfig("p1", "s1", "j1");
        PipelineConfig p2 = createPipelineConfig("p2", "s2", "j1");
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p3 = createPipelineConfig("p3", "s3", "j1");
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p4 = createPipelineConfig("p4", "s4", "j1");
        p4.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"), new CaseInsensitiveString("s2")));
        pipelines.addAll(asList(p4, p2, p1, p3));
        Map<String, List<PipelineConfig>> expectedPipelines = cruiseConfig.generatePipelineVsDownstreamMap();
        assertThat(expectedPipelines.size(), is(4));
        assertThat(expectedPipelines.get("p1"), hasItems(p2, p3));
        assertThat(expectedPipelines.get("p2"), hasItems(p4));
        assertThat(expectedPipelines.get("p3").isEmpty(), is(true));
        assertThat(expectedPipelines.get("p4").isEmpty(), is(true));
    }


    @Test
    public void shouldSetOriginInPipelines() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        PipelineConfig pipe = pipelines.get(0);
        mainCruiseConfig.setOrigins(new FileConfigOrigin());
        assertThat(pipe.getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }

    @Test
    public void shouldSetOriginInEnvironments() {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        BasicEnvironmentConfig env = new BasicEnvironmentConfig(new CaseInsensitiveString("e"));
        mainCruiseConfig.addEnvironment(env);
        mainCruiseConfig.setOrigins(new FileConfigOrigin());
        assertThat(env.getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }


    @Test
    public void shouldGetPipelinesWithGroupName() throws Exception {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        PipelineConfigs group2 = createGroup("group2", createPipelineConfig("pipeline2", "stage2"));
        CruiseConfig config = createCruiseConfig();
        config.setGroup(new PipelineGroups(group1, group2));


        assertThat(config.pipelines("group1"), is(group1));
        assertThat(config.pipelines("group2"), is(group2));
    }

    @Test
    public void shouldReturnTrueForPipelineThatInFirstGroup() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        CruiseConfig config = new BasicCruiseConfig(group1);
        assertThat("shouldReturnTrueForPipelineThatInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline1")), is(true));
    }

    @Test
    public void shouldReturnFalseForPipelineThatNotInFirstGroup() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        PipelineConfigs group2 = createGroup("group2", createPipelineConfig("pipeline2", "stage2"));
        CruiseConfig config = new BasicCruiseConfig(group1, group2);
        assertThat("shouldReturnFalseForPipelineThatNotInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline2")), is(false));
    }

    @Test
    public void shouldIncludeRemotePipelinesAsPartOfCachedPipelineConfigs() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2");
        ConfigRepoConfig repoConfig1 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url1"), "plugin");
        ConfigRepoConfig repoConfig2 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url2"), "plugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig1, repoConfig2));
        PartialConfig partialConfigInRepo1 = PartialConfigMother.withPipeline("pipeline_in_repo1", new RepoConfigOrigin(repoConfig1, "repo1_r1"));
        PartialConfig partialConfigInRepo2 = PartialConfigMother.withPipeline("pipeline_in_repo2", new RepoConfigOrigin(repoConfig2, "repo2_r1"));

        cruiseConfig.merge(asList(partialConfigInRepo1, partialConfigInRepo2), false);
        assertThat(cruiseConfig.getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo1")), is(true));
        assertThat(cruiseConfig.getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo2")), is(true));
    }

    @Test
    public void shouldRejectRemotePipelinesNotOriginatingFromRegisteredConfigReposFromCachedPipelineConfigs() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2");
        ConfigRepoConfig repoConfig1 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url1"), "plugin");
        ConfigRepoConfig repoConfig2 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url2"), "plugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig2));
        PartialConfig partialConfigInRepo1 = PartialConfigMother.withPipeline("pipeline_in_repo1", new RepoConfigOrigin(repoConfig1, "repo1_r1"));
        PartialConfig partialConfigInRepo2 = PartialConfigMother.withPipeline("pipeline_in_repo2", new RepoConfigOrigin(repoConfig2, "repo2_r1"));
        cruiseConfig.merge(asList(partialConfigInRepo1, partialConfigInRepo2), false);
        assertThat(cruiseConfig.getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo1")), is(false));
        assertThat(cruiseConfig.getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo2")), is(true));
    }

    @Test
    public void shouldReturnAListOfPipelineNamesAssociatedWithOneTemplate() {
        ArrayList<CaseInsensitiveString> pipelinesAssociatedWithATemplate = new ArrayList<>();
        pipelinesAssociatedWithATemplate.add(new CaseInsensitiveString("p1"));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");

        assertThat(cruiseConfig.pipelinesAssociatedWithTemplate(new CaseInsensitiveString("t1")), is(pipelinesAssociatedWithATemplate));
    }

    @Test
    public void shouldReturnNullForAssociatedPipelineNamesWhenTemplateNameIsBlank() {
        ArrayList<CaseInsensitiveString> pipelinesAssociatedWithATemplate = new ArrayList<>();
        pipelinesAssociatedWithATemplate.add(new CaseInsensitiveString("p1"));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");

        assertThat(cruiseConfig.pipelinesAssociatedWithTemplate(new CaseInsensitiveString("")), is(new ArrayList<CaseInsensitiveString>()));
    }

    @Test
    public void shouldReturnAnEmptyListForPipelinesIfTemplateNameIsNull() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();

        assertThat(cruiseConfig.pipelinesAssociatedWithTemplate(null).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAnEmptyListIfThereAreNoPipelinesAssociatedWithGivenTemplate() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();

        assertThat(cruiseConfig.pipelinesAssociatedWithTemplate(new CaseInsensitiveString("non-existent-template")).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAMapOfAllTemplateNamesToPipelinesForAnAdminUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p2", "t2", "s2", "j2");

        HashMap<CaseInsensitiveString, List<CaseInsensitiveString>> map = new HashMap<>();
        List<CaseInsensitiveString> template1Pipelines = Arrays.asList(new CaseInsensitiveString("p1"));
        List<CaseInsensitiveString> template2Pipelines = Arrays.asList(new CaseInsensitiveString("p2"));
        map.put(new CaseInsensitiveString("t1"), template1Pipelines);
        map.put(new CaseInsensitiveString("t2"), template2Pipelines);

        assertThat(cruiseConfig.templatesWithPipelinesForUser("admin", null), is(map));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesToPipelinesMapForTemplateAdmin() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("template-admin");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", new Authorization(new AdminsConfig(new AdminUser(templateAdmin))), StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        HashMap<CaseInsensitiveString, List<CaseInsensitiveString>> map = new HashMap<>();
        map.put(new CaseInsensitiveString("t2"), new ArrayList<>());

        assertThat(cruiseConfig.templatesWithPipelinesForUser(templateAdmin.toString(), null), is(map));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesToPipelinesMapForTemplateViewUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("template-view");
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", new Authorization(new ViewConfig(new AdminUser(templateViewUser))), StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        HashMap<CaseInsensitiveString, List<CaseInsensitiveString>> map = new HashMap<>();
        map.put(new CaseInsensitiveString("t2"), new ArrayList<>());

        assertThat(cruiseConfig.templatesWithPipelinesForUser(templateViewUser.toString(), null), is(map));
    }

    @Test
    public void shouldReturnASubsetOfTemplatesToPipelinesMapForGroupAdmin() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString groupAdmin = new CaseInsensitiveString("template-view");
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, "group", "p1", "s1", "j1");
        PipelineConfigs pipelineConfigs = cruiseConfig.getGroups().get(0);
        pipelineConfigs.setAuthorization(new Authorization(new AdminsConfig(new AdminUser(groupAdmin))));
        PipelineTemplateConfig template2 = PipelineTemplateConfigMother.createTemplate("t2", StageConfigMother.manualStage("foo"));
        cruiseConfig.addTemplate(template2);

        HashMap<CaseInsensitiveString, List<CaseInsensitiveString>> map = new HashMap<>();
        map.put(new CaseInsensitiveString("t2"), new ArrayList<>());

        assertThat(cruiseConfig.templatesWithPipelinesForUser(groupAdmin.toString(), null), is(map));
    }

    @Test
    public void shouldReturnAnEmptyMapForARegularUser() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString regularUser = new CaseInsensitiveString("view");
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");

        assertThat(cruiseConfig.templatesWithPipelinesForUser(regularUser.toString(), null), is(new HashMap<>()));
    }

    private BasicCruiseConfig getCruiseConfigWithSecurityEnabled() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(null, null, false, new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))), null);
        cruiseConfig.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFile(cruiseConfig);
        return cruiseConfig;
    }
}
