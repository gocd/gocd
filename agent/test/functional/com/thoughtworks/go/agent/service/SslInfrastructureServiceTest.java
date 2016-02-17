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
import com.thoughtworks.go.agent.testhelpers.AgentCertificateMother;
import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.security.AuthSSLProtocolSocketFactory;
import com.thoughtworks.go.security.Registration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.agent.service.SslInfrastructureService.*;
import static com.thoughtworks.go.agent.testhelpers.AgentCertificateMother.agentCertificate;
import static com.thoughtworks.go.config.GuidService.deleteGuid;
import static com.thoughtworks.go.config.GuidService.storeGuid;
import static com.thoughtworks.go.util.TestUtils.exists;
import static org.apache.commons.httpclient.protocol.Protocol.getProtocol;
import static org.apache.commons.httpclient.protocol.Protocol.unregisterProtocol;
import static org.apache.log4j.Logger.getLogger;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class SslInfrastructureServiceTest {
    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();
    private static final Logger LOGGER = getLogger(SslInfrastructureServiceTest.class);
    private SslInfrastructureService sslInfrastructureService;
    private boolean remoteCalled;
    private HttpConnectionManagerParams httpConnectionManagerParams = new HttpConnectionManagerParams();
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        remoteCalled = false;
        AGENT_CERTIFICATE_FILE.delete();
        AGENT_CERTIFICATE_FILE.deleteOnExit();
        Registration registration = createRegistration();
        sslInfrastructureService = new SslInfrastructureService(requesterStub(registration), httpClientStub(), httpConnectionManagerParams);
        storeGuid("uuid");
    }

    @After
    public void teardown() throws Exception {
        deleteGuid();
        unregisterProtocol("https");
        AGENT_CERTIFICATE_FILE.delete();
        AGENT_TRUST_FILE.delete();
    }

    @Test
    public void shouldInvalidateKeystore() throws Exception {
        folder.create();
        File configFile = folder.newFile();

        shouldCreateSslInfrastucture();

        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        assertThat(AGENT_CERTIFICATE_FILE, exists());
        assertRemoteCalled();

        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        assertRemoteNotCalled();

        sslInfrastructureService.invalidateAgentCertificate();
        sslInfrastructureService.registerIfNecessary(new AgentAutoRegistrationPropertiesImpl(configFile));
        assertRemoteCalled();
    }

    private void shouldCreateSslInfrastucture() throws Exception {
        sslInfrastructureService.createSslInfrastructure();
        Protocol protocol = getProtocol("https");
        assertThat(protocol.getSocketFactory(), instanceOf(AuthSSLProtocolSocketFactory.class));
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
        Registration certificates = agentCertificate();
        return new Registration(certificates.getPrivateKey(), certificates.getChain());
    }

    private RemoteRegistrationRequester requesterStub(final Registration registration) {
        final SslInfrastructureServiceTest me = this;
        return new RemoteRegistrationRequester(null, agentRegistryStub(), new HttpClient()) {
            protected Registration requestRegistration(String agentHostName, AgentAutoRegistrationProperties agentAutoRegisterProperties)
                    throws IOException, ClassNotFoundException {
                LOGGER.debug("Requesting remote registration");
                me.remoteCalled = true;
                return registration;
            }
        };
    }

    private HttpClient httpClientStub() {
        final HttpClient client = context.mock(HttpClient.class);
        context.checking(new Expectations() {
            {
                allowing(client).setHttpConnectionManager(with(any(MultiThreadedHttpConnectionManager.class)));
                allowing(client).getHttpConnectionManager();
                will(returnValue(new MultiThreadedHttpConnectionManager()));
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
