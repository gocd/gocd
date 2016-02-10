/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.Report;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class AgentRemoteHandlerTest {
    private AgentRemoteHandler handler;
    private BuildRepositoryRemote remote;
    private AgentService agentService;
    private AgentStub agent = new AgentStub();

    @Before
    public void setUp() {
        remote = mock(BuildRepositoryRemote.class);
        agentService = mock(AgentService.class);
        handler = new AgentRemoteHandler(remote, agentService);
    }

    @Test
    public void registerConnectedAgentsByPing() {
        AgentInstance instance = AgentInstanceMother.idle();
        AgentRuntimeInfo info = new AgentRuntimeInfo(instance.getAgentIdentifier(), AgentRuntimeStatus.Idle, null, "cookie", null);
        when(remote.ping(info)).thenReturn(new AgentInstruction(false));

        handler.process(agent, new Message(Action.ping, info));

        verify(remote).ping(info);
        assertEquals(1, handler.connectedAgents().size());
        assertEquals(agent, handler.connectedAgents().get(instance.getUuid()));
        assertTrue(agent.messages.isEmpty());
    }

    @Test
    public void shouldCancelJobIfAgentRuntimeStatusIsCanceledOnSeverSideWhenClientPingsServer() {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, null);
        info.setCookie("cookie");

        when(remote.ping(info)).thenReturn(new AgentInstruction(true));

        handler.process(agent, new Message(Action.ping, info));

        verify(remote).ping(info);
        assertEquals(1, handler.connectedAgents().size());
        assertEquals(agent, handler.connectedAgents().get("uuid"));

        assertEquals(1, agent.messages.size());
        assertEquals(agent.messages.get(0).getAction(), Action.cancelJob);
    }

    @Test
    public void shouldSetCookieIfNoCookieFoundWhenAgentPingsServer() {
        AgentIdentifier identifier = new AgentIdentifier("HostName", "ipAddress", "uuid");
        AgentRuntimeInfo info = new AgentRuntimeInfo(identifier, AgentRuntimeStatus.Idle, null, null, null);

        when(remote.getCookie(identifier, info.getLocation())).thenReturn("new cookie");
        when(remote.ping(info)).thenReturn(new AgentInstruction(false));

        handler.process(agent, new Message(Action.ping, info));

        verify(remote).ping(info);
        assertEquals(1, agent.messages.size());
        assertEquals(agent.messages.get(0).getAction(), Action.setCookie);
        assertEquals(agent.messages.get(0).getData(), "new cookie");
    }

    @Test
    public void shouldSetCookieAndCancelJobWhenPingServerWithoutCookieAndServerSideRuntimeStatusIsCanceled() {
        AgentIdentifier identifier = new AgentIdentifier("HostName", "ipAddress", "uuid");
        AgentRuntimeInfo info = new AgentRuntimeInfo(identifier, AgentRuntimeStatus.Idle, null, null, null);

        when(remote.getCookie(identifier, info.getLocation())).thenReturn("new cookie");
        when(remote.ping(info)).thenReturn(new AgentInstruction(true));

        handler.process(agent, new Message(Action.ping, info));

        verify(remote).ping(info);
        assertEquals(2, agent.messages.size());
        assertEquals(agent.messages.get(0).getAction(), Action.setCookie);
        assertEquals(agent.messages.get(0).getData(), "new cookie");
        assertEquals(agent.messages.get(1).getAction(), Action.cancelJob);
    }

    @Test
    public void reportCurrentStatus() {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, null);

        JobIdentifier jobIdentifier = new JobIdentifier();
        handler.process(agent, new Message(Action.reportCurrentStatus, new Report(info, jobIdentifier, JobState.Preparing)));

        verify(remote).reportCurrentStatus(info, jobIdentifier, JobState.Preparing);
    }

    @Test
    public void reportCompleting() {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, null);

        JobIdentifier jobIdentifier = new JobIdentifier();
        handler.process(agent, new Message(Action.reportCompleting, new Report(info, jobIdentifier, JobResult.Passed)));

        verify(remote).reportCompleting(info, jobIdentifier, JobResult.Passed);
    }

    @Test
    public void reportCompleted() {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, null);

        JobIdentifier jobIdentifier = new JobIdentifier();
        handler.process(agent, new Message(Action.reportCompleted, new Report(info, jobIdentifier, JobResult.Passed)));

        verify(remote).reportCompleted(info, jobIdentifier, JobResult.Passed);
    }

    @Test
    public void shouldNotRaiseErrorIfRemovedAgentDidNotRegistered() {
        handler.remove(agent);
    }

    @Test
    public void removeRegisteredAgent() {
        AgentInstance instance = AgentInstanceMother.idle();
        AgentRuntimeInfo info = new AgentRuntimeInfo(instance.getAgentIdentifier(), AgentRuntimeStatus.Idle, null, null, null);
        when(remote.ping(info)).thenReturn(new AgentInstruction(false));
        when(agentService.findAgent(instance.getUuid())).thenReturn(instance);

        handler.process(agent, new Message(Action.ping, info));

        handler.remove(agent);
        assertEquals(0, handler.connectedAgents().size());
        assertEquals(AgentStatus.LostContact, instance.getStatus());
    }

    @Test
    public void sendCancelMessage() {
        AgentInstance instance = AgentInstanceMother.idle();
        AgentRuntimeInfo info = new AgentRuntimeInfo(instance.getAgentIdentifier(), AgentRuntimeStatus.Idle, null, null, null);
        when(remote.ping(info)).thenReturn(new AgentInstruction(false));
        handler.process(agent, new Message(Action.ping, info));

        agent.messages.clear();
        handler.sendCancelMessage(instance.getAgentIdentifier().getUuid());
        assertEquals(1, agent.messages.size());
    }

    @Test
    public void sendCancelMessageShouldNotErrorOutWhenGivenUUIDIsUnknown() {
        handler.sendCancelMessage(null);
        handler.sendCancelMessage("hello");
    }
}
