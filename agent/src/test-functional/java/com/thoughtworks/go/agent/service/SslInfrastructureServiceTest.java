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
package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.AgentAutoRegistrationPropertiesImpl;
import com.thoughtworks.go.agent.URLService;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.config.AgentRegistry;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SslInfrastructureServiceTest {
    private SslInfrastructureService sslInfrastructureService;
    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private GoAgentServerHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private URLService urlService;

    @BeforeEach
    void setup() {
        sslInfrastructureService = new SslInfrastructureService(urlService, httpClient, agentRegistry);
    }

    @Test
    void shouldGetTokenFromServerIfOneNotExist() throws Exception {
        final ArgumentCaptor<HttpRequestBase> httpRequestBaseArgumentCaptor = ArgumentCaptor.forClass(HttpRequestBase.class);
        when(agentRegistry.uuid()).thenReturn("some-uuid");
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("https", 1, 2), 200, null));
        when(httpResponse.getEntity()).thenReturn(new StringEntity("token-from-server"));
        when(httpClient.execute(httpRequestBaseArgumentCaptor.capture())).thenReturn(httpResponse);

        sslInfrastructureService.getTokenIfNecessary();

        verify(agentRegistry).storeTokenToDisk("token-from-server");

        final HttpRequestBase httpRequestBase = httpRequestBaseArgumentCaptor.getValue();
        final List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(httpRequestBase.getURI(), StandardCharsets.UTF_8);
        assertThat(findParam(nameValuePairs, "uuid").getValue()).isEqualTo("some-uuid");
    }

    @Test
    void shouldPassUUIDAndTokenDuringAgentRegistration() throws Exception {
        final ArgumentCaptor<HttpRequestBase> httpRequestBaseArgumentCaptor = ArgumentCaptor.forClass(HttpRequestBase.class);

        when(agentRegistry.uuid()).thenReturn("some-uuid");
        when(agentRegistry.token()).thenReturn("some-token");
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("https", 1, 2), 200, null));
        when(httpResponse.getEntity()).thenReturn(new StringEntity(""));
        when(httpClient.execute(httpRequestBaseArgumentCaptor.capture())).thenReturn(httpResponse);

        sslInfrastructureService.createSslInfrastructure();

        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(new File("foo", "bar")));

        final HttpRequestBase httpRequestBase = httpRequestBaseArgumentCaptor.getValue();
        assertThat(httpRequestBase).asInstanceOf(type(HttpEntityEnclosingRequestBase.class))
            .extracting(HttpEntityEnclosingRequestBase::getEntity)
            .satisfies(entity -> {
                final List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(entity);
                assertThat(findParam(nameValuePairs, "uuid").getValue()).isEqualTo("some-uuid");
                assertThat(findParam(nameValuePairs, "token").getValue()).isEqualTo("some-token");
            });
    }

    @Test
    void shouldDeleteTokenFromDiskWhenServerRejectsTheRegistrationRequestWithForbiddenErrorCode() throws Exception {
        final CloseableHttpResponse httpResponseForbidden = mock(CloseableHttpResponse.class);
        final ProtocolVersion protocolVersion = new ProtocolVersion("https", 1, 2);
        when(agentRegistry.uuid()).thenReturn("some-uuid");
        when(agentRegistry.tokenPresent()).thenReturn(true);
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(protocolVersion, HttpURLConnection.HTTP_OK, null));
        when(httpResponseForbidden.getStatusLine()).thenReturn(new BasicStatusLine(protocolVersion, HttpURLConnection.HTTP_FORBIDDEN, null));
        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponseForbidden).thenReturn(httpResponse);
        sslInfrastructureService.createSslInfrastructure();

        sslInfrastructureService.register(new AgentAutoRegistrationPropertiesImpl(new File("foo", "bar")));

        verify(agentRegistry, times(1)).deleteToken();
        verify(httpClient, times(2)).execute(any(HttpRequestBase.class));
    }

    private NameValuePair findParam(List<NameValuePair> nameValuePairs, final String paramName) {
        return nameValuePairs.stream().filter(nameValuePair -> nameValuePair.getName().equals(paramName)).findFirst().orElse(null);
    }

}
