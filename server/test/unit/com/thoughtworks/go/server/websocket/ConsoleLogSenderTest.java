/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.domain.ConsoleStreamer;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.JobDetailService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.mockito.Mockito.*;

public class ConsoleLogSenderTest {
    private ConsoleLogSender consoleLogSender;
    private ConsoleService consoleService;
    private ConsoleLogEndpoint socket;
    private JobIdentifier jobIdentifier;
    private JobInstance jobInstance;

    @Before
    public void setUp() throws Exception {
        consoleService = mock(ConsoleService.class);
        JobDetailService jobDetailService = mock(JobDetailService.class);
        socket = mock(ConsoleLogEndpoint.class);
        consoleLogSender = new ConsoleLogSender(consoleService, jobDetailService);
        jobIdentifier = mock(JobIdentifier.class);
        jobInstance = mock(JobInstance.class);
        when(jobDetailService.findMostRecentBuild(jobIdentifier)).thenReturn(jobInstance);
    }

    @Test
    public void shouldSendConsoleLog() throws Exception {
        String expected = "Expected output for this test";
        File console = makeConsoleFile(expected);

        when(jobInstance.isCompleted()).thenReturn(true);
        when(consoleService.getStreamer(0L, jobIdentifier)).thenReturn(new ConsoleStreamer(console.toPath(), 0L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(socket).send(expected);
    }

    @Test
    public void shouldSendConsoleLogInMultipleMessagesIfBuildInProgress() throws Exception {
        // simulate delayed append by using 2 different files to represent
        // the log at different points in time
        File consoleInitial = makeConsoleFile("First Output\n");
        File consoleLater = makeConsoleFile("First Output\nSecond Output\n");

        when(jobInstance.isCompleted()).
                thenReturn(false).
                thenReturn(true);

        when(consoleService.getStreamer(anyLong(), eq(jobIdentifier))).
                thenReturn(new ConsoleStreamer(consoleInitial.toPath(), 0L)).
                thenReturn(new ConsoleStreamer(consoleLater.toPath(), 1L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(socket, times(1)).send("First Output");
        verify(socket, times(1)).send("Second Output");
    }

    @Test
    public void shouldNotSendMessagesWhenOutputHasNotAdvanced() throws Exception {
        File console = makeConsoleFile("First Output");
        when(jobInstance.isCompleted()).thenReturn(false).thenReturn(true);
        when(consoleService.getStreamer(anyLong(), eq(jobIdentifier))).
                thenReturn(new ConsoleStreamer(console.toPath(), 0L)).
                thenReturn(new ConsoleStreamer(console.toPath(), 1L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(consoleService, times(2)).getStreamer(anyLong(), eq(jobIdentifier));
        verify(socket, times(1)).send(anyString());
    }

    @Test
    public void shouldCloseSocketAfterProcessingMessage() throws Exception {
        when(jobInstance.isCompleted()).thenReturn(true);
        when(consoleService.getStreamer(0L, jobIdentifier)).thenReturn(new ConsoleStreamer(makeConsoleFile("foo").toPath(), 0L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(socket).close();
    }

    private File makeConsoleFile(String message) throws IOException {
        File console = File.createTempFile("console", ".log");
        console.deleteOnExit();

        Files.write(console.toPath(), message.getBytes());
        return console;
    }
}