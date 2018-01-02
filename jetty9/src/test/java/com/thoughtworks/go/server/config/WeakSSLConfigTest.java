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

package com.thoughtworks.go.server.config;

import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WeakSSLConfigTest {
    @Test
    public void shouldIncludeTheMagicThreeWhichAreSupportedByOurJetty() throws Exception {
        SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);

        when(socketFactory.getSupportedCipherSuites()).thenReturn(new String[]{
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"
                , "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"
                , "TLS_RSA_WITH_AES_128_CBC_SHA256"
                , "SSL_RSA_WITH_RC4_128_SHA"
                , "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256"
                , "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256"
                , "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256"
                , "SSL_RSA_EXPORT_WITH_RC4_40_MD5"
                , "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"
                , "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"
                , "SSL_RSA_WITH_RC4_128_MD5"
                , "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"});
        WeakSSLConfig sslConfig = new WeakSSLConfig(socketFactory);

        assertThat(Arrays.asList(sslConfig.getCipherSuitesToBeIncluded()), is(Arrays.asList("SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_WITH_RC4_128_MD5")));
    }

    @Test
    public void shouldIncludeAllSuitesIfTheMagicThreeDoNotExist() throws Exception {
        SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);

        String[] supportedSuites = {
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"
                , "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"
                , "TLS_RSA_WITH_AES_128_CBC_SHA256"
                , "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256"
                , "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256"
                , "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256"
                , "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"
                , "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"
                , "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"};
        when(socketFactory.getSupportedCipherSuites()).thenReturn(supportedSuites);
        WeakSSLConfig goSSLConfig = new WeakSSLConfig(socketFactory);

        List<String> includedSuites = Arrays.asList(goSSLConfig.getCipherSuitesToBeIncluded());
        assertThat(includedSuites.size(), is(supportedSuites.length));

        for (String cipherSuite : includedSuites) {
            assertThat(Arrays.asList(supportedSuites).contains(cipherSuite), is(true));
        }
    }
}
