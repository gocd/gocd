/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ModeAwareFilterTest {
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    private ModeAwareFilter filter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        filter = new ModeAwareFilter(systemEnvironment);
    }

    @Test
    public void shouldNotBlockNonGetRequestWhenInActiveState() throws Exception {
        when(request.getMethod()).thenReturn("get").thenReturn("post").thenReturn("put").thenReturn("delete");
        when(systemEnvironment.isServerActive()).thenReturn(true);

        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(4)).doFilter(request, response);
    }

    @Test
    public void shouldNotBlockGetOrHeadRequestWhenInPassiveState() throws Exception {
        when(request.getMethod()).thenReturn("get").thenReturn("head");
        when(systemEnvironment.isServerActive()).thenReturn(false);

        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
    }

    @Test
    public void shouldBlockNonGetRequestWhenInPassiveState() throws Exception {
        when(request.getMethod()).thenReturn("post").thenReturn("put").thenReturn("delete");
        when(systemEnvironment.isServerActive()).thenReturn(false);

        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void shouldAllowLoginPostRequestInPassiveState() throws Exception {
        when(request.getMethod()).thenReturn("post");
        when(systemEnvironment.isServerActive()).thenReturn(false);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        when(request.getRequestURI()).thenReturn("/go/auth/security_check");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldAllowSwitchToActiveStateChangePostRequestInPassiveState() throws Exception {
        when(request.getMethod()).thenReturn("post");
        when(systemEnvironment.isServerActive()).thenReturn(false);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        when(request.getRequestURI()).thenReturn("/go/api/state/active");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldRedirectToPassiveServerErrorPageForNonGetRequestWhenInPassiveState() throws Exception {
        when(request.getMethod()).thenReturn("post");
        when(systemEnvironment.isServerActive()).thenReturn(false);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).sendRedirect("/go/errors/inactive");
    }
}
