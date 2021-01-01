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
package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.config.AgentRegistry;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class TokenRequesterTest {
    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private GoAgentServerHttpClient httpClient;
    private TokenRequester tokenRequester;

    @BeforeEach
    void setUp() throws Exception {
        initMocks(this);

        tokenRequester = new TokenRequester("toke-url", agentRegistry, httpClient);
    }

    @Test
    void shouldGetTokenFromServer() throws Exception {
        final ArgumentCaptor<HttpRequestBase> argumentCaptor = ArgumentCaptor.forClass(HttpRequestBase.class);
        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);

        when(agentRegistry.uuid()).thenReturn("agent-uuid");
        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("token-from-server"));
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("https", 1, 2), SC_OK, null));

        final String token = tokenRequester.getToken();

        verify(httpClient).execute(argumentCaptor.capture());

        final HttpRequestBase requestBase = argumentCaptor.getValue();
        final List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(requestBase.getURI(), StandardCharsets.UTF_8.name());

        assertThat(token).isEqualTo("token-from-server");
        assertThat(findParam(nameValuePairs, "uuid").getValue()).isEqualTo("agent-uuid");
    }

    @Test
    void shouldErrorOutIfServerRejectTheRequest() throws Exception {
        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);

        when(agentRegistry.uuid()).thenReturn("agent-uuid");
        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("A token has already been issued for this agent."));
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("https", 1, 2), SC_UNPROCESSABLE_ENTITY, null));

        assertThatCode(() -> tokenRequester.getToken())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A token has already been issued for this agent.");
    }

    private NameValuePair findParam(List<NameValuePair> nameValuePairs, final String paramName) {
        return nameValuePairs.stream().filter(nameValuePair -> nameValuePair.getName().equals(paramName)).findFirst().orElse(null);
    }
}
