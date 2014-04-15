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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AuthSSLKeyManagerFactory {
    private final File keystoreFile;
    private final String keystorePassword;
    private static final Log LOG = LogFactory.getLog(AuthSSLKeyManagerFactory.class);

    public AuthSSLKeyManagerFactory(File keystoreFile, String keystorePassword) {
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
    }

    public KeyManager[] keyManagers() {
        try {
            KeyStore store = new KeyStoreManager().loadOrEmpty(keystoreFile, keystorePassword);
            if (LOG.isDebugEnabled()) { logKeyStore(store); }
            return createKeyManagers(store);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get key managers", e);
        }
    }

    private KeyManager[] createKeyManagers(KeyStore keystore)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        bombIfNull(keystore, "Keystore may not be null");

        LOG.trace("Initializing key manager");
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, keystorePassword.toCharArray());
        return kmfactory.getKeyManagers();
    }

    private void logKeyStore(KeyStore store) throws KeyStoreException {
        LOG.trace("Certificates count: " + store.size());
        Enumeration aliases = store.aliases();
        while (aliases.hasMoreElements()) {
            String alias = (String) aliases.nextElement();
            Certificate[] certs = store.getCertificateChain(alias);
            if (certs != null) {
                LOG.debug("Certificate chain '" + alias + "':");
                for (int c = 0; c < certs.length; c++) {
                    if (certs[c] instanceof X509Certificate) {
                        X509Certificate cert = (X509Certificate) certs[c];
                        LOG.trace(" Certificate " + (c + 1) + ":");
                        LOG.trace("  Subject DN: " + cert.getSubjectDN());
                        LOG.trace("  Signature Algorithm: " + cert.getSigAlgName());
                        LOG.trace("  Valid from: " + cert.getNotBefore());
                        LOG.trace("  Valid until: " + cert.getNotAfter());
                        LOG.trace("  Issuer: " + cert.getIssuerDN());
                    }
                }
            }
        }
    }

}

