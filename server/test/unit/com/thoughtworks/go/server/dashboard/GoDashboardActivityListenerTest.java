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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.PipelinePauseChangeListener;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.StageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoDashboardActivityListenerTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private StageService stageService;
    @Mock
    private PipelinePauseService pipelinePauseService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldRegisterSelfForConfigChangeHandlingOnInitialization() throws Exception {
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, null, null, null, null);

        listener.initialize();

        verify(goConfigService).register(listener);
    }

    @Test
    public void shouldInvokeJobChangeHandlerWhenJobStatusChanges() throws Exception {
        JobInstance aJob = JobInstanceMother.cancelled("job1");
        GoDashboardJobStatusChangeHandler handler = mock(GoDashboardJobStatusChangeHandler.class);
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, handler, null, null, null);

        listener.initialize();
        listener.jobStatusChanged(aJob);
        waitForProcessingToHappen();

        verify(handler).call(aJob);
    }

    @Test
    public void onInitialization_shouldRegisterAListener_WhichCallsStageStatusChangeHandler_ForStageStatusChanges() throws Exception {
        Stage aStage = StageMother.custom("stage1");
        GoDashboardStageStatusChangeHandler handler = mock(GoDashboardStageStatusChangeHandler.class);

        ArgumentCaptor<StageStatusListener> captor = ArgumentCaptor.forClass(StageStatusListener.class);
        doNothing().when(stageService).addStageStatusListener(captor.capture());

        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, null, handler, null, null);
        listener.initialize();

        StageStatusListener stageStatusListener = captor.getAllValues().get(0);
        stageStatusListener.stageStatusChanged(aStage);
        waitForProcessingToHappen();

        verify(handler).call(aStage);
        verify(stageService).addStageStatusListener(stageStatusListener);
    }

    @Test
    public void shouldInvokeConfigChangeHandlerWhenConfigChanges() throws Exception {
        CruiseConfig aConfig = GoConfigMother.defaultCruiseConfig();
        GoDashboardConfigChangeHandler handler = mock(GoDashboardConfigChangeHandler.class);
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, null, null, handler, null);

        listener.initialize();
        listener.onConfigChange(aConfig);
        waitForProcessingToHappen();

        verify(handler).call(aConfig);
    }

    @Test
    public void onInitialization_shouldRegisterAListener_WhichCallsConfigChangeHandler_ForPipelineConfigChangeHandling() throws Exception {
        GoDashboardConfigChangeHandler handler = mock(GoDashboardConfigChangeHandler.class);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1");

        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());

        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, null, null, handler, null);
        listener.initialize();
        ((EntityConfigChangedListener<PipelineConfig>) captor.getAllValues().get(1)).onEntityConfigChange(pipelineConfig);
        waitForProcessingToHappen();

        verify(handler).call(pipelineConfig);
    }

    @Test
    public void shouldRegisterSelfForPipelineStatusChangeHandlingOnInitialization() throws Exception {
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, null, null, null, null);

        listener.initialize();

        verify(pipelinePauseService).registerListener(listener);
    }

    @Test
    public void shouldInvokePipelinePauseStatusChangeHandlerWhenPipelinePauseEventOccurs() throws Exception {
        GoDashboardPipelinePauseStatusChangeHandler handler = mock(GoDashboardPipelinePauseStatusChangeHandler.class);
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, null, null, null, handler);

        PipelinePauseChangeListener.Event pauseEvent = PipelinePauseChangeListener.Event.pause("pipeline1", Username.valueOf("user1"));

        listener.initialize();
        listener.pauseStatusChanged(pauseEvent);
        waitForProcessingToHappen();

        verify(handler).call(pauseEvent);
    }

    private void waitForProcessingToHappen() throws InterruptedException {
        Thread.sleep(1000); /* Prevent potential race, of queue not being processed. Being a little lazy. :( */
    }
}