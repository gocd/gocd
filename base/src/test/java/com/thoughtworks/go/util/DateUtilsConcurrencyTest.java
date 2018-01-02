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

package com.thoughtworks.go.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class DateUtilsConcurrencyTest {
    @Test
    public void shouldFormatISO8601() throws Exception {
        runInThreads(new DoAction() {
            public void doAction() throws ParseException {
                DateUtils.parseISO8601("2009-09-11 11:11:21 +0800");
            }
        });
    }

    @Test
    public void shouldFormatIntoISO8601String() throws Exception {
        runInThreads(new DoAction() {
            public void doAction() throws ParseException {
                DateUtils.formatISO8601(new Date());
            }
        });
    }

    @Test
    public void shouldFormatRFC822() throws Exception {
        runInThreads(new DoAction() {
            public void doAction() throws ParseException {
                DateUtils.parseRFC822("Wed, 4 Jul 2001 12:08:56 -0700");
            }
        });
    }

    @Test
    public void shouldParseYYYYMMDD() throws Exception {
        runInThreads(new DoAction() {
            public void doAction() throws ParseException {
                DateUtils.parseYYYYMMDD("2018-12-31");
            }
        });
    }

    private void runInThreads(final DoAction action) throws InterruptedException {
        int threadCount = 100;
        Thread[] threads = new Thread[threadCount];
        final List<Throwable> iHateMyLife = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        try {
                            action.doAction();
                        } catch (Throwable e) {
                            iHateMyLife.add(e);
                        }
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
        assertThat(iHateMyLife.toString(), iHateMyLife.size(), is(0));
    }

    private interface DoAction {
        void doAction() throws Exception;
    }
}
