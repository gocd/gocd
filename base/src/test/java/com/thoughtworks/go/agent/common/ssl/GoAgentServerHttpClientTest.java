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

package com.thoughtworks.go.agent.common.ssl;

import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GoAgentServerHttpClientTest {
    @Test
    void shouldNormalizeUriOnRequest() throws Exception {
        GoAgentServerHttpClientBuilder builder = mock();
        when(builder.build()).thenReturn(mock());
        try (GoAgentServerHttpClient client = new GoAgentServerHttpClient(builder)) {
            client.init();
            HttpRequestBase request = mock();
            when(request.getURI()).thenReturn(new URI("http://example.com/gl/../gl"));
            assertThat(client.execute(request)).isNull();
            verify(request).setURI(new URI("http://example.com/gl"));
        }
    }
}