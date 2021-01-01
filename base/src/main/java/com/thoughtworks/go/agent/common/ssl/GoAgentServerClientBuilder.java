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
package com.thoughtworks.go.agent.common.ssl;

import com.thoughtworks.go.util.SslVerificationMode;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

public abstract class GoAgentServerClientBuilder<T> {
    protected final File rootCertFile;
    protected final SystemEnvironment systemEnvironment;
    protected final SslVerificationMode sslVerificationMode;
    protected final File sslPrivateKey;
    protected final File sslPrivateKeyPassphraseFile;
    protected final File sslCertificate;
    protected final char[] agentKeystorePassword = UUID.randomUUID().toString().toCharArray();

    public abstract T build() throws Exception;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    GoAgentServerClientBuilder(SystemEnvironment systemEnvironment, File rootCertFile, SslVerificationMode sslVerificationMode, File sslPrivateKey, File sslPrivateKeyPassphraseFile, File sslCertificate) {
        this.systemEnvironment = systemEnvironment;
        this.rootCertFile = rootCertFile;
        this.sslVerificationMode = sslVerificationMode;
        this.sslPrivateKey = sslPrivateKey;
        this.sslPrivateKeyPassphraseFile = sslPrivateKeyPassphraseFile;
        this.sslCertificate = sslCertificate;
    }

    GoAgentServerClientBuilder(SystemEnvironment systemEnvironment) {
        this(systemEnvironment, systemEnvironment.getRootCertFile(), systemEnvironment.getAgentSslVerificationMode(), systemEnvironment.getAgentPrivateKeyFile(), systemEnvironment.getAgentSslPrivateKeyPassphraseFile(), systemEnvironment.getAgentSslCertificate());
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
        if (this.sslPrivateKey != null && this.sslPrivateKey.exists() && this.sslCertificate != null && this.sslCertificate.exists()) {
            return keyStoreFromPem();
        } else {
            return null;
        }
    }

    private KeyStore keyStoreFromPem() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);
        if (sslCertificate != null && sslPrivateKey != null) {
            PrivateKey privateKey = getPrivateKey();
            keyStore.setKeyEntry("1", privateKey, agentKeystorePassword, agentCertificate().toArray(new X509Certificate[]{}));
        }
        return keyStore;
    }

    private PrivateKey getPrivateKey() throws IOException {
        PrivateKey privateKey;
        try (PEMParser reader = new PEMParser(new FileReader(this.sslPrivateKey, StandardCharsets.UTF_8))) {
            Object pemObject = reader.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(new BouncyCastleProvider());

            if (pemObject instanceof PEMEncryptedKeyPair) {
                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(passphrase());
                KeyPair keyPair = converter.getKeyPair(((PEMEncryptedKeyPair) pemObject).decryptKeyPair(decProv));
                privateKey = keyPair.getPrivate();
            } else if (pemObject instanceof PEMKeyPair) {
                KeyPair keyPair = converter.getKeyPair((PEMKeyPair) pemObject);
                privateKey = keyPair.getPrivate();
            } else if (pemObject instanceof PrivateKeyInfo) {
                PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) pemObject;
                privateKey = converter.getPrivateKey(privateKeyInfo);
            } else {
                throw new RuntimeException("Unable to parse key of type " + pemObject.getClass());
            }
            return privateKey;
        }
    }

    private List<X509Certificate> agentCertificate() throws IOException, CertificateException {
        return new CertificateFileParser().certificates(this.sslCertificate);
    }

    private char[] passphrase() throws IOException {
        if (sslPrivateKeyPassphraseFile != null && sslPrivateKeyPassphraseFile.exists()) {
            String passphrase = FileUtils.readFileToString(sslPrivateKeyPassphraseFile, StandardCharsets.UTF_8);
            return StringUtils.trimToEmpty(passphrase).toCharArray();
        }
        throw new RuntimeException("SSL private key passphrase not specified!");
    }
}
