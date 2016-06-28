/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TokenManagerTest {

    private final String TOKEN = "post.token";

    private TokenManager manager;
    private HttpSession session;
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        manager = new TokenManager();
        session = mock(HttpSession.class);
        request = mock(HttpServletRequest.class);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(session);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void shouldCreateAPostTokenIntheSessionIfNotAvailable() throws Exception {
        when(session.getAttribute(TOKEN)).thenReturn(null);

        manager.create(session);

        verify(session).getAttribute(TOKEN);
        verify(session).setAttribute(eq(TOKEN), anyString());
    }

    @Test
    public void shouldNotCreateAPostTokenIfItsAvailable() throws Exception {
        when(session.getAttribute(TOKEN)).thenReturn("token");

        manager.create(session);

        verify(session).getAttribute(TOKEN);
    }

    @Test
    public void shouldVerifyIfRequestTokenIsAvailable() throws Exception {
        when(request.getParameter(TOKEN)).thenReturn(null);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(TOKEN)).thenReturn(null);

        assertThat(manager.verify(request), is(false));

        verify(request).getParameter(TOKEN);
        verify(request).getSession();
        verify(session).getAttribute(TOKEN);
    }

    @Test
    public void shouldVerifyIfSessionTokenIsAvailable() throws Exception {
        when(request.getParameter(TOKEN)).thenReturn("token");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(TOKEN)).thenReturn(null);

        assertThat(manager.verify(request), is(false));

        verify(request).getParameter(TOKEN);
        verify(request).getSession();
        verify(session).getAttribute(TOKEN);
    }

    @Test
    public void shouldVerifyIfSessionTokenAndPostTokenAreDifferent() throws Exception {
        when(request.getParameter(TOKEN)).thenReturn("random.token");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(TOKEN)).thenReturn("token");

        assertThat(manager.verify(request), is(false));

        verify(request).getParameter(TOKEN);
        verify(request).getSession();
        verify(session).getAttribute(TOKEN);
    }

    @Test
    public void shouldVerifyIfSessionTokenAndPostTokenAreSame() throws Exception {
        when(request.getParameter(TOKEN)).thenReturn("token");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(TOKEN)).thenReturn("token");

        assertThat(manager.verify(request), is(true));

        verify(request).getParameter(TOKEN);
        verify(request).getSession();
        verify(session).getAttribute(TOKEN);
    }
}
