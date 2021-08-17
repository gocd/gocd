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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.listener.SecurityConfigChangeListener;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CcTrayActivityListenerTest {
    private GoConfigService goConfigService;

    @BeforeEach
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
    }

    @Test
    public void shouldRegisterSelfForConfigChangeHandlingOnInitialization() throws Exception {
        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, null, null, null);

        listener.initialize();

        verify(goConfigService).register(listener);
    }

    @Test
    public void onIntializationAndStartOfDaemon_ShouldRegisterAListener_WhichInvokesJobChangeHandler_WhenJobStatusChanges() throws Exception {
        JobInstance aJob = JobInstanceMother.cancelled("job1");
        CcTrayJobStatusChangeHandler handler = mock(CcTrayJobStatusChangeHandler.class);
        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, handler, null, null);

        listener.initialize();
        listener.startDaemon();
        listener.jobStatusChanged(aJob);
        waitForProcessingToHappen();

        verify(handler).call(aJob);
    }

    @Test
    public void onIntializationAndStartOfDaemon_ShouldRegisterAListener_WhichInvokesStageChangeHandler_WhenStageStatusChanges() throws Exception {
        Stage aStage = StageMother.custom("stage1");
        CcTrayStageStatusChangeHandler handler = mock(CcTrayStageStatusChangeHandler.class);
        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, null, handler, null);

        listener.initialize();
        listener.startDaemon();
        listener.stageStatusChanged(aStage);
        waitForProcessingToHappen();

        verify(handler).call(aStage);
    }

    @Test
    public void onIntializationAndStartOfDaemon_ShouldRegisterAListener_WhichInvokesConfigChangeHandler_WhenConfigChanges() throws Exception {
        CruiseConfig aConfig = GoConfigMother.defaultCruiseConfig();
        CcTrayConfigChangeHandler handler = mock(CcTrayConfigChangeHandler.class);
        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, null, null, handler);

        listener.initialize();
        listener.startDaemon();
        listener.onConfigChange(aConfig);
        waitForProcessingToHappen();

        verify(handler).call(aConfig);
    }

    @Test
    public void postIntializationAndStartOfDaemon_WhenPipelineConfigChanges_ShouldInvokeConfigChangeHandler() throws InterruptedException {
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        CaseInsensitiveString p1 = new CaseInsensitiveString("p1");

        when(pipelineConfig.name()).thenReturn(p1);
        CcTrayConfigChangeHandler ccTrayConfigChangeHandler = mock(CcTrayConfigChangeHandler.class);
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());

        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, mock(CcTrayJobStatusChangeHandler.class),  mock(CcTrayStageStatusChangeHandler.class), ccTrayConfigChangeHandler);
        listener.initialize();
        listener.startDaemon();

        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener = (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);

        pipelineConfigChangeListener.onEntityConfigChange(pipelineConfig);
        waitForProcessingToHappen();

        verify(ccTrayConfigChangeHandler).call(pipelineConfig);
    }

    @Test
    public void shouldInvokeConfigChangeHandlerWhenSecurityConfigChanges() throws InterruptedException {
        CcTrayConfigChangeHandler ccTrayConfigChangeHandler = mock(CcTrayConfigChangeHandler.class);
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);

        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, mock(CcTrayJobStatusChangeHandler.class), mock(CcTrayStageStatusChangeHandler.class), ccTrayConfigChangeHandler);

        listener.initialize();
        listener.startDaemon();

        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.get(2) instanceof SecurityConfigChangeListener, is(true));
        SecurityConfigChangeListener securityConfigChangeListener = (SecurityConfigChangeListener) listeners.get(2);

        securityConfigChangeListener.onEntityConfigChange(new PluginRoleConfig());
        waitForProcessingToHappen();

        verify(ccTrayConfigChangeHandler).call(cruiseConfig);
    }

    private void waitForProcessingToHappen() throws InterruptedException {
        Thread.sleep(1000); /* Prevent potential race, of queue not being processed. Being a little lazy. :( */
    }
}
