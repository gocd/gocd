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
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.DESKeyGenerator;
import org.bouncycastle.crypto.params.DESParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

@Deprecated
public class DESCipherProvider implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DESCipherProvider.class);

    private static volatile byte[] cachedKey;

    private final byte[] key;
    private final File cipherFile;

    public DESCipherProvider(SystemEnvironment environment) {
        this.cipherFile = environment.getDESCipherFile();
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
                        LOGGER.info("DES cipher not found. Creating a new cipher file");
                        cachedKey = newKey;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private byte[] generateKey() {
        SecureRandom random = new SecureRandom();
        random.setSeed(UUID.randomUUID().toString().getBytes());
        KeyGenerationParameters generationParameters = new KeyGenerationParameters(random, DESParameters.DES_KEY_LENGTH * 8);
        DESKeyGenerator generator = new DESKeyGenerator();
        generator.init(generationParameters);
        return generator.generateKey();
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
