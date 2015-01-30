/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CcTrayActivityListenerTest {
    StubCcTrayJobStatusChangeHandler jobStatusChangeHandler;
    StubCcTrayStageStatusChangeHandler stageStatusChangeHandler;
    StubCcTrayConfigChangeHandler configChangeHandler;

    @Before
    public void setUp() throws Exception {
        jobStatusChangeHandler = new StubCcTrayJobStatusChangeHandler();
        stageStatusChangeHandler = new StubCcTrayStageStatusChangeHandler();
        configChangeHandler = new StubCcTrayConfigChangeHandler();
    }

    @Test
    public void shouldMultiplexEventsFromDifferentThreadsOnToHandlersOnASingleThread() throws Exception {
        CcTrayActivityListener listener = new CcTrayActivityListener(jobStatusChangeHandler, stageStatusChangeHandler, configChangeHandler);
        listener.startQueueProcessor();

        Thread t1 = callJobStatusChangeInNewThread(listener);
        Thread t2 = callStageStatusChangeInNewThread(listener);
        Thread t3 = callConfigChangeInNewThread(listener);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        Thread.sleep(1000); /* Prevent potential race, of queue not being processed. Being a little lazy. */

        assertThat(jobStatusChangeHandler.threadOfCall, is(not(nullValue())));
        assertThat(jobStatusChangeHandler.threadOfCall, is(not(Thread.currentThread())));
        assertThat(jobStatusChangeHandler.threadOfCall, is(not(t1)));

        assertThat(jobStatusChangeHandler.threadOfCall, is(stageStatusChangeHandler.threadOfCall));
        assertThat(stageStatusChangeHandler.threadOfCall, is(configChangeHandler.threadOfCall));
    }

    @Test
    public void shouldNotAllowTheQueueProcessorToBeStartedMultipleTimes() throws Exception {
        CcTrayActivityListener listener = new CcTrayActivityListener(jobStatusChangeHandler, stageStatusChangeHandler, configChangeHandler);
        listener.startQueueProcessor();

        try {
            listener.startQueueProcessor();
            fail("Should have failed to start queue processor a second time.");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Cannot start queue processor multiple times."));
        }
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

    private class StubCcTrayStageStatusChangeHandler extends CCTrayStageStatusChangeHandler {
        private Thread threadOfCall;

        @Override
        public void call(Stage stage) {
            threadOfCall = Thread.currentThread();
        }
    }

    private class StubCcTrayConfigChangeHandler extends CCTrayConfigChangeHandler {
        private Thread threadOfCall;

        @Override
        public void call(CruiseConfig config) {
            threadOfCall = Thread.currentThread();
        }
    }
}