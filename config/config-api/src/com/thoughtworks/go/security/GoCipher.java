/*
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
 *
 */

package com.thoughtworks.go.security;

import java.io.Serializable;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

public class GoCipher implements Serializable {

    private CipherProvider cipherProvider;

    public GoCipher() {
        this(new CipherProvider(new SystemEnvironment()));
    }

    public GoCipher(CipherProvider cipherProvider) {
        this.cipherProvider = cipherProvider;
    }

    public String encrypt(String plainText) throws InvalidCipherTextException {
        return cipher(this.cipherProvider.getKey(), plainText);
    }

    public String decrypt(String cipherText) throws InvalidCipherTextException {//TODO: #5086 kill checked exception
        return decipher(this.cipherProvider.getKey(), cipherText);
    }

    public String cipher(byte[] key, String plainText) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESEngine()));
        KeyParameter keyParameter = new KeyParameter(Hex.decode(key));
        cipher.init(true, keyParameter);
        byte[] plainTextBytes = plainText.getBytes();
        byte[] cipherTextBytes = new byte[cipher.getOutputSize(plainTextBytes.length)];
        int outputLength = cipher.processBytes(plainTextBytes, 0, plainTextBytes.length, cipherTextBytes, 0);
        cipher.doFinal(cipherTextBytes, outputLength);
        return Base64.encodeBase64String(cipherTextBytes).trim();
    }

    public String decipher(byte[] key, String cipherText) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESEngine()));
        cipher.init(false, new KeyParameter(Hex.decode(key)));
        byte[] cipherTextBytes = Base64.decodeBase64(cipherText);
        byte[] plainTextBytes = new byte[cipher.getOutputSize(cipherTextBytes.length)];
        int outputLength = cipher.processBytes(cipherTextBytes, 0, cipherTextBytes.length, plainTextBytes, 0);
        cipher.doFinal(plainTextBytes, outputLength);
        int paddingStarts = plainTextBytes.length - 1;
        for (; paddingStarts >= 0 ; paddingStarts--) {
            if (plainTextBytes[paddingStarts] != 0) {
                break;
            }
        }
        return new String(plainTextBytes, 0, paddingStarts + 1);
    }
}
