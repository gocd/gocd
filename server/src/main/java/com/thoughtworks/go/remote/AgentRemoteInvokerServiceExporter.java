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
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.web.util.NestedServletException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static com.thoughtworks.go.util.SystemEnvironment.AGENT_EXTRA_PROPERTIES;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.*;

/**
 * Custom invoker service exporter that validates UUID authorization on agent requests. This prevents compromised agents
 * (or any other attack masquerading as an authenticated agent) from acting on behalf of another agent.
 */
public class AgentRemoteInvokerServiceExporter extends HttpInvokerServiceExporter {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AgentRemoteInvokerServiceExporter.class);
    private static final Set<MethodSignature> KNOWN_METHODS_NEEDING_UUID_VALIDATION = Set.of(
            new MethodSignature("ping", AgentRuntimeInfo.class),
            new MethodSignature("getWork", AgentRuntimeInfo.class),
            new MethodSignature("reportCurrentStatus", AgentRuntimeInfo.class, JobIdentifier.class, JobState.class),
            new MethodSignature("reportCompleting", AgentRuntimeInfo.class, JobIdentifier.class, JobResult.class),
            new MethodSignature("reportCompleted", AgentRuntimeInfo.class, JobIdentifier.class, JobResult.class),
            new MethodSignature("isIgnored", AgentRuntimeInfo.class, JobIdentifier.class),
            new MethodSignature("getCookie", AgentRuntimeInfo.class)
    );

    private final SystemEnvironment env;

    public AgentRemoteInvokerServiceExporter() {
        this(new SystemEnvironment());
    }

    public AgentRemoteInvokerServiceExporter(SystemEnvironment env) {
        this.env = env;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (rejectRMI()) {
            // yes, ideally, this should be short-circuited in the agent auth filter, but keeping this logic here has
            // some advantages:
            //   - it keeps all deprecated RMI logic in one place so it's easier to remove (just remove this class)
            //   - it's 100% reliable by virtue of its proximity to the RMI invocation code and can't be thwarted by
            //     some clever URI encoding to circumvent the uri path test that we would need to write at the filter
            //     level in order to selectively apply this logic to the RMI endpoint and not the JSON API endpoint
            reject(response, SC_GONE, "This RMI endpoint is disabled.");
            return;
        }

        try {
            RemoteInvocation invocation = readRemoteInvocation(request);

            if (!authorized(request, response, invocation)) {
                return;
            }

            RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
            writeRemoteInvocationResult(request, response, result);
        } catch (ClassNotFoundException ex) {
            throw new NestedServletException("Class not found during deserialization", ex);
        }
    }

    private boolean rejectRMI() {
        final String props = env.get(AGENT_EXTRA_PROPERTIES).toLowerCase();
        return !Arrays.asList(props.split("\\s+")).contains("gocd.agent.remoting.legacy=true");
    }

    /**
     * Verifies that the agent UUID from the deserialized payload matches the UUID permitted by the agent authentication
     * filter.
     *
     * @param request    the {@link HttpServletRequest}
     * @param response   the {@link HttpServletResponse}
     * @param invocation the deserialized {@link RemoteInvocation} payload
     * @return true if authorized; false otherwise
     * @throws IOException on error while writing a response back to the client
     */
    private boolean authorized(HttpServletRequest request, HttpServletResponse response, RemoteInvocation invocation) throws IOException {
        final String uuid = request.getHeader("X-Agent-GUID"); // should never be null since we passed the auth filter
        final MethodSignature current = new MethodSignature(invocation);

        LOG.debug(format("Checking authorization for agent [%s] on invocation: %s", uuid, invocation));

        if (KNOWN_METHODS_NEEDING_UUID_VALIDATION.contains(current)) {
            final String askingFor = AgentUUID.fromRuntimeInfo0(invocation.getArguments());

            if (!uuid.equals(askingFor)) {
                LOG.error(format("DENYING REQUEST: Agent [%s] is attempting a request on behalf of [%s]: %s", uuid, askingFor, invocation));
                reject(response, SC_FORBIDDEN, "Not allowing request on behalf of another agent");
                return false;
            }
        } else {
            LOG.error(format("DENYING REQUEST: Agent [%s] is requesting an unknown method invocation: %s", uuid, invocation));
            reject(response, SC_BAD_REQUEST, format("Unknown invocation: %s", invocation));
            return false;
        }

        LOG.debug(format("ALLOWING REQUEST: Agent [%s] is authorized to invoke: %s", uuid, invocation));
        return true;
    }

    /**
     * Returns a plaintext error response back to the agent on failure
     *
     * @param response   the {@link HttpServletResponse}
     * @param statusCode the HTTP status code
     * @param msg        the error message
     * @throws IOException on error while writing a response back to the client
     */
    private void reject(HttpServletResponse response, int statusCode, String msg) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("text/plain");
        final PrintWriter writer = response.getWriter();
        writer.println(msg);
        writer.flush();
        writer.close();
    }

    /**
     * Just a container class to hold functions to extract the agent UUID from deserialized payloads
     */
    private static class AgentUUID {
        private static String fromRuntimeInfo0(Object[] args) {
            return ((AgentRuntimeInfo) args[0]).getIdentifier().getUuid();
        }
    }

    /**
     * Helper class to make RMI method matching easier
     */
    private static class MethodSignature {
        private final String name;
        private final Class<?>[] paramTypes;

        private MethodSignature(RemoteInvocation invocation) {
            this(invocation.getMethodName(), invocation.getParameterTypes());
        }

        private MethodSignature(String name, Class<?>... paramTypes) {
            this.name = name;
            this.paramTypes = paramTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
            return Objects.equals(name, that.name) && Arrays.equals(paramTypes, that.paramTypes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(name);
            result = 31 * result + Arrays.hashCode(paramTypes);
            return result;
        }
    }
}
