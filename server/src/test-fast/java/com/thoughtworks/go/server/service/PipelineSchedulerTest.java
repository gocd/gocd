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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.ScheduleCheckMessageMatcher;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.scheduling.*;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static com.thoughtworks.go.helper.GoConfigMother.configWithPipelines;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class PipelineSchedulerTest {
    private ScheduleCheckQueue queue;
    private PipelineScheduler scheduler;
    private GoConfigService configService;
    private BuildCauseProducerService buildCauseProducerService;

    @BeforeEach
    public void setUp() {
        queue = mock(ScheduleCheckQueue.class);
        configService = mock(GoConfigService.class);
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        SchedulingCheckerService schedulingCheckerService = mock(SchedulingCheckerService.class);
        buildCauseProducerService = mock(BuildCauseProducerService.class);
        ScheduleCheckCompletedTopic topic = mock(ScheduleCheckCompletedTopic.class);
        SchedulingPerformanceLogger schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
        scheduler = new PipelineScheduler(configService, serverHealthService, schedulingCheckerService,
                buildCauseProducerService, queue, topic, schedulingPerformanceLogger);
    }

    @Test
    public void shouldCheckAllPipelines() {
        scheduler.onConfigChange(configWithPipelines("cruise"));
        scheduler.onConfigChange(configWithPipelines("cruise", "mingle"));
        scheduler.checkPipelines();
        verify(queue).post(ScheduleCheckMessageMatcher.matchScheduleCheckMessage("cruise"));
        verify(queue).post(ScheduleCheckMessageMatcher.matchScheduleCheckMessage("mingle"));
    }

    @Test
    public void shouldNotCheckDeletedPipelines() {

        scheduler.onConfigChange(configWithPipelines("cruise", "mingle", "twist"));
        scheduler.onConfigChange(configWithPipelines("cruise"));
        scheduler.checkPipelines();
        verify(queue).post(ScheduleCheckMessageMatcher.matchScheduleCheckMessage("cruise"));
    }

    @Test
    public void shouldNotCheckPipelineWhenChecking() {
        scheduler.onConfigChange(configWithPipelines("cruise"));
        scheduler.checkPipelines();
        scheduler.checkPipelines();
        verify(queue).post(ScheduleCheckMessageMatcher.matchScheduleCheckMessage("cruise"));
    }

    @Test
    public void shouldCheckPipelineWhenItBecomesIdle() {
        scheduler.onConfigChange(configWithPipelines("cruise"));
        scheduler.checkPipelines();
        scheduler.onMessage(new ScheduleCheckCompletedMessage("cruise", 1234));
        scheduler.checkPipelines();
        verify(queue, times(2)).post(ScheduleCheckMessageMatcher.matchScheduleCheckMessage("cruise"));
    }

    @Test
    public void shouldAddErrorIfPipelineisNotFound() throws Exception {
        when(configService.hasPipelineNamed(new CaseInsensitiveString("invalid"))).thenReturn(false);
        OperationResult operationResult = mock(OperationResult.class);
        final HashMap<String, String> revisions = new HashMap<>();
        final HashMap<String, String> environmentVariables = new HashMap<>();
        scheduler.manualProduceBuildCauseAndSave("invalid", Username.ANONYMOUS, new ScheduleOptions(revisions, environmentVariables, new HashMap<>()), operationResult);
        verify(operationResult).notFound("Pipeline 'invalid' not found", "Pipeline 'invalid' not found", HealthStateType.general(
                HealthStateScope.forPipeline("invalid")));
    }

    @Test
    public void shouldReturn404WhenVariableIsNotInConfigScope() {
        when(configService.hasPipelineNamed(new CaseInsensitiveString("blahPipeline"))).thenReturn(true);
        when(configService.hasVariableInScope("blahPipeline", "blahVariable")).thenReturn(false);
        OperationResult operationResult = mock(OperationResult.class);
        final HashMap<String, String> revisions = new HashMap<>();
        scheduler.manualProduceBuildCauseAndSave("blahPipeline", Username.ANONYMOUS, new ScheduleOptions(revisions, Collections.singletonMap("blahVariable", "blahValue"), new HashMap<>()), operationResult);
        //noinspection unchecked
        verifyNoMoreInteractions(buildCauseProducerService);
        verify(operationResult).notFound("Variable 'blahVariable' has not been configured for pipeline 'blahPipeline'", "Variable 'blahVariable' has not been configured for pipeline 'blahPipeline'",
                HealthStateType.general(HealthStateScope.forPipeline("blahPipeline")));
    }

    @Test
    public void shouldSchedulePipelineWithEnvironmentVariableOverrides() {
        PipelineConfig pipelineConfig = configWithPipelines("blahPipeline").pipelineConfigByName(new CaseInsensitiveString("blahPipeline"));
        when(configService.pipelineConfigNamed(new CaseInsensitiveString("blahPipeline"))).thenReturn(pipelineConfig);
        when(configService.hasPipelineNamed(new CaseInsensitiveString("blahPipeline"))).thenReturn(true);
        when(configService.hasVariableInScope("blahPipeline", "blahVariable")).thenReturn(true);
        OperationResult operationResult = mock(OperationResult.class);
        Map<String, String> variables = Collections.singletonMap("blahVariable", "blahValue");
        final HashMap<String, String> revisions = new HashMap<>();
        scheduler.manualProduceBuildCauseAndSave("blahPipeline", Username.ANONYMOUS, new ScheduleOptions(revisions, variables, new HashMap<>()), operationResult);
        //noinspection unchecked
        verify(buildCauseProducerService).manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(), new ScheduleOptions(new HashMap<>(), variables, new HashMap<>()),
                operationResult);
    }

    @Test
    public void shouldUpdateOperationResultTo404WhenAnInvalidMaterialIsSpecified() throws Exception {
        when(configService.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        when(configService.findMaterial(new CaseInsensitiveString("pipeline"), "invalid-material")).thenReturn(null);
        HttpOperationResult result = new HttpOperationResult();
        final HashMap<String, String> environmentVariables = new HashMap<>();
        scheduler.manualProduceBuildCauseAndSave("pipeline", Username.ANONYMOUS, new ScheduleOptions(Collections.singletonMap("invalid-material", "blah-revision"), environmentVariables, new HashMap<>()), result);
        assertThat(result.httpCode(), is(404));
        assertThat(result.message(), is("material with fingerprint [invalid-material] not found in pipeline [pipeline]"));
    }

    @Test
    public void shouldNotAcceptEmptyRevision() throws Exception {
        when(configService.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        MaterialConfig materialConfig = mock(MaterialConfig.class);
        when(configService.findMaterial(new CaseInsensitiveString("pipeline"), "invalid-material")).thenReturn(materialConfig);

        HttpOperationResult result = new HttpOperationResult();
        final HashMap<String, String> environmentVariables = new HashMap<>();
        scheduler.manualProduceBuildCauseAndSave("pipeline", Username.ANONYMOUS, new ScheduleOptions(Collections.singletonMap("invalid-material", ""), environmentVariables, new HashMap<>()), result);
        assertThat(result.httpCode(), is(406));
        assertThat(result.message(), is("material with fingerprint [invalid-material] has empty revision"));
    }

    @Test
    public void shouldAddPipelineConfigToPipelinesOnPipelineConfigChanged() {
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        PipelineConfig newPipeline = mock(PipelineConfig.class);
        String pipelineName = "newly-added-pipeline";
        ArrayList<PipelineConfig> pipelineConfigs = new ArrayList<>();
        pipelineConfigs.add(newPipeline);
        when(configService.getAllPipelineConfigs()).thenReturn(pipelineConfigs);
        doNothing().when(configService).register(captor.capture());
        scheduler.initialize();
        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.contains(scheduler), is(true));
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> entityConfigChangedListener = (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);


        when(newPipeline.name()).thenReturn(new CaseInsensitiveString(pipelineName));
        entityConfigChangedListener.onEntityConfigChange(newPipeline);
        scheduler.checkPipelines();
        verify(queue, times(1)).post(ScheduleCheckMessageMatcher.matchScheduleCheckMessage(pipelineName));
    }

    @Test
    public void shouldRemovePipelineConfigFromPipelinesOnPipelineConfigDeletion() {
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        when(configService.getAllPipelineConfigs()).thenReturn(new ArrayList<>());
        doNothing().when(configService).register(captor.capture());
        scheduler.initialize();
        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.contains(scheduler), is(true));
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> entityConfigChangedListener = (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);

        PipelineConfig newPipeline = mock(PipelineConfig.class);
        String pipelineName = "deleted-pipeline";
        when(newPipeline.name()).thenReturn(new CaseInsensitiveString(pipelineName));
        entityConfigChangedListener.onEntityConfigChange(newPipeline);
        scheduler.checkPipelines();
        verify(queue, times(0)).post(ScheduleCheckMessageMatcher.matchScheduleCheckMessage(pipelineName));
    }

    @Test
    public void shouldRemovePipelinesOnConfigRepoDeletion() {
        //setup 2 config repos
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ConfigRepoConfig repoConfig1 = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url1"), "plugin", "id1");
        ConfigRepoConfig repoConfig2 = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url2"), "plugin", "id2");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig1, repoConfig2));
        PartialConfig partialConfigInRepo1 = PartialConfigMother.withPipeline("pipeline_in_repo1", new RepoConfigOrigin(repoConfig1, "repo1_r1"));
        PartialConfig partialConfigInRepo2 = PartialConfigMother.withPipeline("pipeline_in_repo2", new RepoConfigOrigin(repoConfig2, "repo2_r1"));
        cruiseConfig.merge(asList(partialConfigInRepo1, partialConfigInRepo2), false);

        when(configService.cruiseConfig()).thenReturn(cruiseConfig);
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(configService).register(captor.capture());
        scheduler.initialize();
        List<ConfigChangedListener> listeners = captor.getAllValues();
        listeners.get(0).onConfigChange(cruiseConfig);
        assertThat(listeners.contains(scheduler), is(true));
        assertThat(listeners.get(2) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<ConfigRepoConfig> entityConfigChangedListener = (EntityConfigChangedListener<ConfigRepoConfig>) listeners.get(2);

        //both should get scheduled
        scheduler.checkPipelines();
        verify(queue, times(2)).post(any(ScheduleCheckMessage.class));

        //reset
        scheduler.onMessage(new ScheduleCheckCompletedMessage("pipeline_in_repo1", 0));
        scheduler.onMessage(new ScheduleCheckCompletedMessage("pipeline_in_repo2", 0));
        reset(queue);

        //remove one config repo
        BasicCruiseConfig updatedConfig = new BasicCruiseConfig();
        updatedConfig.setConfigRepos(new ConfigReposConfig(repoConfig1, repoConfig2));
        updatedConfig.merge(Collections.singletonList(partialConfigInRepo1), false);
        when(configService.cruiseConfig()).thenReturn(updatedConfig);
        entityConfigChangedListener.onEntityConfigChange(null);

        //removed pipeline should not get scheduled
        scheduler.checkPipelines();
        verify(queue, times(1)).post(any(ScheduleCheckMessage.class));
    }
}
