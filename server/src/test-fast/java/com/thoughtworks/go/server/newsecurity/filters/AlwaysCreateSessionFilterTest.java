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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AlwaysCreateSessionFilterTest {
    private HttpServletResponse response;
    private MockHttpServletRequest request;
    private AlwaysCreateSessionFilter alwaysCreateSessionFilter;

    @BeforeEach
    void setUp() throws Exception {
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        alwaysCreateSessionFilter = new AlwaysCreateSessionFilter();
    }

    @Test
    void shouldCreateASessionIfOneNotExist() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        assertThat(request.getSession(false)).isNull();

        alwaysCreateSessionFilter.doFilter(request, response, filterChain);

        assertThat(request.getSession(false)).isNotNull();
    }

    @Test
    void shouldNotCreateASessionIfOneExist() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        final HttpSession session = request.getSession(true);

        assertThat(session).isNotNull();

        alwaysCreateSessionFilter.doFilter(request, response, filterChain);

        assertThat(request.getSession(false)).isEqualTo(session);
    }
}
