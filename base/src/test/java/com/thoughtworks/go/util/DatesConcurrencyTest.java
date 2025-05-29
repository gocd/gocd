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
package com.thoughtworks.go.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DatesConcurrencyTest {
    @Test
    public void shouldFormatIso8601CompactOffset() throws Exception {
        runInThreads(() -> Dates.parseIso8601StrictOffset("2009-09-11T11:11:21+08:00"));
    }

    @Test
    public void shouldFormatIntoISO8601String() throws Exception {
        runInThreads(() -> Dates.formatIso8601CompactOffset(new Date()));
    }

    @Test
    public void shouldFormatRFC822() throws Exception {
        runInThreads(() -> Dates.parseRFC822("Wed, 4 Jul 2001 12:08:56 -0700"));
    }

    private void runInThreads(final DoAction action) throws InterruptedException {
        int threadCount = 100;
        Thread[] threads = new Thread[threadCount];
        final List<Throwable> iHateMyLife = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    try {
                        action.doAction();
                    } catch (Throwable e) {
                        iHateMyLife.add(e);
                    }
                }
            });
        }
        for (Thread thread : threads) {
            thread.setDaemon(true);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(iHateMyLife).describedAs(iHateMyLife.toString()).isEmpty();
    }

    private interface DoAction {
        void doAction() throws Exception;
    }
}
