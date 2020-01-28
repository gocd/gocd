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
package com.thoughtworks.go.server.newsecurity.x509;


import com.thoughtworks.go.ClearSingleton;
import net.sf.ehcache.Element;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.security.Principal;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class CachingSubjectDnX509PrincipalExtractorTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();
    private X509Certificate x509Certificate;
    private CachingSubjectDnX509PrincipalExtractor principalExtractor;
    private Principal principal;

    @BeforeEach
    public void setUp() throws Exception {
        x509Certificate = mock(X509Certificate.class);
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("CN=bob,OU=Acme Corp");
        when(x509Certificate.getSubjectDN()).thenReturn(principal);

        principalExtractor = new CachingSubjectDnX509PrincipalExtractor();
    }

    @Test
    public void shouldExtractPrincipalFromCertificate() {
        String username = (String) principalExtractor.extractPrincipal(x509Certificate);
        assertThat(username).isEqualTo("bob");
    }

    @Test
    public void shouldPopulateCacheWithPrincipal() {
        String username = (String) principalExtractor.extractPrincipal(x509Certificate);
        assertThat(username).isEqualTo("bob");
        assertThat(principalExtractor.getCache().get(x509Certificate).getValue()).isEqualTo("bob");
    }

    @Test
    public void shouldGetPrincipalFromCache() {
        principalExtractor.getCache().put(new Element(x509Certificate, "bob"));
        String username = (String) principalExtractor.extractPrincipal(x509Certificate);
        assertThat(username).isEqualTo("bob");
        verifyZeroInteractions(principal, x509Certificate);
    }
}