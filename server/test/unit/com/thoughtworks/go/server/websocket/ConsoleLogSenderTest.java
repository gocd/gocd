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
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.service.ConsoleService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ConsoleLogSenderTest {
    private ConsoleLogSender consoleLogSender;
    private ConsoleService consoleService;
    private SocketEndpoint socket;
    private JobIdentifier jobIdentifier;
    private JobInstance jobInstance;
    private SocketHealthService socketHealthService;


    @Before
    public void setUp() throws Exception {
        consoleService = mock(ConsoleService.class);
        socketHealthService = mock(SocketHealthService.class);
        JobInstanceDao jobInstanceDao = mock(JobInstanceDao.class);
        socket = mock(SocketEndpoint.class);
        when(socket.isOpen()).thenReturn(true);
        consoleLogSender = new ConsoleLogSender(consoleService, jobInstanceDao, socketHealthService);
        jobIdentifier = mock(JobIdentifier.class);
        jobInstance = mock(JobInstance.class);
        when(jobInstanceDao.mostRecentJobWithTransitions(jobIdentifier)).thenReturn(jobInstance);
    }

    @Test
    public void shouldSendConsoleLog() throws Exception {
        String expected = "Expected output for this test";
        File console = makeConsoleFile(expected);

        when(jobInstance.isCompleted()).thenReturn(true);
        when(consoleService.getStreamer(0L, jobIdentifier)).thenReturn(new ConsoleStreamer(console.toPath(), 0L));
        when(consoleService.getStreamer(1L, jobIdentifier)).thenReturn(new ConsoleStreamer(console.toPath(), 1L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(socket).send(ByteBuffer.wrap(consoleLogSender.maybeGzipIfLargeEnough((expected + '\n').getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void shouldSendfooConsoleLog() throws Exception {
        when(jobInstance.isCompleted()).thenReturn(true);

        File fakeFile = mock(File.class);
        when(fakeFile.exists()).thenReturn(false);
        when(consoleService.consoleLogFile(jobIdentifier)).thenReturn(fakeFile);
        String build = "build p1/s1/j1";
        when(jobIdentifier.toFullString()).thenReturn(build);

        consoleLogSender.process(socket, jobIdentifier, 0L);

        int expectedCode = 4410;
        String expectedReason = String.format("Console log for %s is unavailable as it may have been purged by Go or deleted externally.", build);
        verify(socket).close(expectedCode, expectedReason);
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

        verify(socket, times(1)).send(ByteBuffer.wrap(consoleLogSender.maybeGzipIfLargeEnough("First Output\n".getBytes(StandardCharsets.UTF_8))));
        verify(socket, times(1)).send(ByteBuffer.wrap(consoleLogSender.maybeGzipIfLargeEnough("Second Output\n".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void shouldSendConsoleLogEvenAfterBuildCompletion() throws Exception {
        when(jobInstance.isCompleted()).
                thenReturn(false).
                thenReturn(true);

        File fakeFile = mock(File.class);
        when(fakeFile.exists()).thenReturn(true);
        when(consoleService.consoleLogFile(jobIdentifier)).thenReturn(fakeFile);
        when(consoleService.getStreamer(0L, jobIdentifier))
                .thenReturn(new FakeConsoleStreamer("First Output", "Second Output"));
        when(consoleService.getStreamer(1L, jobIdentifier))
                .thenReturn(new FakeConsoleStreamer("More Output"));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(socket, times(1)).send(ByteBuffer.wrap(consoleLogSender.maybeGzipIfLargeEnough("First Output\n".getBytes(StandardCharsets.UTF_8))));
        verify(socket, times(1)).send(ByteBuffer.wrap(consoleLogSender.maybeGzipIfLargeEnough("Second Output\n".getBytes(StandardCharsets.UTF_8))));
        verify(socket, times(1)).send(ByteBuffer.wrap(consoleLogSender.maybeGzipIfLargeEnough("More Output\n".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void shouldNotSendMessagesWhenOutputHasNotAdvanced() throws Exception {
        File console = makeConsoleFile("First Output");
        when(jobInstance.isCompleted()).thenReturn(false).thenReturn(true);
        when(consoleService.getStreamer(anyLong(), eq(jobIdentifier))).
                thenReturn(new ConsoleStreamer(console.toPath(), 0L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(jobInstance, times(3)).isCompleted();
        verify(socket, times(1)).send(anyObject());
    }

    @Test
    public void shouldCloseSocketAfterProcessingMessage() throws Exception {
        File console = makeConsoleFile("foo");

        when(jobInstance.isCompleted()).thenReturn(true);
        when(consoleService.getStreamer(0L, jobIdentifier)).thenReturn(new ConsoleStreamer(console.toPath(), 0L));
        when(consoleService.getStreamer(1L, jobIdentifier)).thenReturn(new ConsoleStreamer(console.toPath(), 1L));

        consoleLogSender.process(socket, jobIdentifier, 0L);

        verify(socket).close();
    }

    @Test
    public void shouldNotGzipContentsLessThan512Bytes() throws Exception {
        byte[] bytes = RandomStringUtils.randomAlphanumeric(511).getBytes(StandardCharsets.UTF_8);
        byte[] gzipped = consoleLogSender.maybeGzipIfLargeEnough(bytes);
        assertThat(bytes, equalTo(gzipped));
    }

    @Test
    public void shouldGzipContentsGreaterThan512Bytes() throws Exception {
        byte[] bytes = RandomStringUtils.randomAlphanumeric(512).getBytes(StandardCharsets.UTF_8);

        byte[] gzipped = consoleLogSender.maybeGzipIfLargeEnough(bytes);
        assertThat(gzipped.length, lessThanOrEqualTo(bytes.length));

        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzipped));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(gzipped.length);
        IOUtils.copy(gzipInputStream, byteArrayOutputStream);
        assertThat(bytes, equalTo(byteArrayOutputStream.toByteArray()));
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
            // this is necessary for showing no logs has been missed out even after job completion
            if(mockedLines.length <= count) {
                return mockedLines.length;
            }
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