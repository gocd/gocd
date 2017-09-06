/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.websocket.*;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class AgentRemoteHandlerTest {
    private AgentRemoteHandler handler;
    private BuildRepositoryRemote remote;
    private AgentService agentService;
    private AgentStub agent = new AgentStub();
    private ConsoleService consoleService;
    private TimeProvider timeProvider;

    @Before
    public void setUp() {
        timeProvider = mock(TimeProvider.class);
        remote = mock(BuildRepositoryRemote.class);
        agentService = mock(AgentService.class);
        consoleService = mock(ConsoleService.class);
        handler = new AgentRemoteHandler(remote, agentService, mock(JobInstanceService.class), consoleService);
    }

    @Test
    public void registerConnectedAgentsByPing() throws Exception {
        AgentInstance instance = AgentInstanceMother.idle();
        AgentRuntimeInfo info = new AgentRuntimeInfo(instance.getAgentIdentifier(), AgentRuntimeStatus.Idle, null, "cookie", false, timeProvider);
        when(remote.ping(info)).thenReturn(new AgentInstruction(false));

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        verify(remote).ping(info);
        assertEquals(1, handler.connectedAgents().size());
        assertEquals(agent, handler.connectedAgents().get(instance.getUuid()));
        assertTrue(agent.messages.isEmpty());
    }

    @Test
    public void shouldCancelJobIfAgentRuntimeStatusIsCanceledOnSeverSideWhenClientPingsServer() throws Exception {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, false, timeProvider);
        info.setCookie("cookie");

        when(remote.ping(info)).thenReturn(new AgentInstruction(true));

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        verify(remote).ping(info);
        assertEquals(1, handler.connectedAgents().size());
        assertEquals(agent, handler.connectedAgents().get("uuid"));

        assertEquals(1, agent.messages.size());
        assertEquals(agent.messages.get(0).getAction(), Action.cancelBuild);
    }

    @Test
    public void shouldCancelBuildIfAgentRuntimeStatusIsCanceledOnSeverSideWhenClientWithBuildCommandSupportPingsServer() throws Exception {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, true, timeProvider);
        info.setCookie("cookie");

        when(remote.ping(info)).thenReturn(new AgentInstruction(true));

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        verify(remote).ping(info);
        assertEquals(1, handler.connectedAgents().size());
        assertEquals(agent, handler.connectedAgents().get("uuid"));

        assertEquals(1, agent.messages.size());
        assertEquals(agent.messages.get(0).getAction(), Action.cancelBuild);
    }

    @Test
    public void shouldSetCookieIfNoCookieFoundWhenAgentPingsServer() throws Exception {
        AgentIdentifier identifier = new AgentIdentifier("HostName", "ipAddress", "uuid");
        AgentRuntimeInfo info = new AgentRuntimeInfo(identifier, AgentRuntimeStatus.Idle, null, null, false, timeProvider);

        when(remote.getCookie(identifier, info.getLocation())).thenReturn("new cookie");
        when(remote.ping(any(AgentRuntimeInfo.class))).thenReturn(new AgentInstruction(false));

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        verify(remote).ping(withCookie(info, "new cookie"));
        assertEquals(1, agent.messages.size());
        assertEquals(agent.messages.get(0).getAction(), Action.setCookie);
        assertEquals(MessageEncoding.decodeData(agent.messages.get(0).getData(), String.class), "new cookie");
    }

    @Test
    public void shouldSetCookieAndCancelJobWhenPingServerWithoutCookieAndServerSideRuntimeStatusIsCanceled() throws Exception {
        AgentIdentifier identifier = new AgentIdentifier("HostName", "ipAddress", "uuid");
        AgentRuntimeInfo info = new AgentRuntimeInfo(identifier, AgentRuntimeStatus.Idle, null, null, false, timeProvider);

        when(remote.getCookie(identifier, info.getLocation())).thenReturn("new cookie");
        when(remote.ping(any(AgentRuntimeInfo.class))).thenReturn(new AgentInstruction(true));

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        verify(remote).ping(withCookie(info, "new cookie"));
        assertEquals(2, agent.messages.size());
        assertEquals(agent.messages.get(0).getAction(), Action.setCookie);
        assertEquals(MessageEncoding.decodeData(agent.messages.get(0).getData(), String.class), "new cookie");
        assertEquals(agent.messages.get(1).getAction(), Action.cancelBuild);
    }

    private AgentRuntimeInfo withCookie(AgentRuntimeInfo info, String cookie) {
        AgentRuntimeInfo newInfo = MessageEncoding.decodeData(MessageEncoding.encodeData(info), AgentRuntimeInfo.class);
        newInfo.setCookie(cookie);
        return newInfo;
    }

    @Test
    public void reportCurrentStatus() throws Exception {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, false, timeProvider);

        JobIdentifier jobIdentifier = new JobIdentifier();
        handler.process(agent, new Message(Action.reportCurrentStatus, MessageEncoding.encodeData(new Report(info, jobIdentifier, JobState.Preparing))));

        verify(remote).reportCurrentStatus(info, jobIdentifier, JobState.Preparing);
    }

    @Test
    public void reportCompleting() throws Exception {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, false, timeProvider);

        JobIdentifier jobIdentifier = new JobIdentifier();
        handler.process(agent, new Message(Action.reportCompleting, MessageEncoding.encodeData(new Report(info, jobIdentifier, JobResult.Passed))));

        verify(remote).reportCompleting(info, jobIdentifier, JobResult.Passed);
    }

    @Test
    public void consoleOut() throws Exception {
        JobIdentifier jobIdentifier = new JobIdentifier();
        String consoleLine = "wubba lubba dub dub!!!!!";
        File consoleFile = new File("/some/dir");
        ConsoleTransmission msg = new ConsoleTransmission(null, consoleLine, jobIdentifier);
        when(consoleService.consoleLogFile(jobIdentifier)).thenReturn(consoleFile);

        handler.process(agent, new Message(Action.consoleOut, MessageEncoding.encodeData(msg)));
        verify(consoleService).consoleLogFile(eq(jobIdentifier));
        ArgumentCaptor<InputStream> arg = ArgumentCaptor.forClass(InputStream.class);
        verify(consoleService).updateConsoleLog(eq(consoleFile), arg.capture());
        assertThat(IOUtils.toString(arg.getValue()), containsString(consoleLine + "\n"));
    }

    @Test
    public void reportCompleted() throws Exception {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, false, timeProvider);

        JobIdentifier jobIdentifier = new JobIdentifier();
        handler.process(agent, new Message(Action.reportCompleted, MessageEncoding.encodeData(new Report(info, jobIdentifier, JobResult.Passed))));

        verify(remote).reportCompleted(info, jobIdentifier, JobResult.Passed);
    }

    @Test
    public void shouldNotRaiseErrorIfRemovedAgentDidNotRegistered() {
        handler.remove(agent);
    }

    @Test
    public void removeRegisteredAgent() throws Exception {
        AgentInstance instance = AgentInstanceMother.idle();
        AgentRuntimeInfo info = new AgentRuntimeInfo(instance.getAgentIdentifier(), AgentRuntimeStatus.Idle, null, null, false, timeProvider);
        when(remote.ping(any(AgentRuntimeInfo.class))).thenReturn(new AgentInstruction(false));
        when(remote.getCookie(instance.getAgentIdentifier(), info.getLocation())).thenReturn("new cookie");
        when(agentService.findAgent(instance.getUuid())).thenReturn(instance);

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        handler.remove(agent);
        assertEquals(0, handler.connectedAgents().size());
    }

    @Test
    public void sendCancelMessage() throws Exception {
        AgentInstance instance = AgentInstanceMother.idle();
        AgentRuntimeInfo info = new AgentRuntimeInfo(instance.getAgentIdentifier(), AgentRuntimeStatus.Idle, null, null, false, timeProvider);
        when(agentService.findAgentAndRefreshStatus(instance.getUuid())).thenReturn(instance);
        when(remote.ping(any(AgentRuntimeInfo.class))).thenReturn(new AgentInstruction(false));
        when(remote.getCookie(instance.getAgentIdentifier(), info.getLocation())).thenReturn("new cookie");
        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        agent.messages.clear();
        handler.sendCancelMessage(instance.getAgentIdentifier().getUuid());
        assertEquals(1, agent.messages.size());
    }

    @Test
    public void sendCancelMessageShouldNotErrorOutWhenGivenUUIDIsUnknown() {
        handler.sendCancelMessage(null);
        handler.sendCancelMessage("hello");
    }

    @Test
    public void shouldNotSetDupCookieForSameAgent() throws Exception {
        AgentInstance instance = AgentInstanceMother.idle();
        AgentRuntimeInfo info = new AgentRuntimeInfo(instance.getAgentIdentifier(), AgentRuntimeStatus.Idle, null, null, false, timeProvider);
        when(remote.ping(any(AgentRuntimeInfo.class))).thenReturn(new AgentInstruction(false));
        when(remote.getCookie(instance.getAgentIdentifier(), info.getLocation())).thenReturn("cookie");
        when(agentService.findAgent(instance.getUuid())).thenReturn(instance);

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));
        info.setCookie(null);

        reset(remote);
        when(remote.ping(any(AgentRuntimeInfo.class))).thenReturn(new AgentInstruction(false));
        when(remote.getCookie(instance.getAgentIdentifier(), info.getLocation())).thenReturn("new cookie");

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));
        verify(remote).ping(withCookie(info, "cookie"));

        info.setCookie(null);
        handler.remove(agent);

        reset(remote);
        when(remote.ping(any(AgentRuntimeInfo.class))).thenReturn(new AgentInstruction(false));
        when(remote.getCookie(instance.getAgentIdentifier(), info.getLocation())).thenReturn("new cookie");

        handler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));
        verify(remote).ping(withCookie(info, "new cookie"));
    }

    @Test
    public void shouldSendBackAnAcknowledgementMessageIfMessageHasAcknowledgementId() throws Exception {
        AgentInstance instance = AgentInstanceMother.idle();
        AgentRuntimeInfo info = new AgentRuntimeInfo(instance.getAgentIdentifier(), AgentRuntimeStatus.Idle, null, null, false, timeProvider);
        info.setCookie("cookie");
        agent.setIgnoreAcknowledgements(false);
        when(remote.ping(info)).thenReturn(new AgentInstruction(false));
        when(agentService.findAgent(instance.getUuid())).thenReturn(instance);

        Message msg = new Message(Action.ping, MessageEncoding.encodeData(info));
        handler.process(agent, msg);
        assertEquals(1, agent.messages.size());
        assertEquals(Action.acknowledge, agent.messages.get(0).getAction());
        assertEquals(msg.getAcknowledgementId(), MessageEncoding.decodeData(agent.messages.get(0).getData(), String.class));
    }
}
