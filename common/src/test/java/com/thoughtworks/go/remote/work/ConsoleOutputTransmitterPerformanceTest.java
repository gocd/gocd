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
package com.thoughtworks.go.remote.work;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

public class ConsoleOutputTransmitterPerformanceTest {

    private static final long CONSOLE_PUBLISH_INTERVAL_MILLIS = 200;

    private ConsoleOutputTransmitter transmitter;

    @BeforeEach
    public void setup() {
        try (ConsoleOutputTransmitter transmitter = new ConsoleOutputTransmitter(new SlowConsoleAppender(), 50, MILLISECONDS, new ScheduledThreadPoolExecutor(1))) {
            transmitter.consumeLine("Warming up...");
        }
        transmitter = new ConsoleOutputTransmitter(new SlowConsoleAppender(), CONSOLE_PUBLISH_INTERVAL_MILLIS, MILLISECONDS, new ScheduledThreadPoolExecutor(1));
    }

    @Test
    public void shouldNotBlockPublisherWhenSendingToServer() throws InterruptedException {
        int numberPublishIntervals = 5;
        int sendPerPublishInterval = 5;
        long sendIntervalMillis = CONSOLE_PUBLISH_INTERVAL_MILLIS / sendPerPublishInterval;
        int expectedNumberToSendWithZeroBlocking = numberPublishIntervals * sendPerPublishInterval;

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < expectedNumberToSendWithZeroBlocking; i++) {
            transmitter.consumeLine("This is line " + i);
            Thread.sleep(sendIntervalMillis);
        }
        assertThat(System.currentTimeMillis() - startTime)
            .describedAs("Publishing messages should not be blocked excessively (buffer of 15 for sleep variation and minor blocking%)")
            .isCloseTo(expectedNumberToSendWithZeroBlocking * sendIntervalMillis, withinPercentage(15));
    }

    private static void sleepFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }

    private static class SlowConsoleAppender implements ConsoleAppender {
        @Override
        public void append(String content) {
            // Every publish will take 90% of the published interval, so that it can actually catch up
            sleepFor(Math.round(0.9 * CONSOLE_PUBLISH_INTERVAL_MILLIS));
        }
    }
}
