/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

/**
 * Helper to set useful Bag Attributes
 */
class PKCS12BagAttributeSetter {
    private final PKCS12BagAttributeCarrier carrier;

    public static PKCS12BagAttributeSetter usingBagAttributeCarrier(PrivateKey privateKey) {
        return new PKCS12BagAttributeSetter((PKCS12BagAttributeCarrier) privateKey);
    }

    public static PKCS12BagAttributeSetter usingBagAttributeCarrier(X509Certificate cert) {
        return new PKCS12BagAttributeSetter((PKCS12BagAttributeCarrier) cert);
    }

    private PKCS12BagAttributeSetter(PKCS12BagAttributeCarrier carrier) {
        this.carrier = carrier;
    }

    public PKCS12BagAttributeSetter setFriendlyName(String name) {
        carrier.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(name));
        return this;
    }

    public PKCS12BagAttributeSetter setLocalKeyId(PublicKey key) throws CertificateParsingException, InvalidKeyException {
        carrier.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, new SubjectKeyIdentifier(key.getEncoded()));
        return this;
    }
}