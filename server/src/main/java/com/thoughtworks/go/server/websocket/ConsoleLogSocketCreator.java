/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

@Component
public class ConsoleLogSocketCreator implements JettyWebSocketCreator {

    private final ConsoleLogSender handler;
    private final RestfulService restfulService;
    private final Charset consoleLogCharset;
    private final SocketHealthService socketHealthService;

    @Autowired
    public ConsoleLogSocketCreator(ConsoleLogSender handler, RestfulService restfulService, SocketHealthService socketHealthService, SystemEnvironment systemEnvironment) {
        this.handler = handler;
        this.restfulService = restfulService;
        this.socketHealthService = socketHealthService;
        this.consoleLogCharset = systemEnvironment.consoleLogCharset();
    }

    @Override
    public Object createWebSocket(JettyServerUpgradeRequest req, JettyServerUpgradeResponse resp) {
        return new ConsoleLogSocket(handler, getJobIdentifier(req.getRequestPath()), socketHealthService, consoleLogCharset);
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
