/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.io.Serializable;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

public class Registration implements Serializable {

    private final PrivateKey privateKey;
    private final Certificate[] chain;

    public static Registration createNullPrivateKeyEntry() {
        return new Registration(null);
    }

    public Registration(PrivateKey privateKey, Certificate... chain) {
        this.privateKey = privateKey;
        this.chain = chain;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return getFirstCertificate().getPublicKey();
    }

    public Certificate[] getChain() {
        return chain;
    }

    public X509Certificate getFirstCertificate() {
        return (X509Certificate) chain[0];
    }

    public Date getCertificateNotBeforeDate() {
        return getFirstCertificate().getNotBefore();
    }

    public KeyStore.PrivateKeyEntry asKeyStoreEntry() {
        return new KeyStore.PrivateKeyEntry(privateKey, chain);
    }

    public boolean isValid() {
        return privateKey != null && chain != null && chain.length > 0;
    }

}
