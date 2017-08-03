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
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.JobDetailService;
import com.thoughtworks.go.server.util.Retryable;
import org.apache.commons.io.output.ProxyOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.GZIPOutputStream;

@Component
public class ConsoleLogSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogSender.class);

    private static final int LOG_DOES_NOT_EXIST = 4004;
    private static final int BUF_SIZE = 1024 * 1024; // 1MB
    private static final int FILL_INTERVAL = 500;

    @Autowired
    private ConsoleService consoleService;

    @Autowired
    private JobDetailService jobDetailService;

    @Autowired
    private SocketHealthService socketHealthService;

    @Autowired
    ConsoleLogSender(ConsoleService consoleService, JobDetailService jobDetailService, SocketHealthService socketHealthService) {
        this.consoleService = consoleService;
        this.jobDetailService = jobDetailService;
        this.socketHealthService = socketHealthService;
    }

    public void process(final SocketEndpoint webSocket, JobIdentifier jobIdentifier, long start) throws Exception {
        if (start < 0L) start = 0L;

        socketHealthService.register(webSocket);

        // check if we're tailing a running build, or viewing a prior build's logs
        boolean isRunningBuild = !detectCompleted(jobIdentifier);

        // Sometimes the log file may not have been created yet; leave it up to the client to handle reconnect logic.
        try {
            waitForLogToExist(webSocket, jobIdentifier);
        } catch (Retryable.TooManyRetriesException e) {
            socketHealthService.deregister(webSocket);
            webSocket.close(LOG_DOES_NOT_EXIST, e.getMessage());
            return;
        }

        try (ConsoleConsumer streamer = consoleService.getStreamer(start, jobIdentifier)) {
            do {
                start += sendLogs(webSocket, streamer, jobIdentifier);

                // allow buffers to fill to avoid sending 1 line at a time for running builds
                if (isRunningBuild) {
                    Thread.sleep(FILL_INTERVAL);
                }
            } while (webSocket.isOpen() && !detectCompleted(jobIdentifier));

            // empty the tail end of the file because the build could have been marked completed, and exited the
            // loop before we've seen the last content update
            if (isRunningBuild) sendLogs(webSocket, streamer, jobIdentifier);

            LOGGER.debug("Sent {} log lines for {}", streamer.totalLinesConsumed(), jobIdentifier);
        } finally {
            socketHealthService.deregister(webSocket);
            webSocket.close();
        }
    }

    private void waitForLogToExist(final SocketEndpoint websocket, final JobIdentifier jobIdentifier) throws Retryable.TooManyRetriesException {
        Retryable.retry(integer -> {
            try {
                return !websocket.isOpen() || consoleService.consoleLogFile(jobIdentifier).exists();
            } catch (IllegalArtifactLocationException e) {
                LOGGER.error("Job identifier {} is not valid; Cannot resolve console log file", jobIdentifier, e);

                return true; // Stop trying
            }
        }, String.format("waiting for console log to exist for %s", jobIdentifier), 20);
    }

    private boolean detectCompleted(JobIdentifier jobIdentifier) throws Exception {
        return jobDetailService.findMostRecentBuild(jobIdentifier).isCompleted();
    }

    private long sendLogs(final SocketEndpoint webSocket, final ConsoleConsumer console, final JobIdentifier jobIdentifier) throws IllegalArtifactLocationException, IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(BUF_SIZE);
        final OutputStream proxyOutputStream = new AutoFlushingStream(buffer, webSocket, BUF_SIZE);
        long linesProcessed = console.stream(line -> {
            try {
                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bytes.length);
                byteArrayOutputStream.write(bytes);
                byteArrayOutputStream.write('\n');
                proxyOutputStream.write(byteArrayOutputStream.toByteArray());
            } catch (IOException e) {
                LOGGER.error("Failed to send log line {} for {}", console.totalLinesConsumed(), jobIdentifier, e);
            }
        });

        flushBuffer(buffer, webSocket);
        return linesProcessed;
    }

    private void flushBuffer(ByteArrayOutputStream buffer, SocketEndpoint webSocket) throws IOException {
        if (buffer.size() == 0) return;
        webSocket.send(ByteBuffer.wrap(gzip(buffer.toByteArray())));
        buffer.reset();
    }

    byte[] gzip(byte[] input) {
        if (input.length < 512) {
            return input;
        }
        // To avoid having to re-allocate the internal byte array, allocate an initial buffer assuming a safe 10:1 compression ratio
        final ByteArrayOutputStream gzipBytes = new ByteArrayOutputStream(input.length / 10);
        try {
            final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(gzipBytes, 1024 * 8);
            gzipOutputStream.write(input);
            gzipOutputStream.close();
        } catch (IOException e) {
            LOGGER.error("Could not gzip {}", input);
        }
        return gzipBytes.toByteArray();
    }

    // Flushes stream just before it becomes larger than `bufSize`
    private class AutoFlushingStream extends ProxyOutputStream {
        private final ByteArrayOutputStream buffer;
        private final SocketEndpoint webSocket;
        private final int bufSize;

        public AutoFlushingStream(ByteArrayOutputStream buffer, SocketEndpoint webSocket, int bufSize) {
            super(buffer);
            this.buffer = buffer;
            this.webSocket = webSocket;
            this.bufSize = bufSize;
        }

        @Override
        protected void beforeWrite(int n) throws IOException {
            maybeFlush(n);
        }

        @Override
        protected void afterWrite(int n) throws IOException {
            maybeFlush(n);
        }

        private void maybeFlush(int n) throws IOException {
            if (buffer.size() + n >= bufSize) {
                flushBuffer(buffer, webSocket);
            }
        }
    }
}
