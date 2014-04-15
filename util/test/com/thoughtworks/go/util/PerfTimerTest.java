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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PerfTimerTest {

    @Test public void shouldRecordElapsedTime() throws Exception {
        TestingClock clock = new TestingClock();

        PerfTimer timer = PerfTimer.start("Message", clock);
        clock.addSeconds(1);
        timer.stop();

        assertThat(timer.elapsed(), is(1000L));
    }

    @Test public void shouldRecordElapsedTimeForDifferentTimes() throws Exception {
        TestingClock clock = new TestingClock();

        PerfTimer timer = PerfTimer.start("Message", clock);

        clock.addSeconds(1);
        clock.addSeconds(1);
        timer.stop();

        assertThat(timer.elapsed(), is(2000L));
    }

    @Test public void shouldLogTimeWithMessage() throws Exception {
        TestingClock clock = new TestingClock();

        PerfTimer timer = PerfTimer.start("Message", clock);

        clock.addSeconds(1);

        LogFixture log = LogFixture.startListening();
        timer.stop();
        assertThat(log.getLog(), containsString("Performance: Message took 1000ms"));
        log.stopListening();
    }

    @Test public void shouldStopBeforeREportingElapsed() throws Exception {
        TestingClock clock = new TestingClock();

        PerfTimer timer = PerfTimer.start("Message", clock);

        clock.addSeconds(1);

        assertThat(timer.elapsed(), is(1000L));
    }

    @Test public void shouldHaveSimpleConstructor() throws Exception {
        TestingClock clock = new TestingClock();

        PerfTimer timer = PerfTimer.start("Message", clock);

        clock.addSeconds(1);

        LogFixture log = LogFixture.startListening();
        timer.stop();
        assertThat(log.getLog(), containsString("Performance: Message took 1000ms"));
        log.stopListening();
    }

}
