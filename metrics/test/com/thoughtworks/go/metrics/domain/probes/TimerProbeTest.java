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

package com.thoughtworks.go.metrics.domain.probes;

import com.thoughtworks.go.metrics.domain.context.Context;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;

public class TimerProbeTest {
    private TimerProbe timerProbe;

    @Before
    public void setUp() throws Exception {
        timerProbe = new TimerProbe(ProbeType.VALIDATING_CONFIG, null);
    }

    @Test
    public void shouldCaptureExecutionMetrics() throws InterruptedException {
        Context context = timerProbe.begin();
        Thread.sleep(500L);
        timerProbe.end(context);

        context = timerProbe.begin();
        Thread.sleep(1000L);
        timerProbe.end(context);

        assertThat(timerProbe.getTimer().count(), is(2L));
    }

    @Test
    public void shouldReturnWhenStoppingWatchWithoutStartingItAndNotThrowNullPointerException() {
        timerProbe.end(null);
    }

    @Test
    public void shouldCaptureExecutionMetrics_MultithreadedMode() throws InterruptedException {
        MyThread longestRunningThread = new MyThread(500L);
        MyThread middleThread = new MyThread(300L);
        MyThread shortestRunningThread = new MyThread(100L);
        Thread t1 = new Thread(longestRunningThread);
        Thread t2 = new Thread(middleThread);
        Thread t3 = new Thread(shortestRunningThread);

        t1.start();
        Thread.sleep(100L);
        t2.start();
        Thread.sleep(100L);
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        assertThat(longestRunningThread.getMin(), is(shortestRunningThread.getMin()));
        assertThat(longestRunningThread.getMax(), greaterThan(middleThread.getMax()));
        assertThat(longestRunningThread.getMax(), greaterThan(shortestRunningThread.getMax()));

        assertThat(middleThread.getMin(), is(shortestRunningThread.getMin()));

        assertThat(shortestRunningThread.getMax(), is(shortestRunningThread.getMin()));
    }


    class MyThread implements Runnable {
        private final long millis;
        private double min;
        private double max;

        MyThread(long millis) {
            this.millis = millis;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        @Override
        public void run() {
            TimerProbe goTimer = new TimerProbe(ProbeType.UPDATE_CONFIG, null);
            try {
                Context goTimerContext = goTimer.begin();
                Thread.sleep(millis);
                goTimer.end(goTimerContext);
                min = goTimer.getTimer().min();
                max = goTimer.getTimer().max();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
