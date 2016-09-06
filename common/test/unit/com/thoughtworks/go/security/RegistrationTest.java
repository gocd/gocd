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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class RegistrationTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void decodeFromJson() throws IOException {
        Registration origin = createRegistration();
        Registration reg = RegistrationJSONizer.fromJson(RegistrationJSONizer.toJson(origin));
        assertThat(reg.getPrivateKey(), is(origin.getPrivateKey()));
        assertThat(reg.getPublicKey(), is(origin.getPublicKey()));
        assertThat(reg.getChain(), is(origin.getChain()));
        assertThat(reg.getCertificateNotBeforeDate(), is(origin.getCertificateNotBeforeDate()));
        assertThat(reg.getFirstCertificate(), is(origin.getFirstCertificate()));
        assertThat(reg.getChain().length, is(3));
    }


    @Test
    public void shouldBeValidWhenPrivateKeyAndChainIsPresent() throws Exception {
        assertFalse(Registration.createNullPrivateKeyEntry().isValid());
        assertFalse(new Registration(null, null).isValid());
        assertFalse(new Registration(null).isValid());

        assertTrue(createRegistration().isValid());
    }

    @Test
    public void registrationFromEmptyJSONShouldBeInvalid() throws Exception {
        assertFalse(RegistrationJSONizer.fromJson("{}").isValid());
    }

    @Test
    public void shouldEncodeDecodeEmptyRegistration() throws Exception {
        Registration toSerialize = Registration.createNullPrivateKeyEntry();
        Registration deserialized = RegistrationJSONizer.fromJson(RegistrationJSONizer.toJson(toSerialize));

        assertTrue(EqualsBuilder.reflectionEquals(toSerialize, deserialized));
    }

    private Registration createRegistration() throws IOException {
        File tempKeystoreFile = tmpFolder.newFile();
        X509CertificateGenerator certificateGenerator = new X509CertificateGenerator();
        certificateGenerator.createAndStoreCACertificates(tempKeystoreFile);
        return certificateGenerator.createAgentCertificate(tempKeystoreFile, "blah");
    }

}
