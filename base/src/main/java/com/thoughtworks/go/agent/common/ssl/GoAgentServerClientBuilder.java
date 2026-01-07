/*
 * Copyright Thoughtworks, Inc.
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
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

public abstract class GoAgentServerClientBuilder<T> {
    protected final File rootCertificate;
    protected final SslVerificationMode sslVerificationMode;
    protected final File agentSslPrivateKey;
    protected final File agentSslPrivateKeyPassphraseFile;
    protected final File agentSslCertificate;
    protected final char[] agentKeystorePassword = UUID.randomUUID().toString().toCharArray();

    public abstract T build() throws Exception;

    GoAgentServerClientBuilder(File rootCertificate, SslVerificationMode sslVerificationMode, File agentSslCertificate, File agentSslPrivateKey, File agentSslPrivateKeyPassphraseFile) {
        this.rootCertificate = rootCertificate;
        this.sslVerificationMode = sslVerificationMode;
        this.agentSslCertificate = agentSslCertificate;
        this.agentSslPrivateKey = agentSslPrivateKey;
        this.agentSslPrivateKeyPassphraseFile = agentSslPrivateKeyPassphraseFile;
    }

    GoAgentServerClientBuilder(SystemEnvironment systemEnvironment) {
        this(systemEnvironment.getRootCertFile(), systemEnvironment.getAgentSslVerificationMode(), systemEnvironment.getAgentSslCertificate(), systemEnvironment.getAgentPrivateKeyFile(), systemEnvironment.getAgentSslPrivateKeyPassphraseFile());
    }

    KeyStore agentTruststore() throws GeneralSecurityException, IOException {
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

    KeyStore agentKeystore() throws GeneralSecurityException, IOException, OperatorCreationException, PKCSException {
        if (this.agentSslCertificate != null && this.agentSslCertificate.exists() && this.agentSslPrivateKey != null && this.agentSslPrivateKey.exists()) {
            return keyStoreFromPem();
        } else {
            return null;
        }
    }

    private KeyStore keyStoreFromPem() throws GeneralSecurityException, IOException, OperatorCreationException, PKCSException {
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

    private PrivateKey getPrivateKey() throws IOException, OperatorCreationException, PKCSException {
        try (PEMParser reader = new PEMParser(new FileReader(this.agentSslPrivateKey, StandardCharsets.UTF_8))) {
            Object pemObject = reader.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProviderHolder.INSTANCE);

            return switch (pemObject) {
                case PEMEncryptedKeyPair pemEncryptedKeyPair ->
                    converter.getKeyPair(pemEncryptedKeyPair.decryptKeyPair(newPemDecryptorProvider())).getPrivate();
                case PEMKeyPair pemKeyPair ->
                    converter.getKeyPair(pemKeyPair).getPrivate();
                case PKCS8EncryptedPrivateKeyInfo pkcs8EncryptedPrivateKeyInfo ->
                    converter.getPrivateKey(pkcs8EncryptedPrivateKeyInfo.decryptPrivateKeyInfo(newInputDecryptorProvider()));
                case PrivateKeyInfo privateKeyInfo ->
                    converter.getPrivateKey(privateKeyInfo);
                default -> throw new RuntimeException("Unable to parse key of type " + pemObject.getClass());
            };
        }
    }

    private InputDecryptorProvider newInputDecryptorProvider() throws OperatorCreationException, IOException {
        InputDecryptorProvider decProv = new JceOpenSSLPKCS8DecryptorProviderBuilder()
            .setProvider(BouncyCastleProviderHolder.INSTANCE)
            .build(passphrase());
        return decProv;
    }

    private PEMDecryptorProvider newPemDecryptorProvider() throws IOException {
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
            .setProvider(BouncyCastleProviderHolder.INSTANCE)
            .build(passphrase());
        return decProv;
    }

    private List<X509Certificate> agentCertificate() throws IOException, CertificateException {
        return new CertificateFileParser().certificates(this.agentSslCertificate);
    }

    private char[] passphrase() throws IOException {
        if (agentSslPrivateKeyPassphraseFile != null && agentSslPrivateKeyPassphraseFile.exists()) {
            return Files.readString(agentSslPrivateKeyPassphraseFile.toPath(), StandardCharsets.UTF_8).trim().toCharArray();
        }
        throw new RuntimeException("SSL private key passphrase not specified!");
    }
}
