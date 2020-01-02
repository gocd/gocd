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
package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.filters.AgentSessionReduceIdleTimeoutFilter;
import com.thoughtworks.go.server.newsecurity.filters.AlwaysCreateSessionFilter;
import com.thoughtworks.go.server.newsecurity.filters.ApiSessionReduceIdleTimeoutFilter;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class CreateSessionFilterChainTest {
    private ApiSessionReduceIdleTimeoutFilter apiSessionReduceIdleTimeoutFilter;
    private AgentSessionReduceIdleTimeoutFilter agentSessionReduceIdleTimeoutFilter;
    private AlwaysCreateSessionFilter alwaysCreateSessionFilter;
    private HttpServletResponse response;
    private MockHttpServletRequest request;

    static Stream<Arguments> urlsAndTimeouts() {
        return Stream.of(
                Arguments.of("/cctray.xml", new SystemEnvironment().get(SystemEnvironment.API_REQUEST_IDLE_TIMEOUT_IN_SECONDS)),
                Arguments.of("/api/foo", new SystemEnvironment().get(SystemEnvironment.API_REQUEST_IDLE_TIMEOUT_IN_SECONDS)),
                Arguments.of("/admin/agent", new SystemEnvironment().get(SystemEnvironment.AGENT_REQUEST_IDLE_TIMEOUT_IN_SECONDS)),
                Arguments.of("/admin/agent-plugins.zip", new SystemEnvironment().get(SystemEnvironment.AGENT_REQUEST_IDLE_TIMEOUT_IN_SECONDS)),
                Arguments.of("/admin/tfs-impl.jar", new SystemEnvironment().get(SystemEnvironment.AGENT_REQUEST_IDLE_TIMEOUT_IN_SECONDS)),
                Arguments.of("/admin/agent/token", new SystemEnvironment().get(SystemEnvironment.AGENT_REQUEST_IDLE_TIMEOUT_IN_SECONDS)),
                Arguments.of("/admin/latest-agent.status", new SystemEnvironment().get(SystemEnvironment.AGENT_REQUEST_IDLE_TIMEOUT_IN_SECONDS)),
                Arguments.of("/foobar", 0)
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        apiSessionReduceIdleTimeoutFilter = spy(new ApiSessionReduceIdleTimeoutFilter(new SystemEnvironment()));
        agentSessionReduceIdleTimeoutFilter = spy(new AgentSessionReduceIdleTimeoutFilter(new SystemEnvironment()));
        alwaysCreateSessionFilter = spy(new AlwaysCreateSessionFilter());
    }

    @ParameterizedTest
    @MethodSource("urlsAndTimeouts")
    void shouldCreateASessionIfOneNotExist(String url, int timeout) throws ServletException, IOException {
        request.setServletPath(url);
        FilterChain filterChain = mock(FilterChain.class);

        assertThat(request.getSession(false)).isNull();

        new CreateSessionFilterChain(apiSessionReduceIdleTimeoutFilter, agentSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter).doFilter(request, response, filterChain);

        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getMaxInactiveInterval()).isEqualTo(timeout);
    }

    @ParameterizedTest
    @MethodSource("urlsAndTimeouts")
    void shouldNotCreateASessionIfOneExist(String url, int timeout) throws ServletException, IOException {
        request.setServletPath(url);
        FilterChain filterChain = mock(FilterChain.class);

        final HttpSession session = request.getSession(true);
        assertThat(request.getSession(false).getMaxInactiveInterval()).isZero();

        assertThat(session).isNotNull();

        new CreateSessionFilterChain(apiSessionReduceIdleTimeoutFilter, agentSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter).doFilter(request, response, filterChain);

        assertThat(request.getSession(false)).isEqualTo(session);
        assertThat(request.getSession(false).getMaxInactiveInterval()).isZero();
    }

}
