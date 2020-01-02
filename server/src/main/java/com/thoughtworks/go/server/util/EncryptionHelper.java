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


import org.bouncycastle.util.io.pem.PemReader;

import javax.crypto.*;
import java.io.IOException;
import java.io.StringReader;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.apache.commons.codec.CharEncoding.UTF_8;

public class EncryptionHelper {
    private static PublicKey getRSAPublicKeyFrom(String content) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemReader reader = new PemReader(new StringReader(content));
        EncodedKeySpec spec = new X509EncodedKeySpec(reader.readPemObject().getContent());
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static PrivateKey getRSAPrivateKeyFrom(String content) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemReader reader = new PemReader(new StringReader(content));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(reader.readPemObject().getContent());
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static String encryptUsingRSA(String plainText, String publicKeyContent) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, getRSAPublicKeyFrom(publicKeyContent));
        return Base64.getEncoder().encodeToString(encryptCipher.doFinal(plainText.getBytes(UTF_8)));
    }


    public static String decryptUsingRSA(String cipherText, String privateKeyContent) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, getRSAPrivateKeyFrom(privateKeyContent));
        return new String(decryptCipher.doFinal(Base64.getDecoder().decode(cipherText)), UTF_8);
    }

    public static boolean verifyRSASignature(String subordinatePublicKeyContent, String signatureContent, String masterPublicKeyContent) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        PublicKey masterPublicKey = getRSAPublicKeyFrom(masterPublicKeyContent);
        signatureContent = signatureContent.replace("\n", "");
        Signature signature = Signature.getInstance("SHA512withRSA");
        signature.initVerify(masterPublicKey);
        signature.update(subordinatePublicKeyContent.getBytes());
        return signature.verify(Base64.getDecoder().decode(signatureContent.getBytes()));
    }

    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128); // The AES key size in number of bits
        return generator.generateKey();
    }

    public static String encryptUsingAES(SecretKey secretKey, String dataToEncrypt) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] byteCipherText = aesCipher.doFinal(dataToEncrypt.getBytes());
        return Base64.getEncoder().encodeToString(byteCipherText);
    }

    public static String decryptUsingAES(SecretKey secretKey, String dataToDecrypt) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] bytePlainText = aesCipher.doFinal(Base64.getDecoder().decode(dataToDecrypt));
        return new String(bytePlainText);
    }
}
