/*
 * Copyright 2020 ThoughtWorks, Inc.
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

package com.thoughtworks.go.remote;

import com.thoughtworks.go.security.*;
import com.thoughtworks.go.util.SystemEnvironment;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.File;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@SuppressWarnings("deprecation")
class SerializationTest {
    @Test
    void rejectsSerializationOfGoCipher() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().toJson(new GoCipher(mock(Encrypter.class))));
        assertEquals(format("Refusing to serialize a %s instance and leak security details!", GoCipher.class.getName()), e.getMessage());
    }

    @Test
    void rejectsDeserializationOfGoCipher() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{}", GoCipher.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", GoCipher.class.getName()), e.getMessage());
    }

    @Test
    void rejectsSerializationOfAESEncrypter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().toJson(new AESEncrypter(mock(AESCipherProvider.class))));
        assertEquals(format("Refusing to serialize a %s instance and leak security details!", AESEncrypter.class.getName()), e.getMessage());
    }

    @Test
    void rejectsDeserializationOfAESEncrypter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{}", AESEncrypter.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", AESEncrypter.class.getName()), e.getMessage());
    }

    @Test
    void rejectsSerializationOfAESCipherProvider() {
        final AESCipherProvider acp = new AESCipherProvider(new TempSystemEnvironment());
        try {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                    Serialization.instance().toJson(acp));
            assertEquals(format("Refusing to serialize a %s instance and leak security details!", AESCipherProvider.class.getName()), e.getMessage());
        } finally {
            acp.removeCachedKey();
        }
    }

    @Test
    void rejectsDeserializationOfAESCipherProvider() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{}", AESCipherProvider.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", AESCipherProvider.class.getName()), e.getMessage());
    }

    @Test
    void rejectsSerializationOfDESEncrypter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().toJson(new DESEncrypter(mock(DESCipherProvider.class))));
        assertEquals(format("Refusing to serialize a %s instance and leak security details!", DESEncrypter.class.getName()), e.getMessage());
    }

    @Test
    void rejectsDeserializationOfDESEncrypter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{}", DESEncrypter.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", DESEncrypter.class.getName()), e.getMessage());
    }

    @Test
    void rejectsSerializationOfDESCipherProvider() {
        final DESCipherProvider dcp = new DESCipherProvider(new TempSystemEnvironment());
        try {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                    Serialization.instance().toJson(dcp));
            assertEquals(format("Refusing to serialize a %s instance and leak security details!", DESCipherProvider.class.getName()), e.getMessage());
        } finally {
            dcp.removeCachedKey();
        }
    }

    @Test
    void rejectsDeserializationOfDESCipherProvider() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{ \"whatever\": \"actual payload doesn't matter\" }", DESCipherProvider.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", DESCipherProvider.class.getName()), e.getMessage());
    }

    private static class TempSystemEnvironment extends SystemEnvironment {
        @SneakyThrows
        @Override
        public File getAESCipherFile() {
            final File file = File.createTempFile("aes", null);
            file.deleteOnExit();
            return file;
        }

        @SneakyThrows
        @Override
        public File getDESCipherFile() {
            final File file = File.createTempFile("des", null);
            file.deleteOnExit();
            return file;
        }
    }
}
