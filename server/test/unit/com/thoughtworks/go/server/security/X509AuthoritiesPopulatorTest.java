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

package com.thoughtworks.go.server.security;

import org.junit.Assert;

import java.security.cert.X509Certificate;

import com.thoughtworks.go.security.X509CertificateGenerator;
import static com.thoughtworks.go.server.security.X509AuthoritiesPopulator.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.UserDetails;

public class X509AuthoritiesPopulatorTest {
    private final X509AuthoritiesPopulator populator = new X509AuthoritiesPopulator(ROLE_AGENT);

    @Test
    public void shouldNotReturnUserDetailsIfCertificateHasNoOu() {
        X509Certificate agentCertificate = new X509CertificateGenerator().createCertificateWithDn(
                "CN=hostname").getFirstCertificate();
        try {
            populator.getUserDetails(agentCertificate);
            Assert.fail("Oh dear. You should have thrown an exception, silly!");
        } catch (BadCredentialsException ignored) {
        }
    }

    @Test
    public void shouldReturnUserDetailsWithCorrectAuthorityIfAgentCertificateHasOu() {
        X509Certificate agentCertificate = new X509CertificateGenerator().createCertificateWithDn(
                "CN=hostname, OU=agent").getFirstCertificate();
        UserDetails userDetails = populator.getUserDetails(agentCertificate);
        GrantedAuthority[] actual = userDetails.getAuthorities();
        GrantedAuthority expected = new GrantedAuthorityImpl(ROLE_AGENT);
        assertThat(actual.length, is(1));
        assertThat(actual[0], is(expected));
        assertThat(userDetails.getUsername(), is("_go_agent_hostname"));
    }

}
