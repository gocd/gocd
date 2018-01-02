/************************* GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END *********************************/

package com.thoughtworks.go.server.web;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TokenInterceptorTest {

    private TokenManager manager;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        manager = mock(TokenManager.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(request);
        verifyNoMoreInteractions(response);
        verifyNoMoreInteractions(manager);
    }

    @Test
    public void shouldCreateSessionTokenForNonPOSTRequest() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        MockHttpSession session = new MockHttpSession();
        when(request.getSession()).thenReturn(session);

        TokenInterceptor interceptor = new TokenInterceptor(manager);

        assertThat(interceptor.preHandle(request, response, mock(Object.class)), is(true));

        verify(request).getMethod();
        verify(manager).create(session);
        verify(request).getSession();
    }

    @Test
    public void shouldVerifyAPOSTRequest() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(manager.verify(request)).thenReturn(true);

        TokenInterceptor interceptor = new TokenInterceptor(manager);
        assertThat(interceptor.preHandle(request, response, mock(Object.class)), is(true));

        verify(request).getMethod();
        verify(manager).verify(request);
    }

    @Test
    public void shouldSetErrorResponseForUnverifiedPOSTRequest() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(manager.verify(request)).thenReturn(false);

        TokenInterceptor interceptor = new TokenInterceptor(manager);
        assertThat(interceptor.preHandle(request, response, mock(Object.class)), is(false));

        verify(request).getMethod();
        verify(manager).verify(request);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Bad or missing Token in the POST request");
    }
}