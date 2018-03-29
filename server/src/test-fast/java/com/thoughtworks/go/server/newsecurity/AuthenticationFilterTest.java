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

package com.thoughtworks.go.server.newsecurity;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AuthenticationFilterTest {
    private final HttpServletRequest request = new MockHttpServletRequest();
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    public void shouldCreateASessionIfOneNotExist() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        assertThat(request.getSession(false)).isNull();

        new OldAuthenticationFilter(pluginAuthenticationProvider, goConfigService).doFilter(request, response, filterChain);

        assertThat(request.getSession(false)).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldNotCreateASessionIfOneExist() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        final HttpSession session = request.getSession(true);
        assertThat(session).isNotNull();

        new OldAuthenticationFilter(pluginAuthenticationProvider, goConfigService).doFilter(request, response, filterChain);

        assertThat(request.getSession(false)).isEqualTo(session);
        verify(filterChain).doFilter(request, response);
    }
}