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
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.JobDetailService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.lang.String.format;

@Component
public class ConsoleLogSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogSender.class);
    private static final int BUFFER_SIZE = 1000;

    @Autowired
    private ConsoleService consoleService;
    private JobDetailService jobDetailService;

    @Autowired
    public ConsoleLogSender(ConsoleService consoleService, JobDetailService jobDetailService) {
        this.consoleService = consoleService;
        this.jobDetailService = jobDetailService;
    }

    public void process(final ConsoleLogEndpoint webSocket, JobIdentifier jobIdentifier, long start) throws Exception {
        if (start < 0L) start = 0L;

        // check if we're tailing a running build, or viewing a prior build's logs
        boolean isRunningBuild = !detectCompleted(jobIdentifier);

        do {
            start += sendLogs(webSocket, jobIdentifier, start);

            // allow buffers to fill to avoid sending 1 line at a time for running builds
            if (isRunningBuild) Thread.sleep(500);
        } while (!detectCompleted(jobIdentifier));

        // empty the tail end of the file because the build could have been marked completed, and exited the
        // loop before we've seen the last content update
        if (isRunningBuild) start += sendLogs(webSocket, jobIdentifier, start);

        LOGGER.debug("Sent {} lines", start);
        webSocket.close();
    }

    private boolean detectCompleted(JobIdentifier jobIdentifier) throws Exception {
        return jobDetailService.findMostRecentBuild(jobIdentifier).isCompleted();
    }

    private long sendLogs(ConsoleLogEndpoint webSocket, JobIdentifier jobIdentifier, final long start) throws IllegalArtifactLocationException, IOException {
        final ArrayList<String> buffer = new ArrayList<>();

        ConsoleStreamer console = consoleService.getStreamer(start, jobIdentifier);

        console.stream(new Consumer<String>() {
            @Override
            public void accept(String line) {
                try {
                    if (buffer.size() >= BUFFER_SIZE) {
                        flushBuffer(buffer, webSocket);
                    } else {
                        buffer.add(line);
                    }

                } catch (IOException e) {
                    String message = format("Failed to send log line {} for {}", start + console.processedLineCount(), jobIdentifier);
                    LOGGER.error(message, e);
                }
            }
        });

        flushBuffer(buffer, webSocket);
        return console.processedLineCount();
    }

    private void flushBuffer(ArrayList<String> buffer, ConsoleLogEndpoint webSocket) throws IOException {
        if (buffer.isEmpty()) return;

        webSocket.send(StringUtils.join(buffer, "\n"));
        buffer.clear();
    }

}
