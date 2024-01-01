/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.agent;

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.util.LogFixture;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@MockitoSettings
class AgentWorkRetrievalSchedulerTest {

    @Mock
    private AgentController controller;

    @Mock
    private TaskScheduler taskScheduler;

    @Test
    void shouldLoopForWorkWithExponentialBackoffs() throws InterruptedException {
        AgentWorkRetrievalScheduler scheduler = createSchedulerForIterations(exponentialBackOffTwoToTen(), 10);

        when(controller.performWork())
            .thenReturn(WorkAttempt.FAILED)
            .thenReturn(WorkAttempt.FAILED)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.OK)
            .thenReturn(WorkAttempt.OK)
            .thenReturn(WorkAttempt.NOTHING_TO_DO);

        try (LogFixture logging = LogFixture.logFixtureFor(AgentWorkRetrievalScheduler.class, Level.DEBUG)) {
            Thread runner = new Thread(scheduler);

            runner.start();
            runner.join();

            verify(controller, times(10)).performWork();

            assertThat(logging.getRawMessages().stream().filter(x -> x.startsWith("[Agent Loop] Waiting")))
                .containsExactly(
                    "[Agent Loop] Waiting 2 ms before retrieving next work.", // Initial delay
                    "[Agent Loop] Waiting 4 ms before retrieving next work.", // Failed 1
                    "[Agent Loop] Waiting 8 ms before retrieving next work.", // Failed 2
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do 1 (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do 2 (at maximum)
                    "[Agent Loop] Waiting 2 ms before retrieving next work.", // After OK - reset
                    "[Agent Loop] Waiting 2 ms before retrieving next work.", // After OK - reset
                    "[Agent Loop] Waiting 4 ms before retrieving next work.", // Nothing to do
                    "[Agent Loop] Waiting 8 ms before retrieving next work.", // Nothing to do
                    "[Agent Loop] Waiting 10 ms before retrieving next work." // Nothing to do
                );
        }
    }

    @Test
    void shouldLoopForWorkWithInstantMaxBackoff() throws InterruptedException {
        AgentWorkRetrievalScheduler scheduler = createSchedulerForIterations(instantBackOffTwoToTen(), 9);

        when(controller.performWork())
            .thenReturn(WorkAttempt.FAILED)
            .thenReturn(WorkAttempt.FAILED)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.OK)
            .thenReturn(WorkAttempt.OK)
            .thenReturn(WorkAttempt.NOTHING_TO_DO);

        try (LogFixture logging = LogFixture.logFixtureFor(AgentWorkRetrievalScheduler.class, Level.DEBUG)) {
            Thread runner = new Thread(scheduler);

            runner.start();
            runner.join();

            verify(controller, times(9)).performWork();

            assertThat(logging.getRawMessages().stream().filter(x -> x.startsWith("[Agent Loop] Waiting")))
                .containsExactly(
                    "[Agent Loop] Waiting 2 ms before retrieving next work.", // Initial delay
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Failed 1 (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Failed 2 (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do 1 (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do 2 (at maximum)
                    "[Agent Loop] Waiting 2 ms before retrieving next work.", // After OK - reset
                    "[Agent Loop] Waiting 2 ms before retrieving next work.", // After OK - reset
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work." // Nothing to do (at maximum)
                );
        }
    }

    private AgentWorkRetrievalScheduler createSchedulerForIterations(final ExponentialBackOff backoffStrategy, final int numIterations) {
        return new AgentWorkRetrievalScheduler(controller, backoffStrategy, taskScheduler) {
            int iterations;

            @Override
            void waitFor(long waitMillis) {
                if (++iterations >= numIterations) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    private static ExponentialBackOff exponentialBackOffTwoToTen() {
        ExponentialBackOff backoff = new ExponentialBackOff();
        backoff.setInitialInterval(2);
        backoff.setMultiplier(2);
        backoff.setMaxInterval(10);
        return backoff;
    }

    private static ExponentialBackOff instantBackOffTwoToTen() {
        ExponentialBackOff backoff = new ExponentialBackOff();
        backoff.setInitialInterval(2);
        backoff.setMultiplier(10);
        backoff.setMaxInterval(10);
        return backoff;
    }
}