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

    public static final long CONSOLE_PUBLISH_INTERVAL_MILLIS = 100;

    @Test
    public void shouldNotBlockPublisherWhenSendingToServer() throws InterruptedException {
        int numberToSend = 4;
        int actuallySent;
        try (ConsoleOutputTransmitter transmitter = new ConsoleOutputTransmitter(new SlowResource(), CONSOLE_PUBLISH_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, new ScheduledThreadPoolExecutor(1))) {
            actuallySent = transmitData(transmitter, numberToSend);
        }
        assertThat(numberToSend).isLessThanOrEqualTo(actuallySent);
    }

    private int transmitData(final ConsoleOutputTransmitter transmitter, final int numberIterations)
        throws InterruptedException {
        final int[] count = {0};
        Thread thread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            count[0] = 0;
            while (System.currentTimeMillis() < startTime + (numberIterations * CONSOLE_PUBLISH_INTERVAL_MILLIS)) {
                String line = "This is line " + count[0];
                transmitter.consumeLine(line);
                sleepForPublishInterval();
                count[0]++;
            }
        });
        thread.start();
        thread.join();
        return count[0];
    }

    private void sleepForPublishInterval() {
        try {
            Thread.sleep(ConsoleOutputTransmitterPerformanceTest.CONSOLE_PUBLISH_INTERVAL_MILLIS);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }
}
