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

import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsoleOutputTransmitterPerformanceTest {

    @Test
    public void shouldNotBlockPublisherWhenSendingToServer() throws InterruptedException {
        int consolePublishIntervalMillis = 50;
        try (ConsoleOutputTransmitter transmitter = new ConsoleOutputTransmitter(new SlowResource(), consolePublishIntervalMillis, TimeUnit.MILLISECONDS, new ScheduledThreadPoolExecutor(1))) {
            long startTime = System.currentTimeMillis();
            int numberMessages = 4;
            for (int i = 0; i < numberMessages; i++) {
                transmitter.consumeLine("This is line " + i);
                Thread.sleep(consolePublishIntervalMillis);
            }
            assertThat(System.currentTimeMillis() - startTime)
                .describedAs("Publishing messages should not be blocked (buffer of 50 ms)")
                .isLessThan(consolePublishIntervalMillis * numberMessages + 50);
        }
    }
}
