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

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgentRemoteHandlerTest implements Agent {
    private AgentRemoteHandler handler;
    private BuildRepositoryRemote remote;
    private List<Action> sendActions = new ArrayList<Action>();

    @Before
    public void setUp() {
        remote = mock(BuildRepositoryRemote.class);
        handler = new AgentRemoteHandler(remote);
    }

    @Test
    public void registerAgentByPing() {
        AgentRuntimeInfo info = AgentRuntimeInfo.fromAgent(new AgentIdentifier("HostName", "ipAddress", "uuid"));
        info.setCookie("cookie");
        when(remote.ping(info)).thenReturn(new AgentInstruction(false));

        handler.process(this, new Ping(info));

        verify(remote).ping(info);
        assertEquals(1, handler.connectedAgents().size());
        assertEquals(this, handler.connectedAgents().get("uuid"));
        assertTrue(sendActions.isEmpty());
    }

    @Test
    public void shouldCancelJobIfAgentRuntimeStatusIsCanceledOnSeverSideWhenClientPingsServer() {
        AgentRuntimeInfo info = AgentRuntimeInfo.fromAgent(new AgentIdentifier("HostName", "ipAddress", "uuid"));
        info.setCookie("cookie");
        when(remote.ping(info)).thenReturn(new AgentInstruction(true));

        handler.process(this, new Ping(info));

        verify(remote).ping(info);
        assertEquals(1, handler.connectedAgents().size());
        assertEquals(this, handler.connectedAgents().get("uuid"));

        assertEquals(1, sendActions.size());
        assertEquals(sendActions.get(0).getClass(), CancelJob.class);
    }

    @Test
    public void shouldSetCookieIfNoCookieFoundWhenAgentPingsServer() {
        AgentIdentifier identifier = new AgentIdentifier("HostName", "ipAddress", "uuid");
        AgentRuntimeInfo info = AgentRuntimeInfo.fromAgent(identifier);
        when(remote.getCookie(identifier, info.getLocation())).thenReturn("new cookie");
        when(remote.ping(info)).thenReturn(new AgentInstruction(false));

        handler.process(this, new Ping(info));

        verify(remote).ping(info);
        verify(remote).getCookie(identifier, info.getLocation());
        assertEquals(1, sendActions.size());
        assertEquals(sendActions.get(0).getClass(), SetCookie.class);
        assertEquals(sendActions.get(0).data(), "new cookie");
    }

    @Test
    public void shouldSetCookieAndCancelJobWhenPingServerWithoutCookieAndServerSideRuntimeStatusIsCanceled() {
        AgentIdentifier identifier = new AgentIdentifier("HostName", "ipAddress", "uuid");
        AgentRuntimeInfo info = AgentRuntimeInfo.fromAgent(identifier);
        when(remote.getCookie(identifier, info.getLocation())).thenReturn("new cookie");
        when(remote.ping(info)).thenReturn(new AgentInstruction(true));

        handler.process(this, new Ping(info));

        verify(remote).ping(info);
        assertEquals(2, sendActions.size());
        assertEquals(sendActions.get(0).getClass(), SetCookie.class);
        assertEquals(sendActions.get(0).data(), "new cookie");
        assertEquals(sendActions.get(1).getClass(), CancelJob.class);
    }

    @Override
    public boolean send(Action action) {
        sendActions.add(action);
        return true;
    }
}
