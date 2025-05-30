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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@ExtendWith(ResetCipher.class)
public class DesToAesMigratorTest {
    private File desCipherFile;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    void setUp() {
        systemEnvironment = spy(new SystemEnvironment());
        desCipherFile = systemEnvironment.getDESCipherFile();
    }

    @Test
    void shouldNotCreateDesCipherIfCipherFileMissing() {
        assertThat(desCipherFile).doesNotExist();

        DesToAesMigrator migrator = new DesToAesMigrator(systemEnvironment);
        assertThat(migrator.desEncrypter).isNull();
    }

    @Test
    void shouldCreateDesEncryptorIfCipherFileIsPresent(ResetCipher resetCipher) throws IOException {
        resetCipher.setupDESCipherFile();
        assertThat(desCipherFile).exists();

        DesToAesMigrator migrator = new DesToAesMigrator(systemEnvironment);

        assertThat(migrator.desEncrypter).isNotNull();
    }

    @Test
    void shouldConvertFromDESEncryptedTextToAES(ResetCipher resetCipher) throws IOException, CryptoException {
        resetCipher.setupAESCipherFile();
        resetCipher.setupDESCipherFile();

        assertThat(DesToAesMigrator.desToAES("mvcX9yrQsM4iPgm1tDxN1A==")).startsWith("AES:");
    }
}
