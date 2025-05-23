/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.presentation.CanDeleteResult;
import com.thoughtworks.go.server.service.tasks.PluggableTaskService;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PipelineConfigServiceTest {

    private PipelineConfigService pipelineConfigService;
    private CruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private PluggableTaskService pluggableTaskService;
    private ExternalArtifactsService externalArtifactsService;

    @BeforeEach
    public void setUp() {
        PipelineConfigs configs = createGroup("group", "pipeline", "in_env");
        downstream(configs);
        cruiseConfig = new BasicCruiseConfig(configs);
        cruiseConfig.addEnvironment(environment("foo", "in_env"));
        PipelineConfig remotePipeline = PipelineConfigMother.pipelineConfig("remote");
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

        assertThat(pipelineToCanDeleteIt.size()).isEqualTo(4);
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("down"))).isEqualTo(new CanDeleteResult(true, "Delete this pipeline."));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("in_env"))).isEqualTo(new CanDeleteResult(false, "Cannot delete pipeline 'in_env' as it is present in environment 'foo'."));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("pipeline"))).isEqualTo(new CanDeleteResult(false, "Cannot delete pipeline 'pipeline' as pipeline 'down' depends on it."));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("remote"))).isEqualTo(new CanDeleteResult(false, "Cannot delete pipeline 'remote' defined in configuration repository 'url at revision 1234'."));
    }

    @Test
    public void shouldGetPipelineConfigBasedOnName() {
        String pipelineName = "pipeline";
        PipelineConfig pipeline = pipelineConfigService.getPipelineConfig(pipelineName);
        assertThat(pipeline).isEqualTo(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName)));
    }

    private void downstream(PipelineConfigs configs) {
        PipelineConfig down = PipelineConfigMother.pipelineConfig("down");
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

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
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

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("P1", new StageConfig(new CaseInsensitiveString("S1"), new JobConfigs(job1)),
                new StageConfig(new CaseInsensitiveString("S2"), new JobConfigs(job2)));

        pipelineConfigService.createPipelineConfig(null, pipeline, null, null);

        verify(pluggableTaskService).isValid(xUnit);
        verify(pluggableTaskService).isValid(docker);
    }

    @Test
    public void shouldGetPipelinesCount() {
        assertThat(pipelineConfigService.totalPipelinesCount()).isEqualTo(this.cruiseConfig.allPipelines().size());
    }

    @Test
    public void pipelineCountShouldIncludeConfigRepoPipelinesAsWell() {
        CruiseConfig mergedCruiseConfig = GoConfigMother.deepClone(cruiseConfig);
        ReflectionUtil.setField(mergedCruiseConfig, "allPipelineConfigs", null);
        mergedCruiseConfig.addPipeline("default", PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString()));
        when(goConfigService.cruiseConfig()).thenReturn(mergedCruiseConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(mergedCruiseConfig.getAllPipelineConfigs());

        assertThat(pipelineConfigService.totalPipelinesCount()).isEqualTo(mergedCruiseConfig.allPipelines().size());
    }

    @Test
    public void shouldGetAllViewableMergePipelineConfigs() {
        CruiseConfig mergedCruiseConfig = GoConfigMother.deepClone(cruiseConfig);
        ReflectionUtil.setField(mergedCruiseConfig, "allPipelineConfigs", null);
        mergedCruiseConfig.addPipeline("group1", PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString()));
        mergedCruiseConfig.addPipeline("group2", PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString()));
        mergedCruiseConfig.addPipeline("group3", PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString()));

        when(goConfigService.getMergedConfigForEditing()).thenReturn(mergedCruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(mergedCruiseConfig.getAllPipelineConfigs());

        Username username = new Username("user1");

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group3")).thenReturn(false);

        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableGroupsForUserIncludingConfigRepos(username);

        assertThat(pipelineConfigs.size()).isEqualTo(2);
        assertThat(pipelineConfigs.get(0).getGroup()).isEqualTo("group2");
        assertThat(pipelineConfigs.get(1).getGroup()).isEqualTo("group1");
    }


    @Test
    public void shouldGetAllViewableGroups() {
        CruiseConfig cruiseConfig = GoConfigMother.deepClone(this.cruiseConfig);
        ReflectionUtil.setField(cruiseConfig, "allPipelineConfigs", null);
        cruiseConfig.addPipeline("group1", PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString()));
        cruiseConfig.addPipeline("group2", PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString()));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(cruiseConfig.getAllPipelineConfigs());

        Username username = new Username("user1");

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(false);

        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableGroupsFor(username);

        assertThat(pipelineConfigs.size()).isEqualTo(1);
        assertThat(pipelineConfigs.get(0).getGroup()).isEqualTo("group1");
    }
}
