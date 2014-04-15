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

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class KeyPairCreator {
    public static KeyPair createKeyPair() {
        try {
            KeyPair seed = KeyPairGenerator.getInstance("RSA", "BC").generateKeyPair();
            RSAPrivateKey privateSeed = (RSAPrivateKey) seed.getPrivate();
            RSAPublicKey publicSeed = (RSAPublicKey) seed.getPublic();
            KeyFactory fact = KeyFactory.getInstance("RSA", "BC");
            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(
                    privateSeed.getModulus(),
                    privateSeed.getPrivateExponent()
            );
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                    publicSeed.getModulus(),
                    publicSeed.getPublicExponent()
            );
            return new KeyPair(fact.generatePublic(publicKeySpec), fact.generatePrivate(privateKeySpec));
        } catch (Exception e) {
            throw bomb("Couldn't create public-private key pair", e);
        }
    }
}
