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

import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.jce.X509Principal;

/**
 * Helper to create a X509 Principal with ordered identifiers
 */
public class X509PrincipalGenerator {
    private final Hashtable<DERObjectIdentifier, String> attrs = new Hashtable<>();
    private final Vector<DERObjectIdentifier> order = new Vector<>();

    public static X509Principal createX509Principal(PrincipalIdentifier... identifiers) {
        return new X509PrincipalGenerator(identifiers).principal();
    }

    public static PrincipalIdentifier withOU(String ou) {
        return new PrincipalIdentifier(X509Principal.OU, ou);
    }

    public static PrincipalIdentifier withCN(String cn) {
        return new PrincipalIdentifier(X509Principal.CN, cn);
    }

    public static PrincipalIdentifier withEmailAddress(String emailAddress) {
        return new PrincipalIdentifier(X509Principal.EmailAddress, emailAddress);
    }

    private X509PrincipalGenerator(PrincipalIdentifier... identifiers) {
        for (PrincipalIdentifier identifier : identifiers) {
            order.addElement(identifier.identifier);
            attrs.put(identifier.identifier, identifier.value);
        }
    }

    private X509Principal principal() {
        return new X509Principal(order, attrs);
    }

    public static class PrincipalIdentifier {
        private DERObjectIdentifier identifier;
        private String value;

        private PrincipalIdentifier(DERObjectIdentifier identifier, String value) {
            this.identifier = identifier;
            this.value = value;
        }
    }
}
