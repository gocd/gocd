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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.websocket.*;

class BuildRepositoryRemoteAdapter implements BuildRepositoryRemote {
    private JobRunner runner;
    private WebSocketSessionHandler webSocketSessionHandler;

    BuildRepositoryRemoteAdapter(JobRunner runner, WebSocketSessionHandler webSocketSessionHandler) {
        this.runner = runner;
        this.webSocketSessionHandler = webSocketSessionHandler;
    }

    @Override
    public AgentInstruction ping(AgentRuntimeInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Work getWork(AgentRuntimeInfo runtimeInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportCurrentStatus(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobState jobState) {
        Report report = new Report(agentRuntimeInfo, jobIdentifier, jobState);
        webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.reportCurrentStatus, MessageEncoding.encodeData(report)));
    }

    @Override
    public void reportCompleting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        Report report = new Report(agentRuntimeInfo, jobIdentifier, result);
        webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.reportCompleting, MessageEncoding.encodeData(report)));
    }

    @Override
    public void reportCompleted(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        Report report = new Report(agentRuntimeInfo, jobIdentifier, result);
        webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.reportCompleted, MessageEncoding.encodeData(report)));
    }

    @Override
    public boolean isIgnored(JobIdentifier jobIdentifier) {
        return runner.isJobCancelled();
    }

    @Override
    public String getCookie(AgentIdentifier identifier, String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void consumeLine(String line, JobIdentifier jobIdentifier) {
        taggedConsumeLine(null, line, jobIdentifier);
    }

    @Override
    public void taggedConsumeLine(String tag, String line, JobIdentifier jobIdentifier) {
        ConsoleTransmission consoleTransmission = new ConsoleTransmission(tag, line, jobIdentifier);
        webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.consoleOut, MessageEncoding.encodeData(consoleTransmission)));
    }
}