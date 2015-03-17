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
import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.Request;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Jetty6RequestTest {
    private Jetty6Request jettyRequest;
    private Request request;

    @Before
    public void setUp() throws Exception {
        request = mock(Request.class);
        jettyRequest = new Jetty6Request(request);
        when(request.getUri()).thenReturn(new HttpURI("foo/bar/baz"));
        when(request.getRootURL()).thenReturn(new StringBuffer("http://junk/"));
    }

    @Test
    public void shouldGetUrl() {
        assertThat(jettyRequest.getUrl(), is("http://junk/foo/bar/baz"));
    }

    @Test
    public void shouldGetUriPath() {
        assertThat(jettyRequest.getUriPath(), is("foo/bar/baz"));
    }

    @Test
    public void shouldGetUriAsString() {
        assertThat(jettyRequest.getUriAsString(), is("foo/bar/baz"));
    }

}