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

package com.thoughtworks.go.security;

import org.springframework.util.Assert;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ServiceLoader;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class AESEncrypter implements Encrypter, Serializable {
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private final AESCipherProvider cipherProvider;

    private static IVProvider ivProvider;

    private IVProvider getIvProviderInstance() {
        if (ivProvider == null) {
            synchronized (AESEncrypter.class) {
                if (ivProvider == null) {
                    ivProvider = ServiceLoader.load(IVProvider.class).iterator().next();
                }
            }
        }
        return ivProvider;
    }

    public AESEncrypter(AESCipherProvider cipherProvider) {
        this.cipherProvider = cipherProvider;
    }

    @Override
    public boolean canDecrypt(String cipherText) {
        if (isBlank(cipherText)) {
            return false;
        }
        String[] splits = cipherText.split(":");
        return splits.length == 3 && "AES".equals(splits[0]) && isNotBlank(splits[1]) && isNotBlank(splits[2]);
    }

    @Override
    public String encrypt(String plainText) throws CryptoException {
        try {
            byte[] initializationVector = getIvProviderInstance().createIV();
            Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, createSecretKeySpec(), new IvParameterSpec(initializationVector));

            byte[] bytesToEncrypt = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = encryptCipher.doFinal(bytesToEncrypt);

            return String.join(":", "AES", ENCODER.encodeToString(initializationVector), ENCODER.encodeToString(encryptedBytes));
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public String decrypt(String cipherText) throws CryptoException {
        try {
            Assert.isTrue(canDecrypt(cipherText), "bad cipher text");

            String[] splits = cipherText.split(":");

            String encodedIV = splits[1];
            String encodedCipherText = splits[2];

            byte[] initializationVector = DECODER.decode(encodedIV);
            Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, createSecretKeySpec(), new IvParameterSpec(initializationVector));

            byte[] decryptedBytes = decryptCipher.doFinal(DECODER.decode(encodedCipherText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

    private SecretKeySpec createSecretKeySpec() {
        return new SecretKeySpec(cipherProvider.getKey(), "AES");
    }
}
