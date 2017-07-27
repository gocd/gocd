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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.service.RestfulService;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConsoleLogSocketCreator implements WebSocketCreator {

    @Autowired
    private final ConsoleLogSender handler;

    @Autowired
    private final RestfulService restfulService;
    private SocketHealthService socketHealthService;

    @Autowired
    public ConsoleLogSocketCreator(ConsoleLogSender handler, RestfulService restfulService, SocketHealthService socketHealthService) {
        this.handler = handler;
        this.restfulService = restfulService;
        this.socketHealthService = socketHealthService;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return new ConsoleLogSocket(handler, getJobIdentifier(req.getRequestPath()), socketHealthService);
    }

    private JobIdentifier getJobIdentifier(String requestPath) {
        String[] parts = requestPath.split("/");

        String pipelineName = parts[2];
        String pipelineLabel = parts[3];
        String stageName = parts[4];
        String stageCounter = parts[5];
        String jobName = parts[6];

        return restfulService.findJob(pipelineName, pipelineLabel, stageName, stageCounter, jobName);
    }

}
