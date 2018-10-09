/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TokenRequesterTest {
    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private GoAgentServerHttpClient httpClient;
    private TokenRequester tokenRequester;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        tokenRequester = new TokenRequester("toke-url", agentRegistry, httpClient);
    }

    @Test
    public void shouldGetTokenFromServer() throws Exception {
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

        assertThat(token, is("token-from-server"));
        assertThat(findParam(nameValuePairs, "uuid").getValue(), is("agent-uuid"));
    }

    @Test
    public void shouldErrorOutIfServerRejectTheRequest() throws Exception {
        final ArgumentCaptor<HttpRequestBase> argumentCaptor = ArgumentCaptor.forClass(HttpRequestBase.class);
        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);

        when(agentRegistry.uuid()).thenReturn("agent-uuid");
        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("A token has already been issued for this agent."));
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("https", 1, 2), SC_UNPROCESSABLE_ENTITY, null));

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("A token has already been issued for this agent.");

        tokenRequester.getToken();
    }

    private NameValuePair findParam(List<NameValuePair> nameValuePairs, final String paramName) {
        return nameValuePairs.stream().filter(new Predicate<NameValuePair>() {
            @Override
            public boolean test(NameValuePair nameValuePair) {
                return nameValuePair.getName().equals(paramName);
            }
        }).findFirst().orElse(null);
    }
}
