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
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class GoAgentServerHttpClientBuilder {

    public static final File AGENT_CERTIFICATE_FILE = new File("config", "agent.jks");
    @Deprecated
    public static final File AGENT_TRUST_FILE = new File("config", "trust.jks");

    private final File rootCertFile;
    private final File keyStoreFile;
    private final SslVerificationMode sslVerificationMode;
    private final SystemEnvironment systemEnvironment;

    public GoAgentServerHttpClientBuilder(File rootCertFile, SslVerificationMode sslVerificationMode) {
        this(rootCertFile, AGENT_CERTIFICATE_FILE, sslVerificationMode, new SystemEnvironment());
    }

    public GoAgentServerHttpClientBuilder(SystemEnvironment systemEnvironment) {
        this(systemEnvironment.getRootCertFile(), AGENT_CERTIFICATE_FILE, systemEnvironment.getAgentSslVerificationMode(), systemEnvironment);
    }

    private GoAgentServerHttpClientBuilder(File rootCertFile, File keyStoreFile, SslVerificationMode sslVerificationMode, SystemEnvironment systemEnvironment) {
        this.rootCertFile = rootCertFile;
        this.keyStoreFile = keyStoreFile;
        this.sslVerificationMode = sslVerificationMode;
        this.systemEnvironment = systemEnvironment;
    }

    public CloseableHttpClient httpClient() throws Exception {
        return httpClientBuilder(HttpClients.custom()).build();
    }

    public HttpClientBuilder httpClientBuilder(HttpClientBuilder builder) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {
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

        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create()
                .useProtocol(systemEnvironment.get(SystemEnvironment.GO_SSL_TRANSPORT_PROTOCOL_TO_BE_USED_BY_AGENT));

        if (trustStore != null || trustStrategy != null) {
            sslContextBuilder.loadTrustMaterial(trustStore, trustStrategy);
        }

        sslContextBuilder.loadKeyMaterial(agentKeystore(), keystorePassword().toCharArray());

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContextBuilder.build(), hostnameVerifier);
        builder.setSSLSocketFactory(sslsf);
        return builder;
    }

    public void initialize() {
        File parentFile = GoAgentServerHttpClientBuilder.AGENT_TRUST_FILE.getParentFile();

        if (!(parentFile.exists() || parentFile.mkdirs())) {
            bomb("Unable to create folder " + parentFile.getAbsolutePath());
        }
    }

    public KeyStore agentTruststore() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        KeyStore trustStore = null;

        List<X509Certificate> certificates = new CertificateFileParser().certificates(rootCertFile);

        for (X509Certificate certificate : certificates) {
            if (trustStore == null) {
                trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
            }
            trustStore.setCertificateEntry(certificate.getSubjectX500Principal().getName(), certificate);
        }

        return trustStore;
    }

    public KeyStore agentKeystore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream is = keyStoreInputStream()) {
            keyStore.load(is, keystorePassword().toCharArray());
        }
        return keyStore;
    }

    public String keystorePassword() {
        return systemEnvironment.get(SystemEnvironment.GO_AGENT_KEYSTORE_PASSWORD);
    }

    private InputStream keyStoreInputStream() throws FileNotFoundException {
        return !keyStoreFile.exists() ? null : new FileInputStream(keyStoreFile);
    }

    public X500Principal principal() {
        try {
            KeyStore keyStore = agentKeystore();
            if (keyStore.containsAlias("agent")) {
                return ((X509Certificate) keyStore.getCertificate("agent")).getSubjectX500Principal();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
