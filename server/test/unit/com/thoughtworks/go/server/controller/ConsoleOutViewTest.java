/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.controller;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

public class ConsoleOutViewTest {

    private ConsoleOutView content;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private PrintWriter writer;

    @Before
    public void setUp() throws Exception {
        content = new ConsoleOutView(200, "content");
        response = mock(HttpServletResponse.class);
        request = mock(HttpServletRequest.class);
        writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    public void shouldSetTheCorrectContentTypeWhileRendering() throws Exception {
        content.render(null, request, response);
        verify(response).setContentType("text/plain; charset=utf-8");
    }

    @Test
    public void shouldSetXJSONHeaderWithOffset() throws Exception {
        content.render(null, request, response);
        verify(response).addHeader("X-JSON", "[200]");
    }

    @Test
    public void shouldWriteContentToResponse() throws Exception {
        content.render(null, request, response);
        verify(writer).write("content");
        verify(writer).close();
    }
}