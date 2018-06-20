/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.apache.commons.codec.CharEncoding.UTF_8;

public class RSAEncryptionHelper {
    private static PublicKey getPublicKeyFrom(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKeyContent = new String(Files.readAllBytes(Paths.get(path)));
        publicKeyContent = publicKeyContent.replaceAll("\\n", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");

        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent)));
    }

    private static PrivateKey getPrivateKeyFrom(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKeyContent = new String(Files.readAllBytes(Paths.get(path)));
        privateKeyContent = privateKeyContent.replaceAll("\\n", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");

        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent)));
    }

    public static String encrypt(String plainText, String publicKeyPath) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, getPublicKeyFrom(publicKeyPath));
        return Base64.getEncoder().encodeToString(encryptCipher.doFinal(plainText.getBytes(UTF_8)));
    }


    public static String decrypt(String cipherText, String privateKeyPath) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, getPrivateKeyFrom(privateKeyPath));
        return new String(decryptCipher.doFinal(Base64.getDecoder().decode(cipherText)), UTF_8);
    }
}
