/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class AgentStatusHttpdTest {

    @Mock
    private AgentHealthHolder agentHealthHolder;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private NanoHTTPD.IHTTPSession session;
    private AgentStatusHttpd agentStatusHttpd;

    @BeforeEach
    void setUp() {
        initMocks(this);
        this.agentStatusHttpd = new AgentStatusHttpd(systemEnvironment, new IsConnectedToServerV1(agentHealthHolder));
    }

    @Test
    void shouldReturnMethodNotAllowedOnNonGetNonHeadRequests() throws Exception {
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.POST);
        NanoHTTPD.Response response = this.agentStatusHttpd.serve(session);
        assertThat(response.getStatus()).isEqualTo(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED);
        assertThat(response.getMimeType()).isEqualTo("text/plain; charset=utf-8");
        assertThat(IOUtils.toString(response.getData(), StandardCharsets.UTF_8)).isEqualTo("This method is not allowed. Please use GET or HEAD.");
    }

    @Test
    void shouldReturnNotFoundForBadUrl() throws Exception {
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.GET);
        when(session.getUri()).thenReturn("/foo");

        NanoHTTPD.Response response = this.agentStatusHttpd.serve(session);
        assertThat(response.getStatus()).isEqualTo(NanoHTTPD.Response.Status.NOT_FOUND);
        assertThat(response.getMimeType()).isEqualTo("text/plain; charset=utf-8");
        assertThat(IOUtils.toString(response.getData(), StandardCharsets.UTF_8)).isEqualTo("The page you requested was not found");
    }

    @Test
    void shouldRouteToIsConnectedToServerHandler() throws Exception {
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.GET);
        when(session.getUri()).thenReturn("/health/latest/isConnectedToServer");
        when(agentHealthHolder.hasLostContact()).thenReturn(false);

        NanoHTTPD.Response response = this.agentStatusHttpd.serve(session);
        assertThat(response.getStatus()).isEqualTo(NanoHTTPD.Response.Status.OK);
        assertThat(response.getMimeType()).isEqualTo("text/plain; charset=utf-8");
        assertThat(IOUtils.toString(response.getData(), StandardCharsets.UTF_8)).isEqualTo("OK!");
    }

    @Test
    void shouldRouteToIsConnectedToServerV1Handler() throws Exception {
        when(session.getMethod()).thenReturn(NanoHTTPD.Method.GET);
        when(session.getUri()).thenReturn("/health/v1/isConnectedToServer");
        when(agentHealthHolder.hasLostContact()).thenReturn(false);

        NanoHTTPD.Response response = this.agentStatusHttpd.serve(session);
        assertThat(response.getStatus()).isEqualTo(NanoHTTPD.Response.Status.OK);
        assertThat(response.getMimeType()).isEqualTo("text/plain; charset=utf-8");
        assertThat(IOUtils.toString(response.getData(), StandardCharsets.UTF_8)).isEqualTo("OK!");
    }

    @Test
    void shouldNotInitializeServerIfSettingIsTurnedOff() throws Exception {
        when(systemEnvironment.getAgentStatusEnabled()).thenReturn(true);
        AgentStatusHttpd spy = spy(agentStatusHttpd);
        doThrow(new RuntimeException("This is not expected to be invoked")).when(spy).start();
        spy.init();
    }

    @Test
    void shouldInitializeServerIfSettingIsTurnedOn() throws Exception {
        when(systemEnvironment.getAgentStatusEnabled()).thenReturn(true);
        AgentStatusHttpd spy = spy(agentStatusHttpd);
        spy.init();
        verify(spy).start();
    }

    @Test
    void initShouldNotBlowUpIfServerDoesNotStart() throws Exception {
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