/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.security;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Date;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.SystemEnvironment.GO_SSL_CERTS_ALGORITHM;
import static com.thoughtworks.go.util.SystemEnvironment.GO_SSL_CERTS_PUBLIC_KEY_ALGORITHM;

import java.security.cert.Certificate;

@Component
public class X509CertificateGenerator {
    private static final int YEARS = 10;
    private static final String PASSWORD = "Crui3CertSigningPassword";
    @Deprecated
    private static final char[] PASSWORD_AS_CHAR_ARRAY = PASSWORD.toCharArray();
    public static final String AGENT_CERT_OU = "Cruise agent certificate";
    private static final String INTERMEDIATE_CERT_OU = "Cruise intermediate certificate";
    private static final String CERT_EMAIL = "support@thoughtworks.com";
    private static final String FRIENDLY_NAME = "cruise";
    @Deprecated /*KeyStoreManager*/ private static final String KEYSTORE_TYPE = "JKS";
    private final KeyStoreManager keyStoreManager;

    public X509CertificateGenerator() {
        Security.addProvider(new BouncyCastleProvider());
        this.keyStoreManager = new KeyStoreManager();
    }

    public void createAndStoreX509Certificates(File keystore, File truststore, File agentKeystore,
                                               String password, String principalDn) {
        if (!keystore.exists()) {
            storeX509Certificate(keystore, password, createCertificateWithDn(principalDn));
        }

        if (!(truststore.exists() || agentKeystore.exists())) {
            storeX509Certificate(truststore, password, createAndStoreCACertificates(agentKeystore));
        }
    }

    private void storeX509Certificate(File file, String passwd, Registration entry) {
        try {
            PKCS12BagAttributeSetter.usingBagAttributeCarrier(entry.getPrivateKey())
                    .setFriendlyName(FRIENDLY_NAME)
                    .setLocalKeyId(entry.getPublicKey());

            keyStoreManager.storeX509Certificate(FRIENDLY_NAME, file, passwd, entry);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    public Registration createCertificateWithDn(String dn) {
        KeyPair keypair = generateKeyPair();
        Date epoch = new Date(0);
        X509Certificate certificate = createTypeOneX509Certificate(epoch, dn, keypair);
        return new Registration(keypair.getPrivate(), certificate);
    }

    private X509Certificate createTypeOneX509Certificate(Date startDate, String principalDn, KeyPair keyPair) {
        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal principal = new X500Principal(principalDn);
        certGen.setSerialNumber(serialNumber());
        certGen.setIssuerDN(principal);
        certGen.setNotBefore(startDate);
        DateTime now = new DateTime(new Date());
        certGen.setNotAfter(now.plusYears(YEARS).toDate());
        certGen.setSubjectDN(principal);                       // note: same as issuer
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm(new SystemEnvironment().get(GO_SSL_CERTS_ALGORITHM));

        try {
            return certGen.generate(keyPair.getPrivate(), "BC");
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    private X509Certificate createIntermediateCertificate(PrivateKey caPrivKey,
                                                          X509Certificate caCert,
                                                          Date startDate, KeyPair keyPair) throws Exception {
        X500Name issuerDn = JcaX500NameUtil.getSubject(caCert);

        X500NameBuilder subjectBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        subjectBuilder.addRDN(BCStyle.OU, INTERMEDIATE_CERT_OU);
        subjectBuilder.addRDN(BCStyle.EmailAddress, CERT_EMAIL);
        X500Name subjectDn = subjectBuilder.build();

        X509CertificateGenerator.V3X509CertificateGenerator v3CertGen = new V3X509CertificateGenerator(startDate,
                issuerDn, subjectDn, keyPair.getPublic(), serialNumber());

        // extensions
        v3CertGen.addSubjectKeyIdExtension(keyPair.getPublic());
        v3CertGen.addAuthorityKeyIdExtension(caCert);
        v3CertGen.addBasicConstraintsExtension();

        X509Certificate cert = v3CertGen.generate(caPrivKey);

        Date now = new Date();
        cert.checkValidity(now);
        cert.verify(caCert.getPublicKey());

        PKCS12BagAttributeSetter.usingBagAttributeCarrier(cert)
                .setFriendlyName(INTERMEDIATE_CERT_OU);

        PKCS12BagAttributeSetter.usingBagAttributeCarrier(keyPair.getPrivate())
                .setFriendlyName(FRIENDLY_NAME)
                .setLocalKeyId(keyPair.getPublic());

        return cert;
    }

    private X509Certificate createAgentCertificate(PublicKey publicKey, PrivateKey intermediatePrivateKey,
                                                   PublicKey intermediatePublicKey, String hostname,
                                                   Date startDate) throws Exception {

        X500NameBuilder issuerBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        issuerBuilder.addRDN(BCStyle.OU, INTERMEDIATE_CERT_OU);
        issuerBuilder.addRDN(BCStyle.EmailAddress, CERT_EMAIL);
        X500Name issuerDn = issuerBuilder.build();

        X500NameBuilder subjectBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        subjectBuilder.addRDN(BCStyle.OU, AGENT_CERT_OU);
        subjectBuilder.addRDN(BCStyle.CN, hostname);
        subjectBuilder.addRDN(BCStyle.EmailAddress, CERT_EMAIL);
        X500Name subjectDn = subjectBuilder.build();

        X509CertificateGenerator.V3X509CertificateGenerator v3CertGen = new V3X509CertificateGenerator(startDate,
                issuerDn, subjectDn, publicKey, BigInteger.valueOf(3));

        // add the extensions
        v3CertGen.addSubjectKeyIdExtension(publicKey);
        v3CertGen.addAuthorityKeyIdExtension(intermediatePublicKey);

        X509Certificate cert = v3CertGen.generate(intermediatePrivateKey);

        Date now = new Date();
        cert.checkValidity(now);
        cert.verify(intermediatePublicKey);

        PKCS12BagAttributeSetter.usingBagAttributeCarrier(cert)
                .setFriendlyName("cruise-agent")
                .setLocalKeyId(publicKey);

        return cert;
    }

    public Registration createAndStoreCACertificates(File keystore) {
        Date startDate = new Date(0);
        String principalDn = "ou=Cruise Server primary certificate, cn=" + getHostname();

        try {
            KeyPair caKeyPair = generateKeyPair();
            X509Certificate caCertificate = createTypeOneX509Certificate(startDate, principalDn, caKeyPair);

            KeyPair intKeyPair = generateKeyPair();
            X509Certificate intermediateCertificate = createIntermediateCertificate(
                    caKeyPair.getPrivate(), caCertificate, startDate, intKeyPair);

            Registration intermediateEntry = new Registration(intKeyPair.getPrivate(), intermediateCertificate);

            keyStoreManager.storeCACertificate(keystore, PASSWORD, caCertificate, intermediateEntry);

            return new Registration(intKeyPair.getPrivate(), intermediateCertificate, caCertificate);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create server certificates", e);
        }
    }

    public Registration createAgentCertificate(final File authorityKeystore, String agentHostname) {
        Date epoch = new Date(0);
        KeyPair agentKeyPair = generateKeyPair();
        try {
            KeyStore store = loadOrCreateCAKeyStore(authorityKeystore);
            KeyStore.PrivateKeyEntry intermediateEntry = (KeyStore.PrivateKeyEntry) store.getEntry("ca-intermediate",
                    new KeyStore.PasswordProtection(PASSWORD_AS_CHAR_ARRAY));

            Certificate[] chain = new Certificate[3];
            chain[2] = store.getCertificate("ca-cert");
            chain[1] = intermediateEntry.getCertificate();
            chain[0] = createAgentCertificate(agentKeyPair.getPublic(),
                    intermediateEntry.getPrivateKey(),
                    chain[1].getPublicKey(), agentHostname, epoch);
            return new Registration(agentKeyPair.getPrivate(), chain);
        } catch (Exception e) {
            throw bomb("Couldn't create agent certificate", e);
        }
    }

    private KeyStore loadOrCreateCAKeyStore(File authorityKeystore) throws Exception {
        KeyStore keyStore = keyStoreManager.tryLoad(authorityKeystore, PASSWORD);
        if (keyStore == null) {
            createAndStoreCACertificates(authorityKeystore);
            keyStore = keyStoreManager.load(authorityKeystore, PASSWORD);
        }
        return keyStore;
    }

    // Used for testing
    boolean verifySigned(File keystore, Certificate agentCertificate) {
        try {
            KeyStore store = KeyStore.getInstance(KEYSTORE_TYPE);
            FileInputStream inputStream = new FileInputStream(keystore);
            store.load(inputStream, PASSWORD_AS_CHAR_ARRAY);
            IOUtils.closeQuietly(inputStream);
            KeyStore.PrivateKeyEntry intermediateEntry = (KeyStore.PrivateKeyEntry) store.getEntry("ca-intermediate",
                    new KeyStore.PasswordProtection(PASSWORD_AS_CHAR_ARRAY));
            Certificate intermediateCertificate = intermediateEntry.getCertificate();
            agentCertificate.verify(intermediateCertificate.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw bomb(e);
        }
    }

    private BigInteger serialNumber() {
        return new BigInteger(Long.toString(Math.round(Math.random() * 11234455544545L)));
    }

    private KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("RSA", "BC").generateKeyPair();
        } catch (Exception e) {
            throw bomb("Couldn't create public-private key pair", e);
        }
    }

    private class V3X509CertificateGenerator {
        private final X509v3CertificateBuilder v3CertGen;

        public V3X509CertificateGenerator(Date startDate, X500Name issuerDn, X500Name subjectDn,
                                          PublicKey publicKey, BigInteger serialNumber) {
            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
            this.v3CertGen = new X509v3CertificateBuilder(issuerDn, serialNumber, startDate, new DateTime().plusYears(YEARS).toDate(), subjectDn, publicKeyInfo);
        }


        public void addSubjectKeyIdExtension(PublicKey key) throws CertificateParsingException, InvalidKeyException, IOException, NoSuchAlgorithmException {
            SubjectKeyIdentifier subjectKeyIdentifier = new JcaX509ExtensionUtils().createSubjectKeyIdentifier(key);
            v3CertGen.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
        }

        public void addAuthorityKeyIdExtension(X509Certificate cert) throws CertificateParsingException, CertificateEncodingException, CertIOException, NoSuchAlgorithmException {
            AuthorityKeyIdentifier authorityKeyIdentifier = new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(cert);
            v3CertGen.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
        }

        public void addAuthorityKeyIdExtension(PublicKey key) throws InvalidKeyException, CertIOException, NoSuchAlgorithmException {
            AuthorityKeyIdentifier authorityKeyIdentifier = new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(key);
            v3CertGen.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
        }

        private void addBasicConstraintsExtension() throws CertIOException {
            v3CertGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        }

        public X509Certificate generate(PrivateKey caPrivKey) throws Exception {
            ContentSigner contentSigner = new JcaContentSignerBuilder(new SystemEnvironment().get(GO_SSL_CERTS_PUBLIC_KEY_ALGORITHM)).setProvider("BC").build(caPrivKey);
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(v3CertGen.build(contentSigner));
        }
    }

}