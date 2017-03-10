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

import com.thoughtworks.go.domain.ConsoleOut;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.JobDetailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;

@Component
public class ClientRemoteHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientRemoteHandler.class);


    @Autowired
    private ConsoleService consoleService;
    private JobDetailService jobDetailService;

    @Autowired
    public ClientRemoteHandler(ConsoleService consoleService, JobDetailService jobDetailService) {
        this.consoleService = consoleService;
        this.jobDetailService = jobDetailService;
    }

    public void process(ConsoleLogEndpoint clientRemoteSocket, JobIdentifier jobIdentifier) throws Exception {
        int currentOutputPosition = 0;
        int previousOutputPosition = currentOutputPosition;

        ConsoleOut consoleOut = readConsoleOut(jobIdentifier, currentOutputPosition);
        clientRemoteSocket.send(consoleOut.output());
        currentOutputPosition = consoleOut.calculateNextStart();
        JobInstance jobInstance = jobDetailService.findMostRecentBuild(jobIdentifier);

        while (!jobInstance.isCompleted()) {
            consoleOut = readConsoleOut(jobIdentifier, currentOutputPosition);
            currentOutputPosition = consoleOut.calculateNextStart();
            if (previousOutputPosition != currentOutputPosition) {
                clientRemoteSocket.send(consoleOut.output());
                previousOutputPosition = currentOutputPosition;
                jobInstance = jobDetailService.findMostRecentBuild(jobIdentifier);
            }
        }
        clientRemoteSocket.close();
    }

    private ConsoleOut readConsoleOut(JobIdentifier identifier, int consoleOutputPosition) throws IOException, IllegalArtifactLocationException {
        ConsoleOut consoleOut;
        try {
            consoleOut = consoleService.getConsoleOut(identifier, consoleOutputPosition);
        } catch (FileNotFoundException e) {
            consoleOut = new ConsoleOut("", 0, 0);
        }
        return consoleOut;
    }
}
