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
package com.thoughtworks.go.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.util.Base64;

@Deprecated
public class DESEncrypter implements Encrypter, Serializable {
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private final DESCipherProvider cipherProvider;

    public DESEncrypter(DESCipherProvider cipherProvider) {
        this.cipherProvider = cipherProvider;
    }

    @Override
    public boolean canDecrypt(String cipherText) {
        return !cipherText.startsWith("AES:");
    }

    @Override
    public String encrypt(String plainText) {
        throw new UnsupportedOperationException("Encrypting using DES is no longer supported!");
    }

    @Override
    public String decrypt(String cipherText) throws CryptoException {
        return decrypt(cipherProvider.getKey(), cipherText);
    }

    private static String decrypt(byte[] key, String cipherText) throws CryptoException {
        if (key == null || key.length == 0) {
            throw new IllegalStateException("No DES key found; cannot decrypt and generating new DES keys is no longer supported");
        }
        try {
            SecretKey secretKey = new SecretKeySpec(key, "DES");
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[8])); // IV was not used in this legacy implementation.
            byte[] cipherTextBytes = DECODER.decode(cipherText);

            byte[] plainTextBytes = cipher.doFinal(cipherTextBytes);
            int paddingStarts = plainTextBytes.length - 1;
            for (; paddingStarts >= 0; paddingStarts--) {
                if (plainTextBytes[paddingStarts] != 0) {
                    break;
                }
            }
            return new String(plainTextBytes, 0, paddingStarts + 1);
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }
}
