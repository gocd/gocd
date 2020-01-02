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
package com.thoughtworks.go.server.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomSslContextFactoryTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    private CustomSslContextFactory customSslContextFactory;

    @Before
    public void setUp() {
        customSslContextFactory = new CustomSslContextFactory();
    }

    @Test
    public void shouldWrapX509TrustManagerWithACustomManager() throws Exception {
        TrustManager[] trustManagers = customSslContextFactory.getTrustManagers(mock(KeyStore.class), null);
        assertThat(trustManagers.length, is(1));
        assertTrue(trustManagers[0] instanceof CustomSslContextFactory.CustomX509TrustManager);
    }

    @Test
    public void shouldReraiseAnyExceptionThrown() throws CertificateException {
        X509TrustManager mock = mock(X509TrustManager.class);
        RuntimeException runtimeException = new RuntimeException("boo");
        X509Certificate[] x509Certificates = new X509Certificate[0];
        when(mock.getAcceptedIssuers()).thenReturn(new X509Certificate[0]);
        doThrow(runtimeException).when(mock).checkClientTrusted(x509Certificates, null);
        CustomSslContextFactory.CustomX509TrustManager trustManager = new CustomSslContextFactory.CustomX509TrustManager(mock);

        try {
            trustManager.checkClientTrusted(x509Certificates, null);
            fail("Expecting exception");
        } catch (Exception e) {
            assertSame(e, runtimeException);
        }
    }
}
