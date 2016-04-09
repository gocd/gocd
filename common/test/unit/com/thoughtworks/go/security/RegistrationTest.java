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

package com.thoughtworks.go.security;

import com.thoughtworks.go.util.TestFileUtil;
import org.bouncycastle.asn1.x509.X509Name;
import org.junit.Test;

import javax.security.auth.x500.X500Principal;
import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class RegistrationTest {
    private static String authorityKeystorePath = "tempAuthorityKeystore";

    @Test
    public void decodeFromJson() {
        Registration origin = createRegistration();
        Registration reg = Registration.fromJson(origin.toJson());
        assertThat(reg.getPrivateKey(), is(origin.getPrivateKey()));
        assertThat(reg.getPublicKey(), is(origin.getPublicKey()));
        assertThat(reg.getChain(), is(origin.getChain()));
        assertThat(reg.getCertificateNotBeforeDate(), is(origin.getCertificateNotBeforeDate()));
        assertThat(reg.getFirstCertificate(), is(origin.getFirstCertificate()));
        assertThat(reg.getChain().length, is(3));
    }

    public static Registration createRegistration() {
        File tempKeystoreFile = TestFileUtil.createUniqueTempFile(authorityKeystorePath);
        X509CertificateGenerator certificateGenerator = new X509CertificateGenerator();
        certificateGenerator.createAndStoreCACertificates(tempKeystoreFile);
        return certificateGenerator.createAgentCertificate(tempKeystoreFile, "blah");
    }

}
