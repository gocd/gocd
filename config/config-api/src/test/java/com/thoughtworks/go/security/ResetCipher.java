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

import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ResetCipher implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
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
    public void beforeEach(ExtensionContext context) throws Exception {
        removeCachedKey();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        removeCachedKey();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == ResetCipher.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return this;
    }

    private void removeCachedKey() {
        aesCipherProvider.removeCachedKey();
        desCipherProvider.removeCachedKey();
    }

    public void setupAESCipherFile() throws IOException {
        setupAESCipherFile("fdf500c4ec6e51172477145db6be63c5");
    }

    public void setupAESCipherFile(String cipher) throws IOException {
        ReflectionUtil.setField(aesCipherProvider, "cachedKey", null);
        FileUtils.writeStringToFile(systemEnvironment.getAESCipherFile(), cipher, UTF_8);
    }

    public void setupDESCipherFile() throws IOException {
        setupDESCipherFile("269298bc31c44620");
    }

    public void setupDESCipherFile(String cipher) throws IOException {
        ReflectionUtil.setField(desCipherProvider, "cachedKey", null);
        FileUtils.writeStringToFile(systemEnvironment.getDESCipherFile(), cipher, UTF_8);
    }
}
