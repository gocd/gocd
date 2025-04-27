/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.apiv1.versioninfos.models;


import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptionHelper {
    private static PublicKey getRSAPublicKeyFrom(String pemContent) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String rawPublicKey = pemContent.replaceAll("-----BEGIN PUBLIC KEY-----", "")
            .replaceAll("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", ""); // remove line breaks and spaces
        EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(rawPublicKey));
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static boolean verifyRSASignature(String subordinatePublicKeyContent, String signatureContent, String masterPublicKeyContent) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        PublicKey masterPublicKey = getRSAPublicKeyFrom(masterPublicKeyContent);
        signatureContent = signatureContent.replace("\n", "");
        Signature signature = Signature.getInstance("SHA512withRSA");
        signature.initVerify(masterPublicKey);
        signature.update(subordinatePublicKeyContent.getBytes());
        return signature.verify(Base64.getDecoder().decode(signatureContent.getBytes()));
    }
}
