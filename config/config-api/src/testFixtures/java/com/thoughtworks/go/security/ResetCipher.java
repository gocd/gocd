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
import org.junit.jupiter.api.extension.*;

import java.io.IOException;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("deprecation")
public class ResetCipher implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    public static final String AES_CIPHER_HEX = "fdf500c4ec6e51172477145db6be63c5";
    public static final String DES_CIPHER_HEX = "269298bc31c44620";
    private final SystemEnvironment systemEnvironment;
    private final AESCipherProvider aesCipherProvider;
    private final DESCipherProvider desCipherProvider;

    public ResetCipher() {
        this(new SystemEnvironment());
    }

    public ResetCipher(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        aesCipherProvider = new AESCipherProvider(systemEnvironment);
        desCipherProvider = new DESCipherProvider(systemEnvironment);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        removeCipher();
    }

    @Override
    public void afterEach(ExtensionContext context) throws IOException {
        removeCipher();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == ResetCipher.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return this;
    }

    private void removeCipher() throws IOException {
        aesCipherProvider.removeCipher();
        desCipherProvider.removeCipher();
    }

    public void setupAESCipherFile() throws IOException {
        setupAESCipherFile(AES_CIPHER_HEX);
    }

    public void setupAESCipherFile(String cipher) throws IOException {
        AESCipherProvider.removeCachedKey();
        Files.writeString(systemEnvironment.getAESCipherFile().toPath(), cipher, UTF_8);
    }

    public void setupDESCipherFile() throws IOException {
        setupDESCipherFile(DES_CIPHER_HEX);
    }

    public void setupDESCipherFile(String cipher) throws IOException {
        DESCipherProvider.removeCachedKey();
        Files.writeString(systemEnvironment.getDESCipherFile().toPath(), cipher, UTF_8);
    }
}
