/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.SslVerificationMode;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.TrustManager;
import java.security.KeyStore;
import java.security.cert.CRL;
import java.util.Collection;

@Component
public class GoAgentServerWebSocketClientBuilder extends GoAgentServerClientBuilder<WebSocketClient> {
    @Autowired
    public GoAgentServerWebSocketClientBuilder(SystemEnvironment systemEnvironment) {
        super(systemEnvironment);
    }

    @Override
    public WebSocketClient build() throws Exception {
        SslContextFactory sslContextFactory = sslVerificationMode == SslVerificationMode.NONE ? new TrustAllSSLContextFactory() : new SslContextFactory();
        sslContextFactory.setNeedClientAuth(true);

        sslContextFactory.setKeyStore(agentKeystore());
        sslContextFactory.setKeyStorePassword(keystorePassword());
        sslContextFactory.setKeyManagerPassword(keystorePassword());

        if (rootCertFile != null) {
            sslContextFactory.setTrustStore(agentTruststore());
            sslContextFactory.setTrustStorePassword(keystorePassword());
        }

        if (sslVerificationMode == SslVerificationMode.NO_VERIFY_HOST) {
            sslContextFactory.setEndpointIdentificationAlgorithm(null);
        }

        WebSocketClient client = new WebSocketClient(sslContextFactory);
        client.setMaxIdleTimeout(systemEnvironment.getWebsocketMaxIdleTime());
        return client;
    }

    private class TrustAllSSLContextFactory extends SslContextFactory {
        @Override
        protected TrustManager[] getTrustManagers(KeyStore trustStore, Collection<? extends CRL> crls) throws Exception {
            return TRUST_ALL_CERTS;
        }
    }
}

