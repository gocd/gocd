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

package com.thoughtworks.go.security;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Date;

import com.thoughtworks.go.util.TempFiles;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class X509CertificateGeneratorTest {
    private TempFiles tempFiles;
    File keystore;

    @Before
    public void setup() {
        tempFiles = new TempFiles();
        keystore = new File(tempFiles.createUniqueFolder("X509CertificateGeneratorTest"), "keystore");
    }

    @After
    public void tearDown() {
        tempFiles.cleanUp();
    }

    @Test
    public void shouldSaveIntermediateCertificateAndRetrieveItToCreateNewAgentCertificate() throws Exception {
        X509CertificateGenerator generator = new X509CertificateGenerator();

        generator.createAndStoreCACertificates(keystore);
        Certificate agentCertificate = generator.createAgentCertificate(keystore, "hostname").getChain()[0];
        assertTrue(generator.verifySigned(keystore, agentCertificate));
    }

    @Test
    public void shouldCreateCertsThatIsValidFromEpochToNowPlusTenYears() throws Exception {
        X509CertificateGenerator generator = new X509CertificateGenerator();
        Registration caCert = generator.createAndStoreCACertificates(keystore);
        Date epoch = new Date(0);
        X509Certificate serverCert = caCert.getFirstCertificate();
        serverCert.checkValidity(epoch); // does not throw CertificateNotYetValidException
        serverCert.checkValidity(DateUtils.addYears(new Date(), 9)); // does not throw CertificateNotYetValidException
    }

    @Test
    public void shouldCreateCertsForAgentThatIsValidFromEpochToNowPlusTenYears() throws Exception {
        X509CertificateGenerator generator = new X509CertificateGenerator();
        Registration agentCertChain = generator.createAgentCertificate(keystore, "agentHostName");
        Date epoch = new Date(0);
        X509Certificate agentCert = agentCertChain.getFirstCertificate();
        agentCert.checkValidity(epoch); // does not throw CertificateNotYetValidException
        agentCert.checkValidity(DateUtils.addYears(new Date(), 9)); // does not throw CertificateNotYetValidException
    }

    @Test
    public void shouldCreateCertWithDnThatIsValidFromEpochToNowPlusTenYears() throws Exception {
        X509CertificateGenerator generator = new X509CertificateGenerator();
        Registration certChain = generator.createCertificateWithDn("CN=hostname");
        Date epoch = new Date(0);
        X509Certificate cert = certChain.getFirstCertificate();
        cert.checkValidity(epoch); // does not throw CertificateNotYetValidException
        cert.checkValidity(DateUtils.addYears(new Date(), 9)); // does not throw CertificateNotYetValidException
    }

    @Test
    public void shouldGeneratePrivateKeyWithCRTFactorsForCompatibilityWithOtherPlatform() throws Exception {
        X509CertificateGenerator generator = new X509CertificateGenerator();
        Registration registration = generator.createAgentCertificate(keystore, "agentHostName");
        assertThat(registration.getPrivateKey(), instanceOf(RSAPrivateCrtKey.class));
        RSAPrivateCrtKey key = (RSAPrivateCrtKey) registration.getPrivateKey();
        assertThat(key.getModulus().signum(), is(1));
        assertThat(key.getPrivateExponent().signum(), is(1));
        assertThat(key.getPrimeP().signum(), is(1));
        assertThat(key.getPrimeExponentQ().signum(), is(1));
        assertThat(key.getCrtCoefficient().signum(), is(1));
    }

    @Test
    public void shouldGenerateValidRSAKeyPair() throws Exception {
        String text = "this is a secret message";
        X509CertificateGenerator generator = new X509CertificateGenerator();
        Registration registration = generator.createAgentCertificate(keystore, "agentHostName");
        PrivateKey privateKey = registration.getPrivateKey();
        PublicKey publicKey = registration.getPublicKey();

        final Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(text.getBytes());

        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(encrypted);
        assertThat(decrypted, is(text.getBytes()));
    }

}
