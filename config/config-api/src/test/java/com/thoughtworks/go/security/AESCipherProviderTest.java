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
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ResetCipher.class)
class AESCipherProviderTest {

    private final SystemEnvironment env = new SystemEnvironment();

    @AfterEach
    public void tearDown() {
        new SystemEnvironment().clearProperty(SystemEnvironment.CONFIG_DIR_PROPERTY);
    }

    @Test
    public void shouldGenerateKeyIfNotFound() throws Exception {
        byte[] key = new AESCipherProvider(env).getKey();
        assertThat(key).hasSize(16).isNotEqualTo(Hex.decodeHex(ResetCipher.AES_CIPHER_HEX));
        assertThat(new AESCipherProvider(env).getKey()).isSameAs(key);
    }

    @Test
    public void shouldBeAbleToLoadExistingCipher(ResetCipher resetCipher) throws Exception {
        resetCipher.setupAESCipherFile();
        byte[] key = new AESCipherProvider(env).getKey();
        assertThat(key).isEqualTo(Hex.decodeHex(ResetCipher.AES_CIPHER_HEX));
        assertThat(new AESCipherProvider(env).getKey()).isSameAs(key);
    }

    /**
     * Validates that we can create files (and ensure directories are created) inside a symlinked config directory.
     * Note that use of Files.createDirectories() inside the cipher provider will fail on Linux if the target directory
     * although was finally addressed in 17.0.14 via https://bugs.openjdk.org/browse/JDK-8294193 (and always correct on 21)
     */
    @Test
    public void shouldStoreFileInsideSymlinkedConfigDirs(@TempDir Path tempDir) throws Exception {
        Path targetConfig = tempDir.resolve("target-config");
        Path symlinkedConfig = tempDir.resolve("symlink-config-dir");
        Files.createDirectory(targetConfig);
        Files.createSymbolicLink(symlinkedConfig, targetConfig);

        new SystemEnvironment().setProperty(SystemEnvironment.CONFIG_DIR_PROPERTY, symlinkedConfig.toString());

        assertThat(new AESCipherProvider(env).getKey())
            .hasSize(16).isNotEqualTo(Hex.decodeHex(ResetCipher.AES_CIPHER_HEX));
    }
}