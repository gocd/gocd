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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.util.SystemTimeClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static java.time.Clock.fixed;
import static java.time.LocalDateTime.now;
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
            Clock tenSecondsFromNow = fixed(Instant.now().plusSeconds(10), systemDefault());
            when(systemTimeClock.currentLocalDateTime())
                    .thenReturn(now())
                    .thenReturn(now(tenSecondsFromNow));

            ExponentialBackOff backOff = new ExponentialBackOff(1.5, systemTimeClock);

            backOff.failedAgain();

            assertThat(backOff.backOffResult().shouldBackOff()).isTrue();
        }

        @Test
        void shouldNotBackOffIfCurrentTimeIsAfter_LastFailureTime_Plus_RetryInterval() {
            Clock fiveSecondsAgo = fixed(Instant.now().minusSeconds(5), systemDefault());
            when(systemTimeClock.currentLocalDateTime())
                    .thenReturn(now())
                    .thenReturn(now(fiveSecondsAgo));

            ExponentialBackOff backOff = new ExponentialBackOff(0.5, systemTimeClock);

            backOff.failedAgain();

            assertThat(backOff.backOffResult().shouldBackOff()).isFalse();
        }

        @Test
        void backOffShouldLimitToMaxRetryInterval() {
            Clock oneHourFromNow = fixed(Instant.now().plusSeconds(61 * 60 * 1000), systemDefault());
            Clock oneHourTwoMinutesFromNow = fixed(Instant.now().plusSeconds(62 * 60 * 1000), systemDefault());
            when(systemTimeClock.currentLocalDateTime())
                    .thenReturn(now())
                    .thenReturn(now(oneHourFromNow))
                    .thenReturn(now(oneHourTwoMinutesFromNow));

            ExponentialBackOff backOff = new ExponentialBackOff(2, systemTimeClock);

            backOff.failedAgain();

            assertThat(backOff.backOffResult().shouldBackOff()).isFalse();
        }
    }
}
