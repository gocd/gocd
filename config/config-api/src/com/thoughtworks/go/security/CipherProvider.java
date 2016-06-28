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

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.DESKeyGenerator;
import org.bouncycastle.crypto.params.DESParameters;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.UUID;

public class CipherProvider implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(CipherProvider.class);

    private SystemEnvironment environment;
    private static volatile byte[] cachedKey;

    private final byte[] key;

    public CipherProvider(SystemEnvironment environment) {
        this.environment = environment;
        primeKeyCache();
        key = cachedKey;
    }

    public byte[] getKey() {
        return key;
    }

    private void primeKeyCache() {
        if (cachedKey == null) {
            File cipherFile = environment.getCipherFile();
            synchronized (cipherFile.getAbsolutePath().intern()) {
                if (cachedKey == null) {
                    try {
                        if (cipherFile.exists()) {
                            cachedKey = FileUtils.readFileToByteArray(cipherFile);
                            return;
                        }
                        byte[] newKey = generateKey();
                        FileUtils.writeByteArrayToFile(cipherFile, newKey);
                        LOGGER.info("Cipher not found. Creating a new cipher file");
                        cachedKey = newKey;
                    } catch (IOException e) {
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
        return Hex.encode(generator.generateKey());
    }

    public void resetCipher() {
        cachedKey = null;
        FileUtils.deleteQuietly(environment.getCipherFile());
        primeKeyCache();
    }
}
