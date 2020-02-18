/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.security.KeyStore;

public class GoAgentServerHttpClientBuilder extends GoAgentServerClientBuilder<CloseableHttpClient> {

    public GoAgentServerHttpClientBuilder(File rootCertFile, SslVerificationMode sslVerificationMode, File sslPrivateKey, File sslPrivateKeyPassphraseFile, File sslCertificate) {
        super(new SystemEnvironment(), rootCertFile, sslVerificationMode, sslPrivateKey, sslPrivateKeyPassphraseFile, sslCertificate);
    }

    public GoAgentServerHttpClientBuilder(SystemEnvironment systemEnvironment) {
        super(systemEnvironment);
    }

    @Override
    public CloseableHttpClient build() throws Exception {
        HttpClientBuilder builder = HttpClients.custom();
        builder.useSystemProperties();
        builder
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setTcpNoDelay(true)
                        .setSoKeepAlive(true)
                        .build()
                )
                .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE);

        HostnameVerifier hostnameVerifier = sslVerificationMode.verifier();
        TrustStrategy trustStrategy = sslVerificationMode.trustStrategy();
        KeyStore trustStore = agentTruststore();

        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();

        if (trustStore != null || trustStrategy != null) {
            sslContextBuilder.loadTrustMaterial(trustStore, trustStrategy);
        }

        KeyStore keystore = agentKeystore();

        if (keystore != null) {
            sslContextBuilder.loadKeyMaterial(keystore, agentKeystorePassword);
        }

        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(), hostnameVerifier);
        builder.setSSLSocketFactory(sslConnectionSocketFactory);
        return builder.build();
    }


}
