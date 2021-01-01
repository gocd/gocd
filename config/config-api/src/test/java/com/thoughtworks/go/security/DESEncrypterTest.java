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

import org.apache.commons.codec.DecoderException;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DESEncrypterTest {

    private DESEncrypter desEncrypter;

    @Before
    public void setUp() throws Exception {
        DESCipherProvider cipherProvider = mock(DESCipherProvider.class);
        when(cipherProvider.getKey()).thenReturn(decodeHex("269298bc31c44620"));
        desEncrypter = new DESEncrypter(cipherProvider);
    }

    @Test
    public void shouldNotEncryptText() {
        assertThatCode(() -> desEncrypter.encrypt(null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Encrypting using DES is no longer supported!");
    }

    @Test
    public void shouldDecryptText() throws CryptoException {
        String plainText = desEncrypter.decrypt("mvcX9yrQsM4iPgm1tDxN1A==");
        assertThat(plainText).isEqualTo("user-password!");
    }

    @Test
    public void canDecryptShouldAnswerTrueIfPasswordLooksLikeItIsNotAES() {
        assertThat(desEncrypter.canDecrypt("AES:foo:bar")).isFalse();

        assertThat(desEncrypter.canDecrypt("foobar")).isTrue();
    }

    @Test
    public void shouldErrorOutWhenCipherTextIsTamperedWith() {
        assertThatCode(() -> desEncrypter.decrypt("some bad junk"))
                .hasMessageContaining("Illegal base64 character 20")
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .isInstanceOf(CryptoException.class);
    }

    @Test
    public void shouldReEncryptUsingNewKey() throws DecoderException, CryptoException {
        String originalCipherText = "mvcX9yrQsM4iPgm1tDxN1A==";
        String newCipherText = DESEncrypter.reEncryptUsingNewKey(decodeHex("269298bc31c44620"), decodeHex("02644c13cb892962"), originalCipherText);

        assertThat(originalCipherText).isNotEqualTo(newCipherText);

        DESCipherProvider newCipher = mock(DESCipherProvider.class);
        when(newCipher.getKey()).thenReturn(decodeHex("02644c13cb892962"));
        DESEncrypter newEncrypter = new DESEncrypter(newCipher);
        assertThat(newEncrypter.decrypt(newCipherText)).isEqualTo("user-password!");
    }
}
