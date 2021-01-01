/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.Serializable;
import java.util.Base64;

@Deprecated
public class DESEncrypter implements Encrypter, Serializable {
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

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
        try {
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESEngine()));
            cipher.init(false, new KeyParameter(key));
            byte[] cipherTextBytes = DECODER.decode(cipherText);

            byte[] plainTextBytes = new byte[cipher.getOutputSize(cipherTextBytes.length)];
            int outputLength = cipher.processBytes(cipherTextBytes, 0, cipherTextBytes.length, plainTextBytes, 0);
            cipher.doFinal(plainTextBytes, outputLength);
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

    public static String reEncryptUsingNewKey(byte[] oldCipher, byte[] newCipher, String encryptedValue) throws CryptoException {
        String plainText = decrypt(oldCipher, encryptedValue);
        return encrypt(newCipher, plainText);
    }

    private static String encrypt(byte[] key, String plainText) throws CryptoException {
        try {
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESEngine()));
            KeyParameter keyParameter = new KeyParameter(key);
            cipher.init(true, keyParameter);
            byte[] plainTextBytes = plainText.getBytes();
            byte[] cipherTextBytes = new byte[cipher.getOutputSize(plainTextBytes.length)];
            int outputLength = cipher.processBytes(plainTextBytes, 0, plainTextBytes.length, cipherTextBytes, 0);
            cipher.doFinal(cipherTextBytes, outputLength);
            return ENCODER.encodeToString(cipherTextBytes).trim();
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

}
