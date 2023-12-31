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

package com.thoughtworks.go.server.service.support;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class DaemonThreadStatsCollectorTest {

    private final DaemonThreadStatsCollector collector = new DaemonThreadStatsCollector();

    @Test
    public void shouldCaptureStatsByThreadId() {
        long threadId = Thread.currentThread().getId();
        collector.captureStats(threadId);
        assertThat(collector.statsFor(threadId))
            .hasEntrySatisfying("CPUTime(nanoseconds)", value -> assertThat(value).asInstanceOf(InstanceOfAssertFactories.LONG).isGreaterThanOrEqualTo(0))
            .hasEntrySatisfying("UUID", value -> assertThat(value).asInstanceOf(InstanceOfAssertFactories.STRING).isNotBlank());
    }
    @Test
    public void shouldIgnoreIfThreadUnknown() {
        assertThat(collector.statsFor(1)).isNull();
    }

    @Test
    public void shouldClearStatsByThreadId() {
        long threadId = Thread.currentThread().getId();
        collector.captureStats(threadId);
        collector.clearStats(threadId);
        assertThat(collector.statsFor(threadId)).isNull();
    }

    @Test
    public void shouldIgnoreIfCantFindThreadCurrentlyAtStart() throws Exception {
        Thread tempThread = withTemporaryThread(thread -> {});
        assertThat(tempThread.isAlive()).isFalse();
        collector.captureStats(tempThread.getId());
        assertThat(collector.statsFor(tempThread.getId())).isNull();
    }

    @Test
    public void shouldIgnoreIfCantFindThreadCurrentlyAtEnd() throws Exception {
        Thread tempThread = withTemporaryThread(thread -> collector.captureStats(thread.getId()));
        assertThat(tempThread.isAlive()).isFalse();
        assertThat(collector.statsFor(tempThread.getId())).isNull();
    }

    private Thread withTemporaryThread(Consumer<Thread> consumer) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread tempThread = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        tempThread.setDaemon(true);
        tempThread.start();
        consumer.accept(tempThread);
        latch.countDown();
        tempThread.join();
        return tempThread;
    }
}
