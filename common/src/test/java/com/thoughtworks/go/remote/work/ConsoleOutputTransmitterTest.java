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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConsoleOutputTransmitterTest {
    @Mock
    private ConsoleAppender consoleAppender;
    private ConsoleOutputTransmitter transmitter;

    @BeforeEach
    public void setup() throws Exception {
        new SystemEnvironment().setProperty(SystemEnvironment.INTERVAL, "60"); // so the thread does not wake up
        transmitter = new ConsoleOutputTransmitter(consoleAppender, 0, mock(ScheduledThreadPoolExecutor.class));
    }

    @AfterEach
    public void tearDown() {
        transmitter.stop();
    }

    @Test
    public void shouldFlushContentsInBufferToServerInOneGo() throws Exception {

        ArgumentCaptor<String> requestArgumentCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(consoleAppender).append(requestArgumentCaptor.capture());

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
