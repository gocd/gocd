/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GoCipherTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    private File desCipherFile;
    private File aesCipherFile;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = spy(new SystemEnvironment());
        aesCipherFile = systemEnvironment.getAESCipherFile();
        desCipherFile = systemEnvironment.getDESCipherFile();

        clearCachedKeys();
    }

    private void clearCachedKeys() {
        ReflectionUtil.setField(new DESCipherProvider(new SystemEnvironment()), "cachedKey", null);
        ReflectionUtil.setField(new AESCipherProvider(new SystemEnvironment()), "cachedKey", null);
        FileUtils.deleteQuietly(new SystemEnvironment().getDESCipherFile());
        FileUtils.deleteQuietly(new SystemEnvironment().getAESCipherFile());
    }

    private void setupAESCipherFile() throws IOException {
        ReflectionUtil.setField(new AESCipherProvider(systemEnvironment), "cachedKey", null);
        FileUtils.writeStringToFile(aesCipherFile, "fdf500c4ec6e51172477145db6be63c5", UTF_8);
    }

    private void setupDESCipherFile() throws IOException {
        ReflectionUtil.setField(new DESCipherProvider(systemEnvironment), "cachedKey", null);
        FileUtils.writeStringToFile(desCipherFile, "269298bc31c44620", UTF_8);
    }

    @After
    public void tearDown() {
        clearCachedKeys();
    }

    @Test
    public void shouldCreateAnAESCipherFileWithTheCipherIfNotFound() throws IOException, CryptoException {
        assertThat(desCipherFile.exists()).isFalse();
        assertThat(aesCipherFile.exists()).isFalse();

        GoCipher goCipher = new GoCipher(systemEnvironment);
        assertThat(desCipherFile.exists()).isFalse();
        assertThat(aesCipherFile.exists()).isTrue();

        String plainText = goCipher.decrypt(goCipher.encrypt("user-password!"));
        assertThat(plainText).isEqualTo("user-password!");
        assertThat(desCipherFile.exists()).isFalse();
        assertThat(aesCipherFile.exists()).isTrue();
    }

    @Test
    public void shouldWorkEvenAfterCipherFileHasBeenDeleted() throws CryptoException, IOException {//serialization friendliness
        setupAESCipherFile();
        setupDESCipherFile();

        GoCipher goCipher = new GoCipher(systemEnvironment);

        FileUtils.deleteQuietly(desCipherFile);
        FileUtils.deleteQuietly(aesCipherFile);

        assertThat(desCipherFile.exists()).isFalse();
        assertThat(aesCipherFile.exists()).isFalse();

        String plainText = goCipher.decrypt(goCipher.encrypt("user-password!"));
        assertThat(plainText).isEqualTo("user-password!");

        assertThat(desCipherFile.exists()).isFalse();
        assertThat(aesCipherFile.exists()).isFalse();
    }

    @Test
    public void shouldNotEnableDesCipherIfCipherFileMissing() {
        assertThat(desCipherFile).doesNotExist();

        GoCipher goCipher = new GoCipher(systemEnvironment);
        assertThat(goCipher.aesEncrypter).isNotNull();
        assertThat(goCipher.desEncrypter).isNull();
    }

    @Test
    public void shouldNotEnableDesCipherIfCipherFileIsPresentAndDESIsNotEnabled() throws IOException {
        setupDESCipherFile();
        assertThat(desCipherFile).exists();
        when(systemEnvironment.desEnabled()).thenReturn(false);

        GoCipher goCipher = new GoCipher(systemEnvironment);
        assertThat(goCipher.aesEncrypter).isNotNull();
        assertThat(goCipher.desEncrypter).isNull();
    }

    @Test
    public void shouldNotEnableDesCipherIfCipherFileIsAbsentAndDESIsEnabled() {
        assertThat(desCipherFile).doesNotExist();
        when(systemEnvironment.desEnabled()).thenReturn(true);
        GoCipher goCipher = new GoCipher(systemEnvironment);
        assertThat(goCipher.aesEncrypter).isNotNull();
        assertThat(goCipher.desEncrypter).isNull();
    }

    @Test
    public void shouldEnableDesCipherIfCipherFileIsPresentAndDESIsEnabled() throws IOException {
        setupDESCipherFile();
        assertThat(desCipherFile).exists();

        when(systemEnvironment.desEnabled()).thenReturn(true);
        GoCipher goCipher = new GoCipher(systemEnvironment);

        assertThat(goCipher.aesEncrypter).isNotNull();
        assertThat(goCipher.desEncrypter).isNotNull();
    }

    @Test
    public void shouldConvertFromDESEncryptedTextToAES() throws IOException, CryptoException {
        setupAESCipherFile();
        setupDESCipherFile();

        GoCipher goCipher = new GoCipher(systemEnvironment);
        String cipherText = goCipher.desToAES("mvcX9yrQsM4iPgm1tDxN1A==");
        assertThat(cipherText).startsWith("AES:");
    }
}
