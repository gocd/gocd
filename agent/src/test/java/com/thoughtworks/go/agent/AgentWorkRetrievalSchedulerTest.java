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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.util.LogFixture;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.slf4j.event.Level;
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
        when(controller.performWork())
            .thenReturn(WorkAttempt.FAILED)
            .thenReturn(WorkAttempt.FAILED)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.OK)
            .thenReturn(WorkAttempt.OK)
            .thenReturn(WorkAttempt.NOTHING_TO_DO); // after this for as many iterations as needed

        int numWaits = 10; // failure & nothing
        int expectedWork = numWaits + 2; // +2 for the OK results
        AgentWorkRetrievalScheduler scheduler = createSchedulerForIterations(exponentialBackOffTwoToTen(), numWaits);

        try (LogFixture logging = LogFixture.logFixtureFor(AgentWorkRetrievalScheduler.class, Level.DEBUG)) {
            Thread runner = new Thread(scheduler);

            runner.start();
            runner.join();

            verify(controller, times(expectedWork)).performWork();

            assertThat(logging.getRawMessages().stream().filter(x -> x.endsWith("retrieving next work.")))
                .containsExactly(
                    "[Agent Loop] Waiting 2 ms before retrieving next work.",  // Failed 1
                    "[Agent Loop] Waiting 4 ms before retrieving next work.",  // Failed 2
                    "[Agent Loop] Waiting 8 ms before retrieving next work.",  // Nothing to do 1
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do 2 (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do 3 (at maximum)
                    "[Agent Loop] Immediately retrieving next work.",          // After OK
                    "[Agent Loop] Immediately retrieving next work.",          // After OK 2
                    "[Agent Loop] Waiting 2 ms before retrieving next work.",  // Nothing to do (reset)
                    "[Agent Loop] Waiting 4 ms before retrieving next work.",  // Nothing to do
                    "[Agent Loop] Waiting 8 ms before retrieving next work.",  // Nothing to do
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work."  // Nothing to do (at maximum)
                );
        }
    }

    @Test
    void shouldLoopForWorkWithInstantMaxBackoff() throws InterruptedException {
        when(controller.performWork())
            .thenReturn(WorkAttempt.FAILED)
            .thenReturn(WorkAttempt.FAILED)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.NOTHING_TO_DO)
            .thenReturn(WorkAttempt.OK)
            .thenReturn(WorkAttempt.OK)
            .thenReturn(WorkAttempt.NOTHING_TO_DO); // after this for as many iterations as needed

        int numWaits = 7; // failure & nothing
        int expectedWork = numWaits + 2; // +2 for the OK results
        AgentWorkRetrievalScheduler scheduler = createSchedulerForIterations(instantBackOffTwoToTen(), numWaits);

        try (LogFixture logging = LogFixture.logFixtureFor(AgentWorkRetrievalScheduler.class, Level.DEBUG)) {
            Thread runner = new Thread(scheduler);

            runner.start();
            runner.join();

            verify(controller, times(expectedWork)).performWork();

            assertThat(logging.getRawMessages().stream().filter(x -> x.endsWith("retrieving next work.")))
                .containsExactly(
                    "[Agent Loop] Waiting 2 ms before retrieving next work.",  // Failed 1
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Failed 2 (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do 1 (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do 2 (at maximum)
                    "[Agent Loop] Immediately retrieving next work.",          // After OK
                    "[Agent Loop] Immediately retrieving next work.",          // After OK 2
                    "[Agent Loop] Waiting 2 ms before retrieving next work.",  // Nothing to do (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work.", // Nothing to do (at maximum)
                    "[Agent Loop] Waiting 10 ms before retrieving next work."  // Nothing to do (at maximum)
                );
        }
    }

    private AgentWorkRetrievalScheduler createSchedulerForIterations(final ExponentialBackOff backoffStrategy, final int numWaits) {
        return new AgentWorkRetrievalScheduler(controller, backoffStrategy, taskScheduler) {
            int iterations;

            @Override
            void waitFor(long waitMillis) {
                if (++iterations >= numWaits) {
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