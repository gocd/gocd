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

package com.thoughtworks.go.server;

import java.util.Arrays;
import javax.net.ssl.SSLSocketFactory;

import com.thoughtworks.go.server.util.GoCipherSuite;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoCipherSuiteTest {
    @Test
    public void shouldExcludeEverySuiteOtherThanTheMagicThreeWhichAreSupportedByOurOldJetty() throws Exception {
        SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);

        when(socketFactory.getSupportedCipherSuites()).thenReturn(new String[]{
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_RSA_WITH_AES_128_CBC_SHA256"
                ,"SSL_RSA_WITH_RC4_128_SHA"
                ,"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_DHE_RSA_WITH_AES_128_CBC_SHA256"
                ,"SSL_RSA_EXPORT_WITH_RC4_40_MD5"
                ,"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"
                ,"SSL_RSA_WITH_RC4_128_MD5"
                ,"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"});
        GoCipherSuite goCipherSuite = new GoCipherSuite(socketFactory);

        assertThat(Arrays.asList(goCipherSuite.getExcludedCipherSuites()), is(Arrays.asList(
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_DHE_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"
                ,"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")));
    }

    @Test
    public void shouldNotExcludeAnySuiteIfTheMagicThreeDoNotExist() throws Exception {
        SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);

        when(socketFactory.getSupportedCipherSuites()).thenReturn(new String[]{
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_DHE_RSA_WITH_AES_128_CBC_SHA256"
                ,"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"
                ,"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"
                ,"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"});
        GoCipherSuite goCipherSuite = new GoCipherSuite(socketFactory);

        assertTrue(Arrays.asList(goCipherSuite.getExcludedCipherSuites()).isEmpty());
    }
}