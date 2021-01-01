/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.remote;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.messaging.BuildRepositoryMessageProducer;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;

import static javax.servlet.http.HttpServletResponse.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class AgentRemoteInvokerServiceExporterTest {
    private static final String AGENT_UUID = "123-456-789";

    private MockHttpServletRequest req;

    private MockHttpServletResponse res;

    @Mock
    private BuildRepositoryMessageProducer target;

    @BeforeEach
    void setup() throws Exception {
        openMocks(this).close();
        req = new MockHttpServletRequest();
        req.addHeader("X-Agent-GUID", AGENT_UUID);
        res = new MockHttpServletResponse();
    }

    @Test
    void isIgnored_allowedForSameUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo(AGENT_UUID);
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("isIgnored", new Class[]{AgentRuntimeInfo.class, JobIdentifier.class}, new Object[]{agent, null}), target);
        invoker.handleRequest(req, res);
        verify(target, only()).isIgnored(agent, null);
        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void isIgnored_rejectedForDifferentUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo("other");
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("isIgnored", new Class[]{AgentRuntimeInfo.class, JobIdentifier.class}, new Object[]{agent, null}), target);
        invoker.handleRequest(req, res);
        verify(target, never()).isIgnored(any(AgentRuntimeInfo.class), any(JobIdentifier.class));
        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    void ping_allowedForSameUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo(AGENT_UUID);
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("ping", new Class[]{AgentRuntimeInfo.class}, new Object[]{agent}), target);
        invoker.handleRequest(req, res);
        verify(target, only()).ping(agent);
        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void ping_rejectedForDifferentUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo("other");
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("ping", new Class[]{AgentRuntimeInfo.class}, new Object[]{agent}), target);
        invoker.handleRequest(req, res);
        verify(target, never()).ping(any(AgentRuntimeInfo.class));
        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    void getWork_allowedForSameUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo(AGENT_UUID);
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("getWork", new Class[]{AgentRuntimeInfo.class}, new Object[]{agent}), target);
        invoker.handleRequest(req, res);
        verify(target, only()).getWork(agent);
        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void getWork_rejectedForDifferentUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo("other");
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("getWork", new Class[]{AgentRuntimeInfo.class}, new Object[]{agent}), target);
        invoker.handleRequest(req, res);
        verify(target, never()).getWork(any(AgentRuntimeInfo.class));
        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    void getCookie_allowedForSameUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo(AGENT_UUID);
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("getCookie", new Class[]{AgentRuntimeInfo.class}, new Object[]{agent}), target);
        invoker.handleRequest(req, res);
        verify(target, only()).getCookie(agent);
        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void getCookie_rejectedForDifferentUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo("other");
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("getCookie", new Class[]{AgentRuntimeInfo.class}, new Object[]{agent}), target);
        invoker.handleRequest(req, res);
        verify(target, never()).getCookie(any(AgentRuntimeInfo.class));
        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    void reportCurrentStatus_allowedForSameUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo(AGENT_UUID);
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("reportCurrentStatus", new Class[]{AgentRuntimeInfo.class, JobIdentifier.class, JobState.class}, new Object[]{agent, null, null}), target);
        invoker.handleRequest(req, res);
        verify(target, only()).reportCurrentStatus(agent, null, null);
        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void reportCurrentStatus_rejectedForDifferentUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo("other");
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("reportCurrentStatus", new Class[]{AgentRuntimeInfo.class, JobIdentifier.class, JobState.class}, new Object[]{agent, null, null}), target);
        invoker.handleRequest(req, res);
        verify(target, never()).reportCurrentStatus(any(AgentRuntimeInfo.class), any(JobIdentifier.class), any(JobState.class));
        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    void reportCompleting_allowedForSameUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo(AGENT_UUID);
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("reportCompleting", new Class[]{AgentRuntimeInfo.class, JobIdentifier.class, JobResult.class}, new Object[]{agent, null, null}), target);
        invoker.handleRequest(req, res);
        verify(target, only()).reportCompleting(agent, null, null);
        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void reportCompleting_rejectedForDifferentUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo("other");
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("reportCompleting", new Class[]{AgentRuntimeInfo.class, JobIdentifier.class, JobResult.class}, new Object[]{agent, null, null}), target);
        invoker.handleRequest(req, res);
        verify(target, never()).reportCompleting(any(AgentRuntimeInfo.class), any(JobIdentifier.class), any(JobResult.class));
        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    void reportCompleted_allowedForSameUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo(AGENT_UUID);
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("reportCompleted", new Class[]{AgentRuntimeInfo.class, JobIdentifier.class, JobResult.class}, new Object[]{agent, null, null}), target);
        invoker.handleRequest(req, res);
        verify(target, only()).reportCompleted(agent, null, null);
        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void reportCompleted_rejectedForDifferentUUID() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo("other");
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("reportCompleted", new Class[]{AgentRuntimeInfo.class, JobIdentifier.class, JobResult.class}, new Object[]{agent, null, null}), target);
        invoker.handleRequest(req, res);
        verify(target, never()).reportCompleted(any(AgentRuntimeInfo.class), any(JobIdentifier.class), any(JobResult.class));
        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    void rejectsUnknownMethod() throws Exception {
        final AgentRuntimeInfo agent = runtimeInfo(AGENT_UUID);
        final AgentRemoteInvokerServiceExporter invoker = deserializingWith(new RemoteInvocation("nonexistent", new Class[]{AgentRuntimeInfo.class}, new Object[]{agent}), target);
        invoker.handleRequest(req, res);
        verifyNoInteractions(target);
        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    private AgentRuntimeInfo runtimeInfo(String uuid) {
        return new AgentRuntimeInfo(identifier(uuid), null, null, null);
    }

    private AgentIdentifier identifier(String uuid) {
        return new AgentIdentifier(null, null, uuid);
    }

    private static AgentRemoteInvokerServiceExporter deserializingWith(final RemoteInvocation invocation, final BuildRepositoryMessageProducer proxy) {
        final AgentRemoteInvokerServiceExporter invoker = new AgentRemoteInvokerServiceExporter() {
            @Override
            protected Object getProxyForService() {
                return proxy;
            }

            @Override
            protected RemoteInvocation readRemoteInvocation(HttpServletRequest request) {
                return invocation;
            }

            @Override
            protected RemoteInvocationResult invokeAndCreateResult(RemoteInvocation invocation, Object targetObject) {
                try {
                    invocation.invoke(targetObject);
                    return new RemoteInvocationResult(true);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    fail("invoke() failed; should never get here. error: " + e.getMessage());
                    return new RemoteInvocationResult(false);
                }
            }

            @Override
            protected void writeRemoteInvocationResult(HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result) {
                if ((Boolean) result.getValue()) {
                    response.setStatus(SC_OK);
                } else {
                    response.setStatus(SC_INTERNAL_SERVER_ERROR);
                }
            }
        };
        invoker.prepare();
        return invoker;
    }
}
