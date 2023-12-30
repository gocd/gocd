/*
 * Copyright 2024 Thoughtworks, Inc.
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
    protected final SystemEnvironment systemEnvironment;
    protected final File rootCertificate;
    protected final SslVerificationMode sslVerificationMode;
    protected final File agentSslPrivateKey;
    protected final File agentSslPrivateKeyPassphraseFile;
    protected final File agentSslCertificate;
    protected final char[] agentKeystorePassword = UUID.randomUUID().toString().toCharArray();

    public abstract T build() throws Exception;

    GoAgentServerClientBuilder(SystemEnvironment systemEnvironment, File rootCertificate, SslVerificationMode sslVerificationMode, File agentSslCertificate, File agentSslPrivateKey, File agentSslPrivateKeyPassphraseFile) {
        this.systemEnvironment = systemEnvironment;
        this.rootCertificate = rootCertificate;
        this.sslVerificationMode = sslVerificationMode;
        this.agentSslCertificate = agentSslCertificate;
        this.agentSslPrivateKey = agentSslPrivateKey;
        this.agentSslPrivateKeyPassphraseFile = agentSslPrivateKeyPassphraseFile;
    }

    GoAgentServerClientBuilder(SystemEnvironment systemEnvironment) {
        this(systemEnvironment, systemEnvironment.getRootCertFile(), systemEnvironment.getAgentSslVerificationMode(), systemEnvironment.getAgentSslCertificate(), systemEnvironment.getAgentPrivateKeyFile(), systemEnvironment.getAgentSslPrivateKeyPassphraseFile());
    }

    KeyStore agentTruststore() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        KeyStore trustStore = null;

        List<X509Certificate> certificates = new CertificateFileParser().certificates(rootCertificate);

        for (X509Certificate certificate : certificates) {
            if (trustStore == null) {
                trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
            }
            trustStore.setCertificateEntry(certificate.getSubjectX500Principal().getName(), certificate);
        }

        return trustStore;
    }

    KeyStore agentKeystore() throws IOException, GeneralSecurityException {
        if (this.agentSslCertificate != null && this.agentSslCertificate.exists() && this.agentSslPrivateKey != null && this.agentSslPrivateKey.exists()) {
            return keyStoreFromPem();
        } else {
            return null;
        }
    }

    private KeyStore keyStoreFromPem() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);
        if (agentSslCertificate != null && agentSslPrivateKey != null) {
            PrivateKey privateKey = getPrivateKey();
            keyStore.setKeyEntry("1", privateKey, agentKeystorePassword, agentCertificate().toArray(new X509Certificate[]{}));
        }
        return keyStore;
    }

    private static class BouncyCastleProviderHolder {
        static final Provider INSTANCE = new BouncyCastleProvider();
    }

    private PrivateKey getPrivateKey() throws IOException {
        PrivateKey privateKey;
        try (PEMParser reader = new PEMParser(new FileReader(this.agentSslPrivateKey, StandardCharsets.UTF_8))) {
            Object pemObject = reader.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProviderHolder.INSTANCE);

            if (pemObject instanceof PEMEncryptedKeyPair) {
                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                    .setProvider(BouncyCastleProviderHolder.INSTANCE)
                    .build(passphrase());
                KeyPair keyPair = converter.getKeyPair(((PEMEncryptedKeyPair) pemObject).decryptKeyPair(decProv));
                privateKey = keyPair.getPrivate();
            } else if (pemObject instanceof PEMKeyPair) {
                KeyPair keyPair = converter.getKeyPair((PEMKeyPair) pemObject);
                privateKey = keyPair.getPrivate();
            } else if (pemObject instanceof PrivateKeyInfo privateKeyInfo) {
                privateKey = converter.getPrivateKey(privateKeyInfo);
            } else {
                throw new RuntimeException("Unable to parse key of type " + pemObject.getClass());
            }
            return privateKey;
        }
    }

    private List<X509Certificate> agentCertificate() throws IOException, CertificateException {
        return new CertificateFileParser().certificates(this.agentSslCertificate);
    }

    private char[] passphrase() throws IOException {
        if (agentSslPrivateKeyPassphraseFile != null && agentSslPrivateKeyPassphraseFile.exists()) {
            String passphrase = FileUtils.readFileToString(agentSslPrivateKeyPassphraseFile, StandardCharsets.UTF_8);
            return StringUtils.trimToEmpty(passphrase).toCharArray();
        }
        throw new RuntimeException("SSL private key passphrase not specified!");
    }
}
