package com.thoughtworks.go.agent;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageEncoding;
import com.thoughtworks.go.websocket.Report;

class BuildRepositoryRemoteAdapter implements BuildRepositoryRemote {
    private JobRunner runner;
    private WebSocketAgentController controller;

    public BuildRepositoryRemoteAdapter(JobRunner runner, WebSocketAgentController controller) {
        this.runner = runner;
        this.controller = controller;
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
        controller.sendAndWaitForAcknowledgement(new Message(Action.reportCurrentStatus, MessageEncoding.encodeData(report)));
    }

    @Override
    public void reportCompleting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        Report report = new Report(agentRuntimeInfo, jobIdentifier, result);
        controller.sendAndWaitForAcknowledgement(new Message(Action.reportCompleting, MessageEncoding.encodeData(report)));
    }

    @Override
    public void reportCompleted(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        Report report = new Report(agentRuntimeInfo, jobIdentifier, result);
        controller.sendAndWaitForAcknowledgement(new Message(Action.reportCompleted, MessageEncoding.encodeData(report)));
    }

    @Override
    public boolean isIgnored(JobIdentifier jobIdentifier) {
        return runner.isJobCancelled();
    }

    @Override
    public String getCookie(AgentIdentifier identifier, String location) {
        throw new UnsupportedOperationException();
    }
}