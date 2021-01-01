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

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import java.io.File;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

public class AESCipherProvider implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AESCipherProvider.class);
    private final File cipherFile;

    private static volatile byte[] cachedKey;

    private final byte[] key;

    public AESCipherProvider(SystemEnvironment environment) {
        this.cipherFile = environment.getAESCipherFile();
        primeKeyCache();
        key = cachedKey;
    }

    public byte[] getKey() {
        return key;
    }

    private void primeKeyCache() {
        if (cachedKey == null) {
            synchronized (cipherFile.getAbsolutePath().intern()) {
                if (cachedKey == null) {
                    try {
                        if (cipherFile.exists()) {
                            cachedKey = decodeHex(FileUtils.readFileToString(cipherFile, UTF_8).trim());
                            return;
                        }
                        byte[] newKey = generateKey();
                        FileUtils.writeStringToFile(cipherFile, encodeHexString(newKey), UTF_8);
                        LOGGER.info("AES cipher not found. Creating a new cipher file");
                        cachedKey = newKey;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private byte[] generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128);
        byte[] key = keygen.generateKey().getEncoded();
        return key;
    }

    public void resetCipher() {
        removeCachedKey();
        primeKeyCache();
    }

    public void removeCachedKey() {
        cachedKey = null;
        FileUtils.deleteQuietly(cipherFile);
    }


}
