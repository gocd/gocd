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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.presentation.CanDeleteResult;
import com.thoughtworks.go.server.service.tasks.PluggableTaskService;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment;
import static com.thoughtworks.go.helper.MaterialConfigsMother.*;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PipelineConfigServiceTest {

    private PipelineConfigService pipelineConfigService;
    private CruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private PluggableTaskService pluggableTaskService;
    private ExternalArtifactsService externalArtifactsService;

    @Before
    public void setUp() throws Exception {
        PipelineConfigs configs = createGroup("group", "pipeline", "in_env");
        downstream(configs);
        cruiseConfig = new BasicCruiseConfig(configs);
        cruiseConfig.addEnvironment(environment("foo", "in_env"));
        PipelineConfig remotePipeline = pipelineConfig("remote");
        remotePipeline.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(git("url"), "plugin", "id"), "1234"));
        cruiseConfig.addPipeline("group", remotePipeline);

        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        pluggableTaskService = mock(PluggableTaskService.class);
        externalArtifactsService = mock(ExternalArtifactsService.class);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(goConfigService.getMergedConfigForEditing()).thenReturn(cruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(cruiseConfig.getAllPipelineConfigs());
        pipelineConfigService = new PipelineConfigService(goConfigService, securityService, pluggableTaskService, null, externalArtifactsService);
    }

    @Test
    public void shouldBeAbleToGetTheCanDeleteStatusOfAllPipelines() {
        Map<CaseInsensitiveString, CanDeleteResult> pipelineToCanDeleteIt = pipelineConfigService.canDeletePipelines();

        assertThat(pipelineToCanDeleteIt.size(), is(4));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("down")), is(new CanDeleteResult(true, "Delete this pipeline.")));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("in_env")), is(new CanDeleteResult(false, "Cannot delete pipeline 'in_env' as it is present in environment 'foo'.")));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("pipeline")), is(new CanDeleteResult(false, "Cannot delete pipeline 'pipeline' as pipeline 'down' depends on it.")));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("remote")), is(new CanDeleteResult(false, "Cannot delete pipeline 'remote' defined in configuration repository 'url at 1234'.")));
    }

    @Test
    public void shouldGetPipelineConfigBasedOnName() {
        String pipelineName = "pipeline";
        PipelineConfig pipeline = pipelineConfigService.getPipelineConfig(pipelineName);
        assertThat(pipeline, is(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName))));
    }

    private void downstream(PipelineConfigs configs) {
        PipelineConfig down = pipelineConfig("down");
        down.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("mingle")));
        configs.add(down);
    }

    @Test
    public void updatePipelineConfigShouldValidateAllPluggableTasks() {
        PluggableTask xUnit = mock(PluggableTask.class);
        PluggableTask docker = mock(PluggableTask.class);

        JobConfig job1 = JobConfigMother.job();
        JobConfig job2 = JobConfigMother.job();

        job1.addTask(xUnit);
        job2.addTask(docker);

        PipelineConfig pipeline = pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        pipelineConfigService.updatePipelineConfig(null, pipeline, "group", null, null);

        verify(pluggableTaskService).isValid(xUnit);
        verify(pluggableTaskService).isValid(docker);
    }

    @Test
    public void createPipelineConfigShouldValidateAllPluggableTasks() {
        PluggableTask xUnit = mock(PluggableTask.class);
        PluggableTask docker = mock(PluggableTask.class);

        JobConfig job1 = JobConfigMother.job();
        JobConfig job2 = JobConfigMother.job();

        job1.addTask(xUnit);
        job2.addTask(docker);

        PipelineConfig pipeline = pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        pipelineConfigService.createPipelineConfig(null, pipeline, null, null);

        verify(pluggableTaskService).isValid(xUnit);
        verify(pluggableTaskService).isValid(docker);
    }

    @Test
    public void shouldGetPipelinesCount() {
        assertThat(pipelineConfigService.totalPipelinesCount(), is(this.cruiseConfig.allPipelines().size()));
    }

    @Test
    public void pipelineCountShouldIncludeConfigRepoPipelinesAsWell() {
        CruiseConfig mergedCruiseConfig = new Cloner().deepClone(cruiseConfig);
        ReflectionUtil.setField(mergedCruiseConfig, "allPipelineConfigs", null);
        mergedCruiseConfig.addPipeline("default", pipelineConfig(UUID.randomUUID().toString()));
        when(goConfigService.cruiseConfig()).thenReturn(mergedCruiseConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(mergedCruiseConfig.getAllPipelineConfigs());

        assertThat(pipelineConfigService.totalPipelinesCount(), is(mergedCruiseConfig.allPipelines().size()));
    }

    @Test
    public void shouldGetAllViewableMergePipelineConfigs() throws Exception {
        CruiseConfig mergedCruiseConfig = new Cloner().deepClone(cruiseConfig);
        ReflectionUtil.setField(mergedCruiseConfig, "allPipelineConfigs", null);
        mergedCruiseConfig.addPipeline("group1", pipelineConfig(UUID.randomUUID().toString()));
        mergedCruiseConfig.addPipeline("group2", pipelineConfig(UUID.randomUUID().toString()));
        mergedCruiseConfig.addPipeline("group3", pipelineConfig(UUID.randomUUID().toString()));

        when(goConfigService.getMergedConfigForEditing()).thenReturn(mergedCruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(mergedCruiseConfig.getAllPipelineConfigs());

        Username username = new Username("user1");

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group3")).thenReturn(false);

        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableGroupsForUserIncludingConfigRepos(username);

        assertThat(pipelineConfigs.size(), is(2));
        assertThat(pipelineConfigs.get(0).getGroup(), is("group2"));
        assertThat(pipelineConfigs.get(1).getGroup(), is("group1"));
    }


    @Test
    public void shouldGetAllViewableGroups() throws Exception {
        CruiseConfig cruiseConfig = new Cloner().deepClone(this.cruiseConfig);
        ReflectionUtil.setField(cruiseConfig, "allPipelineConfigs", null);
        cruiseConfig.addPipeline("group1", pipelineConfig(UUID.randomUUID().toString()));
        cruiseConfig.addPipeline("group2", pipelineConfig(UUID.randomUUID().toString()));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(cruiseConfig.getAllPipelineConfigs());

        Username username = new Username("user1");

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(false);

        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableGroupsFor(username);

        assertThat(pipelineConfigs.size(), is(1));
        assertThat(pipelineConfigs.get(0).getGroup(), is("group1"));
    }

    @Test
    public void onSCMConfigChange_shouldUpdateAllPipelinesWithReferenceToTheSCM() {
        PluggableSCMMaterialConfig pluggableMaterial1 = pluggableSCMMaterialConfig("scm-id1", "key1", "key2");
        PluggableSCMMaterialConfig pluggableMaterial2 = pluggableSCMMaterialConfig("scm-id2", "key1", "key2");

        PipelineConfig pipelineConfig1 = pipelineConfig("pipe1", new MaterialConfigs(git("http://localhost"), pluggableMaterial1));
        PipelineConfig pipelineConfig2 = pipelineConfig("pipe2", new MaterialConfigs(pluggableMaterial1));
        PipelineConfig pipelineConfig3 = pipelineConfig("pipe3", new MaterialConfigs(pluggableMaterial2));

        when(goConfigService.getAllPipelineConfigs()).thenReturn(asList(pipelineConfig1, pipelineConfig2, pipelineConfig3));

        SCM updatedSCM = SCMMother.create("scm-id1", "key3");
        pipelineConfigService.scmConfigChangeListener().onEntityConfigChange(updatedSCM);

        assertThat(pipelineConfig1.materialConfigs().getSCMMaterial().getSCMConfig(), is(updatedSCM));
        assertThat(pipelineConfig2.materialConfigs().getSCMMaterial().getSCMConfig(), is(updatedSCM));
        assertThat(pipelineConfig3.materialConfigs().getSCMMaterial().getSCMConfig(), is(SCMMother.create("scm-id2", "key1", "key2")));
    }

    @Test
    public void onPackageDefinitionChange_shouldUpdateAllPipelinesWithReferenceToThePackage() {
        ConfigurationProperty property1 = ConfigurationPropertyMother.create("key1", "value1");
        ConfigurationProperty property2 = ConfigurationPropertyMother.create("key2", "value2");
        ConfigurationProperty property3 = ConfigurationPropertyMother.create("key3", "value3");

        PackageDefinition packageDefinition1 = PackageDefinitionMother.create("pack-id-1", "pack-1", new Configuration(property1, property2), null);
        PackageDefinition packageDefinition2 = PackageDefinitionMother.create("pack-id-1", "pack-1", new Configuration(property3), null);

        PackageMaterialConfig material1 = new PackageMaterialConfig(new CaseInsensitiveString("material1"), "pack-id-1", packageDefinition1);
        PackageMaterialConfig material2 = new PackageMaterialConfig(new CaseInsensitiveString("material2"), "pack-id-2", packageDefinition2);

        PipelineConfig pipelineConfig1 = pipelineConfig("pipe1", new MaterialConfigs(git("http://localhost"), material1));
        PipelineConfig pipelineConfig2 = pipelineConfig("pipe2", new MaterialConfigs(material1));
        PipelineConfig pipelineConfig3 = pipelineConfig("pipe3", new MaterialConfigs(material2));

        when(goConfigService.getAllPipelineConfigs()).thenReturn(asList(pipelineConfig1, pipelineConfig2, pipelineConfig3));

        PackageDefinition updatedPackage = PackageDefinitionMother.create("pack-id-1", "pack-1", new Configuration(property2), null);

        pipelineConfigService.packageChangeListener().onEntityConfigChange(updatedPackage);

        assertThat(pipelineConfig1.materialConfigs().getPackageMaterial().getPackageDefinition(), is(updatedPackage));
        assertThat(pipelineConfig2.materialConfigs().getPackageMaterial().getPackageDefinition(), is(updatedPackage));
        assertThat(pipelineConfig3.materialConfigs().getPackageMaterial().getPackageDefinition(), is(packageDefinition2));
    }

    @Test
    public void onPackageRepositoryChange_shouldUpdateAllPipelinesWithReferenceToThePackage() {
        ConfigurationProperty property1 = ConfigurationPropertyMother.create("key1", "value1");
        ConfigurationProperty property2 = ConfigurationPropertyMother.create("key2", "value2");
        ConfigurationProperty property3 = ConfigurationPropertyMother.create("key3", "value3");

        PackageRepository packageRepository1 = PackageRepositoryMother.create("repo-1", "repo-1", "plugin1", "0.1", new Configuration(property1, property2));
        PackageRepository packageRepository2 = PackageRepositoryMother.create("repo-2", "repo-2", "plugin1", "0.1", new Configuration(property3));

        PackageDefinition packageDefinition1 = PackageDefinitionMother.create("pack-id-1", "pack-1", new Configuration(), packageRepository1);
        PackageDefinition packageDefinition2 = PackageDefinitionMother.create("pack-id-1", "pack-1", new Configuration(), packageRepository2);

        PackageMaterialConfig material1 = new PackageMaterialConfig(new CaseInsensitiveString("material1"), "pack-id-1", packageDefinition1);
        PackageMaterialConfig material2 = new PackageMaterialConfig(new CaseInsensitiveString("material2"), "pack-id-2", packageDefinition2);

        PipelineConfig pipelineConfig1 = pipelineConfig("pipe1", new MaterialConfigs(git("http://localhost"), material1));
        PipelineConfig pipelineConfig2 = pipelineConfig("pipe2", new MaterialConfigs(material1));
        PipelineConfig pipelineConfig3 = pipelineConfig("pipe3", new MaterialConfigs(material2));

        when(goConfigService.getAllPipelineConfigs()).thenReturn(asList(pipelineConfig1, pipelineConfig2, pipelineConfig3));

        PackageRepository updatedPackageRepository = PackageRepositoryMother.create("repo-1", "repo-1", "plugin1", "0.1", new Configuration(property2));

        pipelineConfigService.packageRespositoryChangeListener().onEntityConfigChange(updatedPackageRepository);

        assertThat(pipelineConfig1.materialConfigs().getPackageMaterial().getPackageDefinition().getRepository(), is(updatedPackageRepository));
        assertThat(pipelineConfig2.materialConfigs().getPackageMaterial().getPackageDefinition().getRepository(), is(updatedPackageRepository));
        assertThat(pipelineConfig3.materialConfigs().getPackageMaterial().getPackageDefinition().getRepository(), is(packageRepository2));
    }
}
