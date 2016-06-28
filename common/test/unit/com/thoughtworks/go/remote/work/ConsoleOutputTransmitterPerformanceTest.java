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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.util.SystemEnvironment;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.lessThanOrEqualTo;

@RunWith(JMock.class)
public class ConsoleOutputTransmitterPerformanceTest {
    private static final int SECOND = 1000;

    Mockery mockery = new Mockery();

    @Before
    public void setUp() {
        new SystemEnvironment().setProperty(SystemEnvironment.INTERVAL, "1");
    }

    @After
    public void tearDown() {
        new SystemEnvironment().clearProperty(SystemEnvironment.INTERVAL);
    }

    @Test
    public void shouldNotBlockPublisherWhenSendingToServer() throws InterruptedException {
        SlowResource resource = new SlowResource();
        final ConsoleOutputTransmitter transmitter = new ConsoleOutputTransmitter(resource);

        int numberToSend = 4;
        int actuallySent = transmitData(transmitter, numberToSend);
        transmitter.stop();

        assertThat("Send should not block.", numberToSend, lessThanOrEqualTo(actuallySent));
    }

    private int transmitData(final ConsoleOutputTransmitter transmitter, final int numberOfSeconds)
            throws InterruptedException {
        final int[] count = {0};
        Thread thread = new Thread(new Runnable() {
            public void run() {
                long startTime = System.currentTimeMillis();
                count[0] = 0;
                while (System.currentTimeMillis() < startTime + numberOfSeconds * SECOND) {
                    String line = "This is line " + count[0];
                    transmitter.consumeLine(line);
                    sleepFor(SECOND);
                    count[0]++;
                }
            }
        });
        thread.start();
        thread.join();
        return count[0];
    }

    private void sleepFor(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //ignore
        }
    }
}
