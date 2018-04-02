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
import com.thoughtworks.go.server.newsecurity.authentication.filters.ApiSessionReduceIdleTimeoutFilter;
import com.thoughtworks.go.server.newsecurity.authentication.mocks.MockHttpServletRequest;
import com.thoughtworks.go.server.newsecurity.authentication.mocks.MockHttpServletResponse;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class CreateSessionFilterChainTest {
    private ApiSessionReduceIdleTimeoutFilter apiSessionReduceIdleTimeoutFilter;
    private AlwaysCreateSessionFilter alwaysCreateSessionFilter;
    private HttpServletResponse response;
    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() throws Exception {
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        apiSessionReduceIdleTimeoutFilter = spy(new ApiSessionReduceIdleTimeoutFilter(new SystemEnvironment()));
        alwaysCreateSessionFilter = spy(new AlwaysCreateSessionFilter());
    }

    @Nested
    class CCTray extends UrlTest {

        @Override
        protected String url() {
            return "/cctray.xml";
        }
    }

    @Nested
    class API extends UrlTest {

        @Override
        protected String url() {
            return "/api/foo";
        }
    }

    @Nested
    class EverythingElse {

        private static final String URL = "/foobar";

        @Test
        public void shouldCreateASessionIfOneNotExistForEverythingElse() throws ServletException, IOException {
            request.setServletPath(URL);
            FilterChain filterChain = mock(FilterChain.class);

            assertThat(request.getSession(false)).isNull();

            new CreateSessionFilterChain(apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter).doFilter(request, response, filterChain);

            assertThat(request.getSession(false)).isNotNull();
            assertThat(request.getSession(false).getMaxInactiveInterval()).isZero();
        }

        @Test
        public void shouldNotCreateASessionIfOneExistForEverythingElse() throws ServletException, IOException {
            request.setServletPath(URL);
            FilterChain filterChain = mock(FilterChain.class);

            final HttpSession session = request.getSession(true);
            assertThat(request.getSession(false).getMaxInactiveInterval()).isZero();

            assertThat(session).isNotNull();

            new CreateSessionFilterChain(apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter).doFilter(request, response, filterChain);

            assertThat(request.getSession(false)).isEqualTo(session);
            assertThat(request.getSession(false).getMaxInactiveInterval()).isZero();
        }

    }

    private abstract class UrlTest {
        @Test
        public void shouldCreateASessionIfOneNotExist() throws ServletException, IOException {
            request.setServletPath(url());
            FilterChain filterChain = mock(FilterChain.class);

            assertThat(request.getSession(false)).isNull();

            new CreateSessionFilterChain(apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter).doFilter(request, response, filterChain);

            assertThat(request.getSession(false)).isNotNull();
            assertThat(request.getSession(false).getMaxInactiveInterval()).isEqualTo(new SystemEnvironment().get(SystemEnvironment.API_REQUEST_IDLE_TIMEOUT_IN_SECONDS));
        }

        @Test
        public void shouldNotCreateASessionIfOneExist() throws ServletException, IOException {
            request.setServletPath(url());
            FilterChain filterChain = mock(FilterChain.class);

            final HttpSession session = request.getSession(true);
            assertThat(request.getSession(false).getMaxInactiveInterval()).isZero();

            assertThat(session).isNotNull();

            new CreateSessionFilterChain(apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter).doFilter(request, response, filterChain);

            assertThat(request.getSession(false)).isEqualTo(session);
            assertThat(request.getSession(false).getMaxInactiveInterval()).isZero();
        }

        protected abstract String url();
    }

}
