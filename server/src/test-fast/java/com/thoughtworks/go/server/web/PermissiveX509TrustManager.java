/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.web;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * From https://github.com/test-load-balancer/tlb (pre http-components) e19d4911b089eeaf1a2c
 */
public class PermissiveX509TrustManager implements X509TrustManager {

    private X509TrustManager standardTrustManager = null;

    public PermissiveX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
        super();
        TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
        factory.init(keystore);

        TrustManager[] trustmanagers = factory.getTrustManagers();

        if (trustmanagers.length == 0)
            throw new NoSuchAlgorithmException("SunX509 trust manager not supported");

        this.standardTrustManager = (X509TrustManager) trustmanagers[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String string) throws CertificateException {
        this.standardTrustManager.checkClientTrusted(certificates, string);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String string) throws CertificateException {
        if ((certificates != null) && (certificates.length == 1)) {
            X509Certificate certificate = certificates[0];

            try {
                certificate.checkValidity();
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        } else {
            this.standardTrustManager.checkServerTrusted(certificates, string);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.standardTrustManager.getAcceptedIssuers();
    }
}
