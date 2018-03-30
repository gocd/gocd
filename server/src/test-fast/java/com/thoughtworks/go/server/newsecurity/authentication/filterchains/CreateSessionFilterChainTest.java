/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.newsecurity.authentication.filterchains;

import com.thoughtworks.go.server.newsecurity.authentication.filters.AlwaysCreateSessionFilter;
import com.thoughtworks.go.server.newsecurity.authentication.mocks.MockHttpServletRequest;
import com.thoughtworks.go.server.newsecurity.authentication.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.authentication.filters.ApiSessionReduceIdleTimeoutFilter;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class CreateSessionFilterChainTest {
    private ApiSessionReduceIdleTimeoutFilterStub apiSessionReduceIdleTimeoutFilter;
    private AlwaysCreateSessionFilterStub alwaysCreateSessionFilter;
    private HttpServletResponse response;
    private MockHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        apiSessionReduceIdleTimeoutFilter = new ApiSessionReduceIdleTimeoutFilterStub();
        alwaysCreateSessionFilter = new AlwaysCreateSessionFilterStub();
    }

    @Test
    public void shouldCreateASessionIfOneNotExist() throws ServletException, IOException {
        request.setServletPath("/cctray.xml");
        FilterChain filterChain = mock(FilterChain.class);

        assertThat(request.getSession(false)).isNull();
        assertFalse(apiSessionReduceIdleTimeoutFilter.isDoFilterCalled());
        assertFalse(alwaysCreateSessionFilter.isDoFilterCalled());

        new CreateSessionFilterChain(apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .doFilter(request, response, filterChain);

        assertThat(request.getSession(false)).isNotNull();
        assertTrue(apiSessionReduceIdleTimeoutFilter.isDoFilterCalled());
        assertTrue(alwaysCreateSessionFilter.isDoFilterCalled());
    }

    @Test
    public void shouldNotCreateASessionIfOneExist() throws ServletException, IOException {
        request.setServletPath("/cctray.xml");
        FilterChain filterChain = mock(FilterChain.class);

        final HttpSession session = request.getSession(true);

        assertThat(session).isNotNull();
        assertFalse(apiSessionReduceIdleTimeoutFilter.isDoFilterCalled());
        assertFalse(alwaysCreateSessionFilter.isDoFilterCalled());

        new CreateSessionFilterChain(apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .doFilter(request, response, filterChain);

        assertThat(request.getSession(false)).isEqualTo(session);
        assertTrue(apiSessionReduceIdleTimeoutFilter.isDoFilterCalled());
        assertTrue(alwaysCreateSessionFilter.isDoFilterCalled());
    }

    class ApiSessionReduceIdleTimeoutFilterStub extends ApiSessionReduceIdleTimeoutFilter {
        boolean isDoFilterCalled;

        public ApiSessionReduceIdleTimeoutFilterStub() {
            super(new SystemEnvironment());
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            super.doFilterInternal(request, response, chain);
            isDoFilterCalled = true;
        }

        public boolean isDoFilterCalled() {
            return isDoFilterCalled;
        }
    }

    class AlwaysCreateSessionFilterStub extends AlwaysCreateSessionFilter {
        boolean isDoFilterCalled;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            super.doFilterInternal(request, response, chain);
            isDoFilterCalled = true;
        }

        public boolean isDoFilterCalled() {
            return isDoFilterCalled;
        }
    }
}