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

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public abstract class GoAgentServerClientBuilder<T> {
    public static final File AGENT_CERTIFICATE_FILE = new File(new SystemEnvironment().getConfigDir(), "agent.jks");
    protected final File rootCertFile;
    private final File keyStoreFile;
    protected final SystemEnvironment systemEnvironment;
    protected final SslVerificationMode sslVerificationMode;

    public abstract T build() throws Exception;

    GoAgentServerClientBuilder(SystemEnvironment systemEnvironment, File rootCertFile, File keyStoreFile, SslVerificationMode sslVerificationMode) {
        this.systemEnvironment = systemEnvironment;
        this.rootCertFile = rootCertFile;
        this.keyStoreFile = keyStoreFile;
        this.sslVerificationMode = sslVerificationMode;
    }

    GoAgentServerClientBuilder(SystemEnvironment systemEnvironment) {
        this(systemEnvironment.getRootCertFile(), AGENT_CERTIFICATE_FILE, systemEnvironment.getAgentSslVerificationMode(), systemEnvironment);
    }

    private GoAgentServerClientBuilder(File rootCertFile, File keyStoreFile, SslVerificationMode sslVerificationMode, SystemEnvironment systemEnvironment) {
        this(systemEnvironment, rootCertFile, keyStoreFile, sslVerificationMode);
    }

    KeyStore agentTruststore() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
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

    KeyStore agentKeystore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream is = keyStoreInputStream()) {
            keyStore.load(is, keystorePassword().toCharArray());
        }
        return keyStore;
    }

    public String keystorePassword() {
        return systemEnvironment.getAgentKeyStorePassword();
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

    public void initialize() {
        File parentFile = GoAgentServerClientBuilder.AGENT_CERTIFICATE_FILE.getParentFile();

        if (!(parentFile.exists() || parentFile.mkdirs())) {
            bomb("Unable to create folder " + parentFile.getAbsolutePath());
        }
    }
}
