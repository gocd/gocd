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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.listener.SecurityConfigChangeListener;
import com.thoughtworks.go.server.domain.PipelineLockStatusChangeListener;
import com.thoughtworks.go.server.domain.PipelinePauseChangeListener;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineLockService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.StageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GoDashboardActivityListenerTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private StageService stageService;
    @Mock
    private PipelinePauseService pipelinePauseService;
    @Mock
    private PipelineLockService pipelineLockService;


    @Test
    public void shouldRegisterSelfForConfigChangeHandlingOnInitialization() throws Exception {
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, null, null, null, null);

        listener.initialize();

        verify(goConfigService).register(listener);
    }

    @Test
    public void onInitializationAndStartOfDaemons_shouldRegisterAListener_WhichCallsStageStatusChangeHandler_ForStageStatusChanges() throws Exception {
        Stage aStage = StageMother.custom("stage1");
        GoDashboardStageStatusChangeHandler handler = mock(GoDashboardStageStatusChangeHandler.class);

        ArgumentCaptor<StageStatusListener> captor = ArgumentCaptor.forClass(StageStatusListener.class);
        doNothing().when(stageService).addStageStatusListener(captor.capture());

        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                handler, null, null, null, null);

        listener.initialize();
        listener.startDaemon();

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
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, handler, null, null, null);

        listener.initialize();
        listener.startDaemon();

        listener.onConfigChange(aConfig);
        waitForProcessingToHappen();

        verify(handler).call(aConfig);
    }

    @Test
    public void onInitializationAndStartOfDaemons_shouldRegisterAListener_WhichCallsConfigChangeHandler_ForPipelineConfigChangeHandling() throws Exception {
        GoDashboardConfigChangeHandler handler = mock(GoDashboardConfigChangeHandler.class);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1");

        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());

        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, handler, null, null, null);
        listener.initialize();
        listener.startDaemon();

        ((EntityConfigChangedListener<PipelineConfig>) captor.getAllValues().get(1)).onEntityConfigChange(pipelineConfig);
        waitForProcessingToHappen();

        verify(handler).call(pipelineConfig);
    }

    @Test
    public void onInitializationAndStartOfDaemons_shouldRegisterAListener_WhichCallsConfigChangeHandler_ForSecurityConfigChangeHandling() throws Exception {
        CruiseConfig aConfig = GoConfigMother.defaultCruiseConfig();
        GoDashboardConfigChangeHandler handler = mock(GoDashboardConfigChangeHandler.class);

        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());
        when(goConfigService.currentCruiseConfig()).thenReturn(aConfig);

        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, handler, null, null, null);

        listener.initialize();
        listener.startDaemon();

        ((SecurityConfigChangeListener) captor.getAllValues().get(2)).onEntityConfigChange(new RoleConfig());
        waitForProcessingToHappen();

        verify(handler).call(aConfig);
    }

    @Test
    public void onInitializationAndStartOfDaemons_shouldRegisterAListener_WhichCallsTemplateConfigChangeHandler_ForTemplateConfigChangeHandling() throws Exception {
        GoDashboardTemplateConfigChangeHandler handler = mock(GoDashboardTemplateConfigChangeHandler.class);
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("t1"));

        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());

        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, null, null, null, handler);
        listener.initialize();
        listener.startDaemon();

        ((EntityConfigChangedListener<PipelineTemplateConfig>) captor.getAllValues().get(3)).onEntityConfigChange(templateConfig);
        waitForProcessingToHappen();

        verify(handler).call(templateConfig);
    }

    @Test
    public void onInitializationAndStartOfDaemons_shouldRegisterAListener_WhichCallsConfigChangeHandler_ForPipelineGroupConfigChangeHandling() throws Exception {
        CruiseConfig aConfig = GoConfigMother.defaultCruiseConfig();
        GoDashboardConfigChangeHandler handler = mock(GoDashboardConfigChangeHandler.class);
        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs();

        when(goConfigService.currentCruiseConfig()).thenReturn(aConfig);
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());

        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, handler, null, null, null);
        listener.initialize();
        listener.startDaemon();

        ((EntityConfigChangedListener<PipelineConfigs>) captor.getAllValues().get(4)).onEntityConfigChange(pipelineConfigs);
        waitForProcessingToHappen();

        verify(handler).call(aConfig);
    }

    @Test
    public void shouldRegisterSelfForPipelineStatusChangeHandlingOnInitialization() throws Exception {
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, null, null, null, null);

        listener.initialize();

        verify(pipelinePauseService).registerListener(listener);
    }

    @Test
    public void shouldInvokePipelinePauseStatusChangeHandlerWhenPipelinePauseEventOccurs() throws Exception {
        GoDashboardPipelinePauseStatusChangeHandler handler = mock(GoDashboardPipelinePauseStatusChangeHandler.class);
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, null, handler, null, null);

        PipelinePauseChangeListener.Event pauseEvent = PipelinePauseChangeListener.Event.pause("pipeline1", Username.valueOf("user1"));

        listener.initialize();
        listener.startDaemon();

        listener.pauseStatusChanged(pauseEvent);
        waitForProcessingToHappen();

        verify(handler).call(pauseEvent);
    }

    @Test
    public void shouldRegisterSelfForPipelineLockStatusChangeHandlingOnInitialization() throws Exception {
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, null, null, null, null);

        listener.initialize();

        verify(pipelineLockService).registerListener(listener);
    }

    @Test
    public void shouldInvokePipelineLockStatusChangeHandlerWhenPipelineLockEventOccurs() throws Exception {
        GoDashboardPipelineLockStatusChangeHandler handler = mock(GoDashboardPipelineLockStatusChangeHandler.class);
        GoDashboardActivityListener listener = new GoDashboardActivityListener(goConfigService, stageService, pipelinePauseService, pipelineLockService,
                null, null, null, handler, null);

        PipelineLockStatusChangeListener.Event lockEvent = PipelineLockStatusChangeListener.Event.lock("pipeline1");

        listener.initialize();
        listener.startDaemon();

        listener.lockStatusChanged(lockEvent);
        waitForProcessingToHappen();

        verify(handler).call(lockEvent);
    }

    private void waitForProcessingToHappen() throws InterruptedException {
        Thread.sleep(1000); /* Prevent potential race, of queue not being processed. Being a little lazy. :( */
    }
}
