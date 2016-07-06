/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.agent.testhelpers.AgentCertificateMother;
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.TestUtils.exists;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(JMock.class)
public class SslInfrastructureServiceTest {
    private static final Logger LOGGER = Logger.getLogger(SslInfrastructureServiceTest.class);
    private final Mockery context = new ClassMockery();
    private SslInfrastructureService sslInfrastructureService;
    private boolean remoteCalled;
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        remoteCalled = false;
        GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE.delete();
        GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE.deleteOnExit();
        Registration registration = createRegistration();
        sslInfrastructureService = new SslInfrastructureService(requesterStub(registration), httpClientStub());
        GuidService.storeGuid("uuid");
    }

    @After
    public void teardown() throws Exception {
        GuidService.deleteGuid();
        GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE.delete();
        GoAgentServerHttpClientBuilder.AGENT_TRUST_FILE.delete();
    }

    @Test
    public void shouldInvalidateKeystore() throws Exception {
        folder.create();
        File configFile = folder.newFile();

        shouldCreateSslInfrastucture();

        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        assertThat(GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE, exists());
        assertRemoteCalled();

        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        assertRemoteNotCalled();

        sslInfrastructureService.invalidateAgentCertificate();
        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        assertRemoteCalled();
    }

    private void shouldCreateSslInfrastucture() throws Exception {
        sslInfrastructureService.createSslInfrastructure();
//        Protocol protocol = Protocol.getProtocol("https");
//        assertThat(protocol.getSocketFactory(), instanceOf(AuthSSLProtocolSocketFactory.class));
    }

    private void assertRemoteCalled() {
        assertThat("Remote called", remoteCalled, is(true));
        remoteCalled = false;
    }

    private void assertRemoteNotCalled() {
        assertThat("Remote not called", remoteCalled, is(false));
        remoteCalled = false;
    }

    private Registration createRegistration() {
        Registration certificates = AgentCertificateMother.agentCertificate();
        return new Registration(certificates.getPrivateKey(), certificates.getChain());
    }

    private SslInfrastructureService.RemoteRegistrationRequester requesterStub(final Registration registration) {
        final SslInfrastructureServiceTest me = this;
        return new SslInfrastructureService.RemoteRegistrationRequester(null, agentRegistryStub(), new GoAgentServerHttpClient(new SystemEnvironment())) {
            protected Registration requestRegistration(String agentHostName, AgentAutoRegistrationProperties agentAutoRegisterProperties)
                    throws IOException, ClassNotFoundException {
                LOGGER.debug("Requesting remote registration");
                me.remoteCalled = true;
                return registration;
            }
        };
    }

    private GoAgentServerHttpClient httpClientStub() {
        final GoAgentServerHttpClient client = context.mock(GoAgentServerHttpClient.class);
        context.checking(new Expectations() {
            {
                try {
                    allowing(client).reset();
                } catch (Exception e){
                    throw bomb(e);
                }
            }
        });
        return client;
    }

    private AgentRegistry agentRegistryStub() {
        final AgentRegistry registry = context.mock(AgentRegistry.class);
        context.checking(new Expectations() {
            {
                allowing(registry).uuid();
                will(returnValue("uuid"));
            }
        });
        return registry;
    }
}
