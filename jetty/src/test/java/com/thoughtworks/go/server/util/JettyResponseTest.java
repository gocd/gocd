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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JettyResponseTest {
    private JettyResponse jettyResponse;
    private ServletResponse response;

    @BeforeEach
    public void setUp() {
        response = mock(ServletResponse.class);
        jettyResponse = new JettyResponse(response);
    }

//    @Test
//    public void shouldGetResponseStatus() {
//        when(response.getStatus()).thenReturn(200);
//        assertThat(jettyResponse.getStatus()).isEqualTo(200);
//    }
}
