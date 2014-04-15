/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;

@Ignore("This reproduces an impossible scenario. We now have the BuildCauseProducer behind a single thread which means that this is impossible.")
public class BuildCauseProducerServiceRandomFailureTest {

    //This method has random failure due to the transactional problem, cannot figure out another way to test it.
    @Test
    public void shouldNotFail() throws InitializationError {
        RunSingleMethodRunner unit4ClassRunner = new RunSingleMethodRunner(PipelineSchedulerIntegrationTest.class);

        RunNotifier notifier = new RunNotifier();
        final List<Failure> failures = new ArrayList<Failure>();
        final List<Description> descriptiones = new ArrayList<Description>();
        notifier.addListener(new RunListener() {
            public void testStarted(Description description) throws Exception {
            }
            public void testFailure(Failure failure) throws Exception {
                failures.add(failure);
            }
            public void testFinished(Description description) throws Exception {
                descriptiones.add(description);
            }
        });
        for (int i = 0; i < 10; i++) {
            unit4ClassRunner.run(notifier);
        }

        assertThat(descriptiones.size(), is(10));
        assertThat(failures, is(eq((List) new ArrayList<Failure>())));
    }

    class RunSingleMethodRunner extends SpringJUnit4ClassRunner {
        public RunSingleMethodRunner(Class<?> aClass) throws org.junit.runners.model.InitializationError {
            super(aClass);
        }

        @Override
        protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
            return !frameworkMethod.getMethod().getName().equals("shouldThrowExceptionIfOtherStageIsRunningInTheSamePipeline");
        }
    }
}