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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.util.SystemTimeClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static java.time.Clock.fixed;
import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExponentialBackOffTest {

    private SystemTimeClock systemTimeClock;

    @BeforeEach
    void setUp() {
        systemTimeClock = mock(SystemTimeClock.class);
    }

    @Nested
    class shouldBackOff {
        @Test
        void shouldBackOffIfCurrentTimeIsBefore_DefaultRetryInterval() {
            ExponentialBackOff backOff = new ExponentialBackOff(0);

            assertThat(backOff.backOffResult().shouldBackOff()).isTrue();
        }

        @Test
        void shouldBackOffIfCurrentTimeIsBefore_LastFailureTime_Plus_RetryInterval() {
            Clock tenSecondsFromNow = fixed(now().plusSeconds(10), systemDefault());
            when(systemTimeClock.currentTime())
                    .thenReturn(now())
                    .thenReturn(now(tenSecondsFromNow));

            ExponentialBackOff backOff = new ExponentialBackOff(1.5f, systemTimeClock);

            backOff.failedAgain();

            assertThat(backOff.backOffResult().shouldBackOff()).isTrue();
        }

        @Test
        void shouldNotBackOffIfCurrentTimeIsAfter_LastFailureTime_Plus_RetryInterval() {
            Clock fiveSecondsAgo = fixed(now().minusSeconds(5), systemDefault());
            when(systemTimeClock.currentTime())
                    .thenReturn(now())
                    .thenReturn(now(fiveSecondsAgo));

            ExponentialBackOff backOff = new ExponentialBackOff(0.5f, systemTimeClock);

            backOff.failedAgain();

            assertThat(backOff.backOffResult().shouldBackOff()).isFalse();
        }

        @Test
        void backOffShouldLimitToMaxRetryInterval() {
            Instant firstFailure = now().plusMillis(ExponentialBackOff.MAX_RETRY_INTERVAL_IN_MILLIS);
            Clock oneHourFromNow = fixed(firstFailure, systemDefault());
            when(systemTimeClock.currentTime())
                    .thenReturn(now()) // start
                    .thenReturn(now(oneHourFromNow)); // first failure

            ExponentialBackOff backOff = new ExponentialBackOff(2, systemTimeClock);

            backOff.failedAgain(); // failed after 60 minutes. Next retry would normally be after another 2 * 60 minutes but capped at 60 minutes.

            when(systemTimeClock.currentTime()).thenReturn(now(fixed(firstFailure.plusMillis(ExponentialBackOff.MAX_RETRY_INTERVAL_IN_MILLIS), systemDefault()))); // backoff result
            BackOffResult backOffResult = backOff.backOffResult();
            assertThat(backOffResult.shouldBackOff()).isFalse();
            assertThat(backOffResult.getNextRetryAttempt()).isBeforeOrEqualTo(firstFailure.plusMillis(ExponentialBackOff.MAX_RETRY_INTERVAL_IN_MILLIS));

            when(systemTimeClock.currentTime()).thenReturn(now(fixed(firstFailure.plusMillis(ExponentialBackOff.MAX_RETRY_INTERVAL_IN_MILLIS).minusMillis(1), systemDefault()))); // backoff result
            assertThat(backOff.backOffResult().shouldBackOff()).isTrue();
        }
    }
}
