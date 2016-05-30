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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConsoleOutputTransmitterTest {
    @Mock
    private ConsoleAppender consoleAppender;
    private ArgumentCaptor<String> requestArgumentCaptor;
    private ConsoleOutputTransmitter transmitter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        new SystemEnvironment().setProperty(SystemEnvironment.INTERVAL, "60"); // so the thread does not wake up

        requestArgumentCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(consoleAppender).append(requestArgumentCaptor.capture());
        transmitter = new ConsoleOutputTransmitter(consoleAppender);
    }

    @After
    public void tearDown() {
        transmitter.stop();
    }

    @Test
    public void shouldFlushContentsInBufferToServerInOneGo() throws Exception {
        transmitter.consumeLine("first line");
        transmitter.consumeLine("second line");

        transmitter.flushToServer();

        verify(consoleAppender).append(any(String.class));
        assertThat(requestArgumentCaptor.getValue(), containsString("first line\n"));
        assertThat(requestArgumentCaptor.getValue(), containsString("second line\n"));
    }

    @Test
    public void shouldNotFlushToServerWhenBufferIsEmpty() throws Exception {
        transmitter.flushToServer();

        verify(consoleAppender, never()).append(any(String.class));
    }
}
