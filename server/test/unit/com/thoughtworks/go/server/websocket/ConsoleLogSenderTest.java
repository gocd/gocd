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

import com.thoughtworks.go.domain.ConsoleConsumer;
import com.thoughtworks.go.domain.ConsoleStreamer;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.JobDetailService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

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
        when(socket.isOpen()).thenReturn(true);
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
        when(jobInstance.isCompleted()).
                thenReturn(false).
                thenReturn(true);

        File fakeFile = mock(File.class);
        when(fakeFile.exists()).thenReturn(true);
        when(consoleService.consoleLogFile(jobIdentifier)).thenReturn(fakeFile);
        when(consoleService.getStreamer(anyLong(), eq(jobIdentifier))).
                thenReturn(new FakeConsoleStreamer("First Output", "Second Output"));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(socket, times(1)).send("First Output");
        verify(socket, times(1)).send("Second Output");
    }

    @Test
    public void shouldNotSendMessagesWhenOutputHasNotAdvanced() throws Exception {
        File console = makeConsoleFile("First Output");
        when(jobInstance.isCompleted()).thenReturn(false).thenReturn(true);
        when(consoleService.getStreamer(anyLong(), eq(jobIdentifier))).
                thenReturn(new ConsoleStreamer(console.toPath(), 0L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(jobInstance, times(2)).isCompleted();
        verify(socket, times(1)).send(anyString());
    }

    @Test
    public void shouldCloseSocketAfterProcessingMessage() throws Exception {
        File console = makeConsoleFile("foo");

        when(jobInstance.isCompleted()).thenReturn(true);
        when(consoleService.getStreamer(0L, jobIdentifier)).thenReturn(new ConsoleStreamer(console.toPath(), 0L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(socket).close();
    }

    private File makeConsoleFile(String message) throws IOException, IllegalArtifactLocationException {
        File console = File.createTempFile("console", ".log");
        console.deleteOnExit();
        when(consoleService.consoleLogFile(jobIdentifier)).thenReturn(console);

        Files.write(console.toPath(), message.getBytes());
        return console;
    }

    /**
     * allows us to simulate log appending by doing reads with multiple invocations to stream()
     */
    private class FakeConsoleStreamer implements ConsoleConsumer {

        private String[] mockedLines;
        private int count = 0;

        FakeConsoleStreamer(String... mockedLines) {
            this.mockedLines = mockedLines;
        }

        @Override
        public long stream(Consumer<String> action) throws IOException {
            action.accept(mockedLines[count]);
            return ++count;
        }

        @Override
        public long totalLinesConsumed() {
            return (long) count;
        }

        @Override
        public void close() throws Exception {
        }
    }
}