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

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.LogFixture;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CcTrayActivityListenerTest {
    private StubCcTrayJobStatusChangeHandler jobStatusChangeHandler;
    private StubCcTrayStageStatusChangeHandler stageStatusChangeHandler;
    private StubCcTrayConfigChangeHandler configChangeHandler;
    private GoConfigService goConfigService;
    private LogFixture logFixture;

    @Before
    public void setUp() throws Exception {
        jobStatusChangeHandler = new StubCcTrayJobStatusChangeHandler();
        stageStatusChangeHandler = new StubCcTrayStageStatusChangeHandler();
        configChangeHandler = new StubCcTrayConfigChangeHandler();
        goConfigService = mock(GoConfigService.class);
        logFixture = LogFixture.startListening(Level.WARN);
    }

    @After
    public void tearDown() throws Exception {
        logFixture.stopListening();
    }

    @Test
    public void shouldRegisterSelfForConfigChangeHandlingOnInitialization() throws Exception {
        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, jobStatusChangeHandler, stageStatusChangeHandler, configChangeHandler);

        listener.initialize();

        verify(goConfigService).register(listener);
    }

    @Test
    public void shouldMultiplexEventsFromDifferentThreadsOnToHandlersOnASingleThread() throws Exception {
        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, jobStatusChangeHandler, stageStatusChangeHandler, configChangeHandler);
        listener.initialize();

        Thread t1 = callJobStatusChangeInNewThread(listener);
        Thread t2 = callStageStatusChangeInNewThread(listener);
        Thread t3 = callConfigChangeInNewThread(listener);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        waitForProcessingToHappen();

        assertThat(jobStatusChangeHandler.threadOfCall, is(not(nullValue())));
        assertThat(jobStatusChangeHandler.threadOfCall, is(not(Thread.currentThread())));
        assertThat(jobStatusChangeHandler.threadOfCall, is(not(t1)));

        assertThat(jobStatusChangeHandler.threadOfCall, is(stageStatusChangeHandler.threadOfCall));
        assertThat(stageStatusChangeHandler.threadOfCall, is(configChangeHandler.threadOfCall));
    }

    @Test
    public void shouldNotAllowTheQueueProcessorToBeStartedMultipleTimes() throws Exception {
        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, jobStatusChangeHandler, stageStatusChangeHandler, configChangeHandler);
        listener.initialize();

        try {
            listener.initialize();
            fail("Should have failed to start queue processor a second time.");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Cannot start queue processor multiple times."));
        }
    }

    @Test
    public void shouldLogAndIgnoreAnyChangesWhichCannotBeHandled() throws Exception {
        CcTrayStageStatusChangeHandler normalStageStatusChangeHandler = mock(CcTrayStageStatusChangeHandler.class);
        CcTrayJobStatusChangeHandler failingJobStatusChangeHandler = mock(CcTrayJobStatusChangeHandler.class);
        doThrow(new RuntimeException("Ouch. Failed.")).when(failingJobStatusChangeHandler).call(any(JobInstance.class));

        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, failingJobStatusChangeHandler, normalStageStatusChangeHandler, configChangeHandler);
        listener.initialize();
        listener.jobStatusChanged(JobInstanceMother.passed("some-job-this-should-fail"));
        listener.stageStatusChanged(StageMother.unrunStage("some-stage"));

        waitForProcessingToHappen();

        assertThat(logFixture.contains(Level.WARN, "Failed to handle action in CCTray queue"), is(true));
        verify(normalStageStatusChangeHandler).call(StageMother.unrunStage("some-stage"));
    }

    @Test
    public void shouldInvokeConfigChangeHandlerWhenPipelineConfigChanges() throws InterruptedException {
        PipelineConfig pipelineConfig=mock(PipelineConfig.class);
        CaseInsensitiveString p1 = new CaseInsensitiveString("p1");
        when(pipelineConfig.name()).thenReturn(p1);
        CcTrayConfigChangeHandler ccTrayConfigChangeHandler = mock(CcTrayConfigChangeHandler.class);
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());
        when(goConfigService.findGroupNameByPipeline(p1)).thenReturn("group1");
        CcTrayActivityListener listener = new CcTrayActivityListener(goConfigService, mock(CcTrayJobStatusChangeHandler.class),  mock(CcTrayStageStatusChangeHandler.class), ccTrayConfigChangeHandler);
        listener.initialize();
        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener= (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);

        pipelineConfigChangeListener.onEntityConfigChange(pipelineConfig);
        waitForProcessingToHappen();

        verify(ccTrayConfigChangeHandler).call(pipelineConfig, "group1");
    }

    private void waitForProcessingToHappen() throws InterruptedException {
        Thread.sleep(1000); /* Prevent potential race, of queue not being processed. Being a little lazy. :( */
    }

    private Thread callJobStatusChangeInNewThread(final CcTrayActivityListener listener) {
        return new Thread() {
            @Override
            public void run() {
                listener.jobStatusChanged(JobInstanceMother.passed("some-job"));
            }
        };
    }

    private Thread callStageStatusChangeInNewThread(final CcTrayActivityListener listener) {
        return new Thread() {
            @Override
            public void run() {
                listener.stageStatusChanged(StageMother.unrunStage("some-stage"));
            }
        };
    }

    private Thread callConfigChangeInNewThread(final CcTrayActivityListener listener) {
        return new Thread() {
            @Override
            public void run() {
                listener.onConfigChange(GoConfigMother.defaultCruiseConfig());
            }
        };
    }

    private class StubCcTrayJobStatusChangeHandler extends CcTrayJobStatusChangeHandler {
        private Thread threadOfCall;

        public StubCcTrayJobStatusChangeHandler() {
            super(null);
        }

        @Override
        public void call(JobInstance job) {
            threadOfCall = Thread.currentThread();
        }
    }

    private class StubCcTrayStageStatusChangeHandler extends CcTrayStageStatusChangeHandler {
        private Thread threadOfCall;

        public StubCcTrayStageStatusChangeHandler() {
            super(null, null, null);
        }

        @Override
        public void call(Stage stage) {
            threadOfCall = Thread.currentThread();
        }
    }

    private class StubCcTrayConfigChangeHandler extends CcTrayConfigChangeHandler {
        private Thread threadOfCall;

        public StubCcTrayConfigChangeHandler() {
            super(null, null, null);
        }

        @Override
        public void call(CruiseConfig config) {
            threadOfCall = Thread.currentThread();
        }
    }
}
