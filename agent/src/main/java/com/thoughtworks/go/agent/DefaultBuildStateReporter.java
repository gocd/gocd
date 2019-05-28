/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.buildsession.BuildStateReporter;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageEncoding;
import com.thoughtworks.go.websocket.Report;

public class DefaultBuildStateReporter implements BuildStateReporter {
    private final WebSocketSessionHandler webSocketSessionHandler;
    private final AgentRuntimeInfo agentRuntimeInfo;

    public DefaultBuildStateReporter(WebSocketSessionHandler webSocketSessionHandler, AgentRuntimeInfo agentRuntimeInfo) {
        this.webSocketSessionHandler = webSocketSessionHandler;
        this.agentRuntimeInfo = agentRuntimeInfo;
    }

    @Override
    public void reportBuildStatus(String buildId, JobState buildState) {
        webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.reportCurrentStatus, MessageEncoding.encodeData(new Report(agentRuntimeInfo, buildId, buildState, null))));
    }

    @Override
    public void reportCompleted(String buildId, JobResult buildResult) {
        Report report = new Report(agentRuntimeInfo, buildId, null, buildResult);
        webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.reportCompleted, MessageEncoding.encodeData(report)));
    }

    @Override
    public void reportCompleting(String buildId, JobResult buildResult) {
        Report report = new Report(agentRuntimeInfo, buildId, null, buildResult);
        webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.reportCompleting, MessageEncoding.encodeData(report)));
    }
}
