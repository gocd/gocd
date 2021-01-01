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

import com.thoughtworks.go.agent.AgentAutoRegistrationPropertiesImpl;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.config.TokenService;
import com.thoughtworks.go.util.URLService;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
public class SslInfrastructureServiceTest {
    private SslInfrastructureService sslInfrastructureService;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private GuidService guidService = new GuidService();
    private TokenService tokenService = new TokenService();

    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private GoAgentServerHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private URLService urlService;

    @BeforeEach
    void setup() throws Exception {
        temporaryFolder.create();
        initMocks(this);

        sslInfrastructureService = new SslInfrastructureService(urlService, httpClient, agentRegistry);
        guidService = new GuidService();
        guidService.store("uuid");
    }

    @AfterEach
    void teardown() {
        temporaryFolder.delete();
        guidService.delete();
        tokenService.delete();
    }

    @Test
    void shouldGetTokenFromServerIfOneNotExist() throws Exception {
        final ArgumentCaptor<HttpRequestBase> httpRequestBaseArgumentCaptor = ArgumentCaptor.forClass(HttpRequestBase.class);
        tokenService.delete();
        when(agentRegistry.uuid()).thenReturn("some-uuid");
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("https", 1, 2), 200, null));
        when(httpResponse.getEntity()).thenReturn(new StringEntity("token-from-server"));
        when(httpClient.execute(httpRequestBaseArgumentCaptor.capture())).thenReturn(httpResponse);

        sslInfrastructureService.getTokenIfNecessary();

        verify(agentRegistry).storeTokenToDisk("token-from-server");

        final HttpRequestBase httpRequestBase = httpRequestBaseArgumentCaptor.getValue();
        final List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(httpRequestBase.getURI(), StandardCharsets.UTF_8);
        assertThat(findParam(nameValuePairs, "uuid").getValue(), is("some-uuid"));
    }

    @Test
    void shouldPassUUIDAndTokenDuringAgentRegistration() throws Exception {
        final ArgumentCaptor<HttpEntityEnclosingRequestBase> httpRequestBaseArgumentCaptor = ArgumentCaptor.forClass(HttpEntityEnclosingRequestBase.class);

        when(agentRegistry.uuid()).thenReturn("some-uuid");
        when(agentRegistry.token()).thenReturn("some-token");
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("https", 1, 2), 200, null));
        when(httpResponse.getEntity()).thenReturn(new StringEntity(""));
        when(httpClient.execute(httpRequestBaseArgumentCaptor.capture())).thenReturn(httpResponse);

        sslInfrastructureService.createSslInfrastructure();

        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(new File("foo", "bar")));

        final HttpEntityEnclosingRequestBase httpRequestBase = httpRequestBaseArgumentCaptor.getValue();
        final List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(httpRequestBase.getEntity());

        assertThat(findParam(nameValuePairs, "uuid").getValue(), is("some-uuid"));
        assertThat(findParam(nameValuePairs, "token").getValue(), is("some-token"));

    }

    @Test
    void shouldDeleteTokenFromDiskWhenServerRejectsTheRegistrationRequestWithForbiddenErrorCode() throws Exception {
        final CloseableHttpResponse httpResponseForbidden = mock(CloseableHttpResponse.class);
        final ProtocolVersion protocolVersion = new ProtocolVersion("https", 1, 2);
        when(agentRegistry.uuid()).thenReturn("some-uuid");
        when(agentRegistry.tokenPresent()).thenReturn(true);
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(protocolVersion, HttpStatus.OK.value(), null));
        when(httpResponseForbidden.getStatusLine()).thenReturn(new BasicStatusLine(protocolVersion, HttpStatus.FORBIDDEN.value(), null));
        when(httpResponse.getEntity()).thenReturn(new StringEntity(""));
        when(httpResponseForbidden.getEntity()).thenReturn(new StringEntity("Not a valid token."));
        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(httpResponseForbidden).thenReturn(httpResponse);
        sslInfrastructureService.createSslInfrastructure();

        sslInfrastructureService.register(new AgentAutoRegistrationPropertiesImpl(new File("foo", "bar")));

        verify(agentRegistry, times(1)).deleteToken();
        verify(httpClient, times(2)).execute(any(HttpRequestBase.class));
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
