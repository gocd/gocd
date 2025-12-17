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

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.DecoderException;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.decodeHex;

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
                            cachedKey = decodeHex(Files.readString(cipherFile.toPath(), UTF_8).trim());
                        } else {
                            cachedKey = new byte[]{}; // Cache an empty key
                            LOGGER.info("DES cipher not found. Ignoring... we do not support generating new DES keys.");
                        }
                    } catch (DecoderException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @TestOnly
    static void removeCachedKey() {
        cachedKey = null;
    }

    @TestOnly
    public void removeCipher() throws IOException {
        Files.deleteIfExists(cipherFile.toPath());
        DESCipherProvider.removeCachedKey();
    }
}
