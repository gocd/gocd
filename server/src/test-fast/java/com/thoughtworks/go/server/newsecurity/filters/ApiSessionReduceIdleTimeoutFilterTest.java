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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ApiSessionReduceIdleTimeoutFilterTest {

    private FilterChain filterChain;

    @BeforeEach
    public void setUp() throws Exception {
        filterChain = mock(FilterChain.class);
    }

    @Test
    public void shouldReduceSessionTimeoutWhenAppliedAndNewSessionIsCreatedAfterApplyingFilter() throws IOException, ServletException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);

        when(systemEnvironment.get(SystemEnvironment.API_REQUEST_IDLE_TIMEOUT_IN_SECONDS)).thenReturn(10);

        assertThat(request.getSession(false)).isNull();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                request.getSession(true);
                return null;
            }
        }).when(filterChain).doFilter(request, response);

        new ApiSessionReduceIdleTimeoutFilter(systemEnvironment).doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(request.getSession(false).getMaxInactiveInterval()).isEqualTo(10);
        assertThat(request.getSession(false)).isNotNull();
    }

    @Test
    public void shouldNotReduceSessionMaxInactiveIntervalWhenSessionAlreadyExist() throws IOException, ServletException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);

        assertThat(request.getSession()).isNotNull();
        when(systemEnvironment.get(SystemEnvironment.API_REQUEST_IDLE_TIMEOUT_IN_SECONDS)).thenReturn(10);

        new ApiSessionReduceIdleTimeoutFilter(systemEnvironment).doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(request.getSession(false).getMaxInactiveInterval()).isEqualTo(0);
    }
}
