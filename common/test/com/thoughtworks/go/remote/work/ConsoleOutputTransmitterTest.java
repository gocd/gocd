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

import java.io.IOException;

import org.jmock.Mockery;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class ConsoleOutputTransmitterTest {
    private ConsoleAppender consoleAppender;
    private Mockery mockery;
    private ConsoleOutputTransmitter transmitter;

    @Before
    public void setup() {
        mockery = new Mockery();
        consoleAppender = mockery.mock(ConsoleAppender.class);
        transmitter = new ConsoleOutputTransmitter(consoleAppender);
    }

    @Test
    public void shouldFlushContentsInBufferToServerInOneGo() throws IOException {
        transmitter.consumeLine("first line");
        transmitter.consumeLine("second line");

        mockery.checking(new Expectations() {
            {
                one(consoleAppender).append("first line\nsecond line\n");
            }
        });

        transmitter.flushToServer();
    }

    @Test
    public void shouldNotFlushToServerWhenBufferIsEmpty() throws IOException {

        mockery.checking(new Expectations() {
            {
                never(consoleAppender).append("");
            }
        });
        transmitter.flushToServer();
    }
}
