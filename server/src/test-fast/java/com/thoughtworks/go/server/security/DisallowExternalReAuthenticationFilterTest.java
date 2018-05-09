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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.ClearSingleton;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class DisallowExternalReAuthenticationFilterTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private DisallowExternalReAuthenticationFilter filter;

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        filter = new DisallowExternalReAuthenticationFilter();
    }

    @Test
    public void shouldDisallowAuthenticatedUsersFromAccessingTheLoginPage() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(null, null, null));
        when(request.getRequestURI()).thenReturn("/go/auth/login");

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("/");
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void shouldDisallowAuthenticatedUsersFromMakingAReAuthenticationRequest() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(null, null, null));
        when(request.getRequestURI()).thenReturn("/go/auth/security_check");

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("/");
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void shouldDisallowAuthenticatedUserFromMakingAPluginLoginRequest() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(null, null, null));
        when(request.getRequestURI()).thenReturn("/go/plugin/my_auth_plugin/login");

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("/");
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void shouldDisallowAuthenticatedUserFromMakingAPluginAuthenticationRequest() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(null, null, null));
        when(request.getRequestURI()).thenReturn("/go/plugin/my_auth_plugin/authenticate");

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("/");
        verifyNoMoreInteractions(filterChain);
    }
}