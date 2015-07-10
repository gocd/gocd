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

package com.thoughtworks.go.server.util;

import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Response;

import javax.servlet.ServletResponseWrapper;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Jetty6ResponseTest {
    private Jetty6Response jetty6Response;
    private Response response;

    @Before
    public void setUp() throws Exception {
        response = mock(Response.class);
        jetty6Response = new Jetty6Response(response);
    }

    @Test
    public void shouldGetResponseStatus() {
        when(response.getStatus()).thenReturn(200);
        assertThat(jetty6Response.getStatus(), is(200));
    }

    @Test
    public void shouldGetResponseContentCount() {
        when(response.getContentCount()).thenReturn(2000l);
        assertThat(jetty6Response.getContentCount(), is(2000l));
    }

    @Test
    public void shouldHandleWrappedResponse() throws Exception {
        ServletResponseWrapper wrappedResponse = mock(ServletResponseWrapper.class);
        when(wrappedResponse.getResponse()).thenReturn(response);
        when(response.getContentCount()).thenReturn(2000l);

        assertThat(jetty6Response.getContentCount(), is(2000l));
    }
}