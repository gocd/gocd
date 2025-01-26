/*
 * Copyright Thoughtworks, Inc.
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

import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JettyResponseTest {
    private JettyResponse jettyResponse;
    private Response response;

    @BeforeEach
    public void setUp() {
        response = mock(Response.class);
        jettyResponse = new JettyResponse(response);
    }

    @Test
    public void shouldGetResponseStatus() {
        when(response.getStatus()).thenReturn(200);
        assertThat(jettyResponse.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldGetResponseContentCount() {
        when(response.getContentCount()).thenReturn(2000L);
        assertThat(jettyResponse.getContentCount()).isEqualTo(2000L);
    }
}
