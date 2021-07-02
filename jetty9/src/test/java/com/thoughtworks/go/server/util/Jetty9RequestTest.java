/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.util;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Jetty9RequestTest {
    private Jetty9Request jetty9Request;
    @Mock(lenient = true)
    private Request request;

    @BeforeEach
    public void setUp() throws Exception {
        jetty9Request = new Jetty9Request(request);
        when(request.getHttpURI()).thenReturn(new HttpURI("foo/bar/baz"));
        when(request.getRootURL()).thenReturn(new StringBuilder("http://junk/"));
    }

    @Test
    public void shouldGetUrl() {
        assertThat(jetty9Request.getUrl(), is("http://junk/foo/bar/baz"));
    }

    @Test
    public void shouldGetUriPath() {
        assertThat(jetty9Request.getUriPath(), is("foo/bar/baz"));
    }

    @Test
    public void shouldGetUriAsString() {
        assertThat(jetty9Request.getUriAsString(), is("foo/bar/baz"));
    }

    @Test
    public void shouldSetRequestUri() {
        HttpURI requestUri = new HttpURI("foo/bar/baz");
        when(request.getHttpURI()).thenReturn(requestUri);
        jetty9Request.setRequestURI("foo/junk?a=b&c=d");
        assertThat(requestUri.getPath(), is("foo/junk"));
        assertThat(requestUri.getQuery(), is("a=b&c=d"));
    }
}
