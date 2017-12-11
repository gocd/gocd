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

package com.thoughtworks.go.server.service;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
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
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PipelineConfigServiceTest {

    private PipelineConfigService pipelineConfigService;
    private CruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private PluggableTaskService pluggableTaskService;

    @Before
    public void setUp() throws Exception {
        PipelineConfigs configs = createGroup("group", "pipeline", "in_env");
        downstream(configs);
        cruiseConfig = new BasicCruiseConfig(configs);
        cruiseConfig.addEnvironment(environment("foo", "in_env"));
        PipelineConfig remotePipeline = PipelineConfigMother.pipelineConfig("remote");
        remotePipeline.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(new GitMaterialConfig("url"), "plugin"), "1234"));
        cruiseConfig.addPipeline("group", remotePipeline);

        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        pluggableTaskService = mock(PluggableTaskService.class);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(goConfigService.getMergedConfigForEditing()).thenReturn(cruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(cruiseConfig.getAllPipelineConfigs());
        pipelineConfigService = new PipelineConfigService(goConfigService, securityService, pluggableTaskService, null);
    }

    @Test
    public void shouldBeAbleToGetTheCanDeleteStatusOfAllPipelines() {
        Map<CaseInsensitiveString, CanDeleteResult> pipelineToCanDeleteIt = pipelineConfigService.canDeletePipelines();

        assertThat(pipelineToCanDeleteIt.size(), is(4));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("down")), is(new CanDeleteResult(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("in_env")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_IN_ENVIRONMENT", new CaseInsensitiveString("in_env"), new CaseInsensitiveString("foo")))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("pipeline")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS", new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("down")))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("remote")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_REMOTE_PIPELINE", new CaseInsensitiveString("remote"), "url at 1234"))));
    }

    @Test
    public void shouldGetPipelineConfigBasedOnName() {
        String pipelineName = "pipeline";
        PipelineConfig pipeline = pipelineConfigService.getPipelineConfig(pipelineName);
        assertThat(pipeline, is(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName))));
    }

    private void downstream(PipelineConfigs configs) {
        PipelineConfig down = PipelineConfigMother.pipelineConfig("down");
        down.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("mingle")));
        configs.add(down);
    }

    @Test
    public void shouldGetAllViewableOrOperatablePipelineConfigs() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("P1");
        PipelineConfig p2 = PipelineConfigMother.pipelineConfig("P2");
        PipelineConfig p3 = PipelineConfigMother.pipelineConfig("P3");
        Username username = new Username(new CaseInsensitiveString("user"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getGroups()).thenReturn(new PipelineGroups(new BasicPipelineConfigs("group1", null, p1),
                new BasicPipelineConfigs("group2", null, p2),
                new BasicPipelineConfigs("group3", null, p3)));

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(false);
        when(securityService.hasOperatePermissionForGroup(username.getUsername(), "group2")).thenReturn(false);

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group3")).thenReturn(false);
        when(securityService.hasOperatePermissionForGroup(username.getUsername(), "group3")).thenReturn(true);

        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableOrOperatableGroupsFor(username);

        assertThat(pipelineConfigs.size(), is(2));
        assertThat(pipelineConfigs.get(0).getGroup(), is("group1"));
        assertThat(pipelineConfigs.get(1).getGroup(), is("group3"));
    }

    @Test
    public void shouldReturnListOfPipelinesWhichAreNotCreatedFromTemplate() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("P1");
        PipelineConfig p2 = PipelineConfigMother.pipelineConfigWithTemplate("P2", "template1");
        Username username = new Username(new CaseInsensitiveString("user"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getGroups()).thenReturn(new PipelineGroups(new BasicPipelineConfigs("group1", null, p1),
                new BasicPipelineConfigs("group2", null, p2)));

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(true);

        List<String> pipelineList = pipelineConfigService.pipelinesNotFromTemplate(username);
        assertThat(pipelineList.size(), is(1));
        assertThat(pipelineList.get(0), is("P1"));
    }

    @Test
    public void shouldGetAllViewablePipelineConfigs() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("P1");
        PipelineConfig p2 = PipelineConfigMother.pipelineConfig("P2");
        Username username = new Username(new CaseInsensitiveString("user"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getGroups()).thenReturn(new PipelineGroups(new BasicPipelineConfigs("group1", null, p1),
                new BasicPipelineConfigs("group2", null, p2)));

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(false);


        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableOrOperatableGroupsFor(username);

        assertThat(pipelineConfigs.size(), is(1));
        assertThat(pipelineConfigs.get(0).getGroup(), is("group1"));
    }

    @Test
    public void updatePipelineConfigShouldValidateAllPluggableTasks() {
        PluggableTask xUnit = mock(PluggableTask.class);
        PluggableTask docker = mock(PluggableTask.class);

        JobConfig job1 = JobConfigMother.job();
        JobConfig job2 = JobConfigMother.job();

        job1.addTask(xUnit);
        job2.addTask(docker);

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        pipelineConfigService.updatePipelineConfig(null, pipeline, null, null);

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

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
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
        mergedCruiseConfig.addPipeline("default", PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString()));
        when(goConfigService.cruiseConfig()).thenReturn(mergedCruiseConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(mergedCruiseConfig.getAllPipelineConfigs());

        assertThat(pipelineConfigService.totalPipelinesCount(), is(mergedCruiseConfig.allPipelines().size()));
    }
}
