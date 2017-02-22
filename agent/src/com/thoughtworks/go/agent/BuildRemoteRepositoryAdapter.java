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

    public BuildRepositoryRemoteAdapter(JobRunner runner, WebSocketSessionHandler webSocketSessionHandler) {
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
        ConsoleTransmission consoleTransmission = new ConsoleTransmission(line, jobIdentifier);
        webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.consoleOut, MessageEncoding.encodeData(consoleTransmission)));
    }
}