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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ApiSessionFilterTest {
    public static final int INACTIVE_INTERVAL = 10;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private HttpSession session;
    private ApiSessionFilter filter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(systemEnvironment.get(SystemEnvironment.API_REQUEST_IDLE_TIMEOUT_IN_SECONDS)).thenReturn(INACTIVE_INTERVAL);
        filter = new ApiSessionFilter(systemEnvironment);
    }

    @Test
    public void shouldUseShortLivedSessionWhenSessionDoesNotExistForThisRequest() throws Exception {
        ensureNoSessionExistsBeforeReachingFilter();

        filter.doFilterHttp(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(session).setMaxInactiveInterval(INACTIVE_INTERVAL);
    }

    @Test
    public void shouldNotUseShortLivedSessionWhenSessionExistsForThisRequest() throws Exception {
        ensureSessionExistsBeforeReachingFilter();

        filter.doFilterHttp(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(session, times(0)).setMaxInactiveInterval(INACTIVE_INTERVAL);
    }

    private void ensureNoSessionExistsBeforeReachingFilter() {
        when(request.getSession(false)).thenReturn(null, session);
    }

    private void ensureSessionExistsBeforeReachingFilter() {
        when(request.getSession(false)).thenReturn(session, session);
    }
}
