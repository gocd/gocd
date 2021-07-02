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
package com.thoughtworks.go.util.command;

import com.thoughtworks.go.util.SystemTimeClock;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StreamPumperTest {

    @Test
    public void testPumping() {
        String line1 = "line1";
        String line2 = "line2";
        String lines = line1 + "\n" + line2;
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(lines.getBytes());

        TestConsumer consumer = new TestConsumer();
        StreamPumper pumper = new StreamPumper(inputStream, consumer, "", "utf-8", new SystemTimeClock());
        new Thread(pumper).start();

        //Check the consumer to see if it got both lines.
        assertThat(consumer.wasLineConsumed(line1, 1000), is(true));
        assertThat(consumer.wasLineConsumed(line2, 1000), is(true));
    }

    @Test
    public void shouldKnowIfPumperExpired() throws Exception {
        PipedOutputStream output = new PipedOutputStream();
        InputStream inputStream = new PipedInputStream(output);
        try {
            TestingClock clock = new TestingClock();
            StreamPumper pumper = new StreamPumper(inputStream, new TestConsumer(), "", "utf-8", clock);
            new Thread(pumper).start();

            output.write("line1\n".getBytes());
            output.flush();

            long timeoutDuration = 2L;
            assertThat(pumper.didTimeout(timeoutDuration, TimeUnit.SECONDS), is(false));
            clock.addSeconds(5);
            assertThat(pumper.didTimeout(timeoutDuration, TimeUnit.SECONDS), is(true));
        } finally {
            output.close();
        }
    }

    @Test
    public void shouldNotHaveExpiredTimeoutWhenCompleted() throws Exception {
        PipedOutputStream output = new PipedOutputStream();
        InputStream inputStream = new PipedInputStream(output);
        TestingClock clock = new TestingClock();
        StreamPumper pumper = new StreamPumper(inputStream, new TestConsumer(), "", "utf-8", clock);
        new Thread(pumper).start();

        output.write("line1\n".getBytes());
        output.flush();
        output.close();
        pumper.readToEnd();
        clock.addSeconds(2);
        assertThat(pumper.didTimeout(1L, TimeUnit.SECONDS), is(false));
    }

    /**
     * Used by the test to track whether a line actually got consumed or not.
     */
    class TestConsumer implements StreamConsumer {

        private List lines = new ArrayList();

        /**
         * Checks to see if this consumer consumed a particular line. This method
         * will wait up to timeout number of milliseconds for the line to get
         * consumed.
         *
         * @param testLine Line to test for.
         * @param timeout  Number of milliseconds to wait for the line.
         * @return true if the line gets consumed, else false.
         */
        public boolean wasLineConsumed(String testLine, long timeout) {

            long start = System.currentTimeMillis();
            long trialTime = 0;

            do {
                if (lines.contains(testLine)) {
                    return true;
                }

                //Sleep a bit.
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    //ignoring...
                }

                //How long have been waiting for the line?
                trialTime = System.currentTimeMillis() - start;

            } while (trialTime < timeout);

            //If we got here, then the line wasn't consume within the timeout
            return false;
        }

        @Override
        public void consumeLine(String line) {
            lines.add(line);
        }
    }


}
