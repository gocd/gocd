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
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.CloseReason;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.function.Consumer;

@Component
public class ConsoleLogSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogSender.class);
    private static final int BUFFER_SIZE = 1000;
    private static final int FILL_INTERVAL = 500;

    private final SystemEnvironment systemEnvironment = new SystemEnvironment();

    @Autowired
    private ConsoleService consoleService;

    @Autowired
    private JobDetailService jobDetailService;

    private static boolean isIdledTooLong(Instant startTime, long threshold) {
        return Duration.between(startTime, Instant.now()).toMillis() >= threshold;
    }

    @Autowired
    ConsoleLogSender(ConsoleService consoleService, JobDetailService jobDetailService) {
        this.consoleService = consoleService;
        this.jobDetailService = jobDetailService;
    }

    public void process(final ConsoleLogEndpoint webSocket, JobIdentifier jobIdentifier, long start) throws Exception {
        if (start < 0L) start = 0L;

        // check if we're tailing a running build, or viewing a prior build's logs
        boolean isRunningBuild = !detectCompleted(jobIdentifier);

        // Sometimes the log file may not have been created yet; leave it up to the client to handle reconnect logic.
        try {
            waitForLogToExist(webSocket, jobIdentifier);
        } catch (TooManyRetriesException e) {
            webSocket.close(new CloseReason(ConsoleLogEndpoint.LOG_DOES_NOT_EXIST, e.getMessage()));
            return;
        }

        Instant t1 = Instant.now();
        long numLinesProcessed;
        long keepAlive = systemEnvironment.getWsKeepAlive();

        try (ConsoleConsumer streamer = consoleService.getStreamer(start, jobIdentifier)) {
            do {
                start += (numLinesProcessed = sendLogs(webSocket, streamer, jobIdentifier));

                // allow buffers to fill to avoid sending 1 line at a time for running builds
                if (isRunningBuild) {
                    Thread.sleep(FILL_INTERVAL);

                    // reset keepAlive timer if we sent data -- we're not idling
                    if (numLinesProcessed > 0) t1 = Instant.now();

                    // send ping if we've been idle for too long (i.e. no frames transmitted over connection)
                    if (isIdledTooLong(t1, keepAlive)) {
                        webSocket.ping();
                        t1 = Instant.now(); // reset timer after ping()
                    }
                }
            } while (webSocket.isOpen() && !detectCompleted(jobIdentifier));

            // empty the tail end of the file because the build could have been marked completed, and exited the
            // loop before we've seen the last content update
            if (isRunningBuild) sendLogs(webSocket, streamer, jobIdentifier);

            LOGGER.debug("Sent {} log lines for {}", streamer.totalLinesConsumed(), jobIdentifier);
        }

        webSocket.close();
    }

    private void waitForLogToExist(ConsoleLogEndpoint websocket, JobIdentifier jobIdentifier) throws IllegalArtifactLocationException, TooManyRetriesException {
        for (int retries = 20; retries > 0; --retries) {
            if (!websocket.isOpen() || consoleService.consoleLogFile(jobIdentifier).exists()) {
                return;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                LOGGER.warn("Couldn't sleep while testing console log existence", ignored);
            }
        }

        throw new TooManyRetriesException(String.format("Console log file does not yet exist for %s", jobIdentifier));
    }

    private boolean detectCompleted(JobIdentifier jobIdentifier) throws Exception {
        return jobDetailService.findMostRecentBuild(jobIdentifier).isCompleted();
    }

    private long sendLogs(final ConsoleLogEndpoint webSocket, final ConsoleConsumer console, final JobIdentifier jobIdentifier) throws IllegalArtifactLocationException, IOException {
        final ArrayList<String> buffer = new ArrayList<>();

        long linesProcessed = console.stream(new Consumer<String>() {
            @Override
            public void accept(String line) {
                try {
                    if (buffer.size() >= BUFFER_SIZE) {
                        flushBuffer(buffer, webSocket);
                    } else {
                        buffer.add(line);
                    }

                } catch (IOException e) {
                    LOGGER.error("Failed to send log line {} for {}", console.totalLinesConsumed(), jobIdentifier, e);
                }
            }
        });

        flushBuffer(buffer, webSocket);
        return linesProcessed;
    }

    private void flushBuffer(ArrayList<String> buffer, ConsoleLogEndpoint webSocket) throws IOException {
        if (buffer.isEmpty()) return;

        webSocket.send(StringUtils.join(buffer, "\n"));
        buffer.clear();
    }

    class TooManyRetriesException extends Exception {
        TooManyRetriesException(String message) {
            super(message);
        }
    }
}
