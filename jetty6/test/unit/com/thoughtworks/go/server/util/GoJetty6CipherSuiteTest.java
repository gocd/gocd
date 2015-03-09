package com.thoughtworks.go.server.util;

import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoJetty6CipherSuiteTest {
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
        GoJetty6CipherSuite goCipherSuite = new GoJetty6CipherSuite(socketFactory);

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
        GoJetty6CipherSuite goCipherSuite = new GoJetty6CipherSuite(socketFactory);

        assertTrue(Arrays.asList(goCipherSuite.getExcludedCipherSuites()).isEmpty());
    }

}