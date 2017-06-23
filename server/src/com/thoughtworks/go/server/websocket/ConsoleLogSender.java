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
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Component
public class ConsoleLogSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogSender.class);

    private static final int LOG_DOES_NOT_EXIST = 4004;
    private static final int BUFFER_SIZE = 1000;
    private static final int FILL_INTERVAL = 500;

    private final SystemEnvironment systemEnvironment = new SystemEnvironment();

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
        Retryable.retry(new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                try {
                    return !websocket.isOpen() || consoleService.consoleLogFile(jobIdentifier).exists();
                } catch (IllegalArtifactLocationException e) {
                    LOGGER.error("Job identifier {} is not valid; Cannot resolve console log file", jobIdentifier, e);

                    return true; // Stop trying
                }
            }
        }, String.format("waiting for console log to exist for %s", jobIdentifier), 20);
    }

    private boolean detectCompleted(JobIdentifier jobIdentifier) throws Exception {
        return jobDetailService.findMostRecentBuild(jobIdentifier).isCompleted();
    }

    private long sendLogs(final SocketEndpoint webSocket, final ConsoleConsumer console, final JobIdentifier jobIdentifier) throws IllegalArtifactLocationException, IOException {
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

    private void flushBuffer(ArrayList<String> buffer, SocketEndpoint webSocket) throws IOException {
        if (buffer.isEmpty()) return;

        webSocket.send(StringUtils.join(buffer, "\n"));
        buffer.clear();
    }

}
