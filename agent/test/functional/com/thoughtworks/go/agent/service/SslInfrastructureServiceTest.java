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

import com.thoughtworks.go.agent.AgentAutoRegistrationPropertiesImpl;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerClientBuilder;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.agent.testhelpers.AgentCertificateMother;
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.config.TokenService;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.util.ClassMockery;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.thoughtworks.go.util.TestUtils.exists;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(JMock.class)
public class SslInfrastructureServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SslInfrastructureServiceTest.class);
    private final Mockery context = new ClassMockery();
    private SslInfrastructureService sslInfrastructureService;
    private boolean remoteCalled;
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private GuidService guidService = new GuidService();
    private TokenService tokenService = new TokenService();

    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private TokenRequester tokenRequester;
    @Mock
    private GoAgentServerHttpClient httpClient;
    @Mock
    private SslInfrastructureService.RemoteRegistrationRequester remoteRegistrationRequester;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        remoteCalled = false;
        GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE.delete();
        GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE.deleteOnExit();

        sslInfrastructureService = new SslInfrastructureService(remoteRegistrationRequester, httpClient, tokenRequester, agentRegistry);
        guidService = new GuidService();
        guidService.store("uuid");
    }

    @After
    public void teardown() throws Exception {
        guidService.delete();
        tokenService.delete();
        GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE.delete();
    }

    @Test
    public void shouldInvalidateKeystore() throws Exception {
        folder.create();
        File configFile = folder.newFile();

        when(agentRegistry.guidPresent()).thenReturn(true);
        when(remoteRegistrationRequester.requestRegistration(anyString(), any(AgentAutoRegistrationProperties.class))).thenReturn(createRegistration());

        shouldCreateSslInfrastructure();

        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        assertThat(GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE, exists());
        verify(remoteRegistrationRequester, times(1)).requestRegistration(anyString(), any(AgentAutoRegistrationProperties.class));

        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        verify(remoteRegistrationRequester, times(1)).requestRegistration(anyString(), any(AgentAutoRegistrationProperties.class));

        sslInfrastructureService.invalidateAgentCertificate();
        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        verify(remoteRegistrationRequester, times(2)).requestRegistration(anyString(), any(AgentAutoRegistrationProperties.class));
    }

    @Test
    public void shouldGetTokenFromServerIfOneNotExist() throws Exception {
        tokenService.delete();
        when(tokenRequester.getToken()).thenReturn("token-from-server");

        sslInfrastructureService.getTokenIfNecessary();

        verify(agentRegistry).storeTokenToDisk("token-from-server");
    }

    private void shouldCreateSslInfrastructure() throws Exception {
        sslInfrastructureService.createSslInfrastructure();
    }

    private Registration createRegistration() {
        Registration certificates = AgentCertificateMother.agentCertificate();
        return new Registration(certificates.getPrivateKey(), certificates.getChain());
    }
}
