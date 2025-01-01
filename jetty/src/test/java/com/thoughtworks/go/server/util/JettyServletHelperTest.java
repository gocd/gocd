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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class JettyServletHelperTest {
    @Test
    public void shouldGetInstanceOfServletHelper(){
        ServletHelper.init();
        assertThat(ServletHelper.getInstance()).isInstanceOf(JettyServletHelper.class);
    }

    @Test
    public void shouldGetJettyRequest() {
        ServletRequest request = new JettyServletHelper().getRequest(mock(Request.class));
        assertThat(request).isInstanceOf(JettyRequest.class);
    }

    @Test
    public void shouldGetJettyResponse() {
        ServletResponse response = new JettyServletHelper().getResponse(mock(Response.class));
        assertThat(response).isInstanceOf(JettyResponse.class);
    }
}
