/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.agent.functional;

import static org.junit.Assert.assertTrue;

import java.io.File;

import com.thoughtworks.go.agent.testhelpers.GoServerRunner;
import com.thoughtworks.go.agent.testhelpers.FakeGoServer;
import com.thoughtworks.go.helper.RandomPort;
import com.thoughtworks.go.security.AuthSSLProtocolSocketFactory;
import com.thoughtworks.go.security.AuthSSLX509TrustManagerFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { })
public class X509CertificateTest {
    private File truststore;
    private ProtocolSocketFactory protocolSocketFactory;
    private FakeGoServer fakeGoServer;
    private int sslPort;
    private int serverPort;

    @Before public void setup() throws Exception {
        truststore = new File("truststore");
        truststore.delete();
        AuthSSLX509TrustManagerFactory trustManagerFactory = new AuthSSLX509TrustManagerFactory(
                truststore, GoServerRunner.PASSWORD);

        protocolSocketFactory = new AuthSSLProtocolSocketFactory(trustManagerFactory, null);
        sslPort = RandomPort.find("X509CertificateTest-sslPort");
        serverPort = RandomPort.find("X509CertificateTest-serverPort");
        fakeGoServer = new FakeGoServer(serverPort, sslPort);
        fakeGoServer.start();
    }

    @After public void teardown() throws Exception {
        truststore.delete();
        fakeGoServer.stop();
    }

    @Test
    public void shouldSaveCertificateInAgentTrustStore() throws Exception {
        Protocol authhttps = new Protocol("https", protocolSocketFactory, sslPort);
        Protocol.registerProtocol("https", authhttps);
        HttpClient client = new HttpClient();
        GetMethod httpget = new GetMethod("https://localhost:" + sslPort + "/go/");
        client.executeMethod(httpget);
        assertTrue("Should have created trust store", truststore.exists());
    }
}
