/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JettyRequestTest {
    private JettyRequest jettyRequest;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private Request request;

    @Captor
    private ArgumentCaptor<HttpURI> capturedUri;

    @BeforeEach
    public void setUp() throws Exception {
        jettyRequest = new JettyRequest(request);
    }

    @Test
    public void shouldGetRootUrl() {
        when(request.getRootURL()).thenReturn(new StringBuilder("http://junk/"));
        assertThat(jettyRequest.getRootURL()).isEqualTo("http://junk/");
    }

    @Test
    public void shouldAlterRequestUriOnRequest() {
        when(request.getHttpURI()).thenReturn(HttpURI.from("foo/bar/baz?a=b&c=d"));

        jettyRequest.modifyPath(path -> path.replaceAll("^foo/bar/baz", "foo/junk"));
        verify(request).setHttpURI(capturedUri.capture());
        assertThat(capturedUri.getValue().getPath()).isEqualTo("foo/junk");
        assertThat(capturedUri.getValue().getQuery()).isEqualTo("a=b&c=d");
    }
}
