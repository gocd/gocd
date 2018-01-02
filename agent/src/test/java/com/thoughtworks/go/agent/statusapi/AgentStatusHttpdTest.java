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

package com.thoughtworks.go.agent.statusapi;

import com.thoughtworks.go.util.SystemEnvironment;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AgentStatusHttpdTest {

    @Mock
    private AgentHealthHolder agentHealthHolder;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private NanoHTTPD.IHTTPSession session;
    private AgentStatusHttpd agentStatusHttpd;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        this.agentStatusHttpd = new AgentStatusHttpd(systemEnvironment, new IsConnectedToServerV1(agentHealthHolder));
    }

    @Test
    public void shouldReturnMethodNotAllowedOnNonGetNonHeadRequests() throws Exception {
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.POST);
        NanoHTTPD.Response response = this.agentStatusHttpd.serve(session);
        assertThat(response.getStatus(), is(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED));
        assertThat(response.getMimeType(), is("text/plain; charset=utf-8"));
        assertThat(IOUtils.toString(response.getData(), StandardCharsets.UTF_8), is("This method is not allowed. Please use GET or HEAD."));
    }

    @Test
    public void shouldReturnNotFoundForBadUrl() throws Exception {
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.GET);
        when(session.getUri()).thenReturn("/foo");

        NanoHTTPD.Response response = this.agentStatusHttpd.serve(session);
        assertThat(response.getStatus(), is(NanoHTTPD.Response.Status.NOT_FOUND));
        assertThat(response.getMimeType(), is("text/plain; charset=utf-8"));
        assertThat(IOUtils.toString(response.getData(), StandardCharsets.UTF_8), is("The page you requested was not found"));
    }

    @Test
    public void shouldRouteToIsConnectedToServerHandler() throws Exception {
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.GET);
        when(session.getUri()).thenReturn("/health/latest/isConnectedToServer");
        when(agentHealthHolder.hasLostContact()).thenReturn(false);

        NanoHTTPD.Response response = this.agentStatusHttpd.serve(session);
        assertThat(response.getStatus(), is(NanoHTTPD.Response.Status.OK));
        assertThat(response.getMimeType(), is("text/plain; charset=utf-8"));
        assertThat(IOUtils.toString(response.getData(), StandardCharsets.UTF_8), is("OK!"));
    }

    @Test
    public void shouldRouteToIsConnectedToServerV1Handler() throws Exception {
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.GET);
        when(session.getUri()).thenReturn("/health/v1/isConnectedToServer");
        when(agentHealthHolder.hasLostContact()).thenReturn(false);

        NanoHTTPD.Response response = this.agentStatusHttpd.serve(session);
        assertThat(response.getStatus(), is(NanoHTTPD.Response.Status.OK));
        assertThat(response.getMimeType(), is("text/plain; charset=utf-8"));
        assertThat(IOUtils.toString(response.getData(), StandardCharsets.UTF_8), is("OK!"));
    }

    @Test
    public void shouldNotInitializeServerIfSettingIsTurnedOff() throws Exception {
        when(systemEnvironment.getAgentStatusEnabled()).thenReturn(true);
        AgentStatusHttpd spy = spy(agentStatusHttpd);
        doThrow(new RuntimeException("This is not expected to be invoked")).when(spy).start();
        spy.init();
    }

    @Test
    public void shouldInitializeServerIfSettingIsTurnedOn() throws Exception {
        when(systemEnvironment.getAgentStatusEnabled()).thenReturn(true);
        AgentStatusHttpd spy = spy(agentStatusHttpd);
        spy.init();
        verify(spy).start();
    }

    @Test
    public void initShouldNotBlowUpIfServerDoesNotStart() throws Exception {
        when(systemEnvironment.getAgentStatusEnabled()).thenReturn(true);
        AgentStatusHttpd spy = spy(agentStatusHttpd);
        doThrow(new RuntimeException("Server had a problem starting up!")).when(spy).start();
        try {
            spy.init();
        } catch (Exception e) {
            fail("Did not expect exception!");
        }
    }
}