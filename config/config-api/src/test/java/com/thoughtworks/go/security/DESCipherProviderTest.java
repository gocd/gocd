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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.DESKeySpec;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DESCipherProviderTest {

    @BeforeEach
    public void setup() throws IOException {
        new DESCipherProvider(new SystemEnvironment()).resetCipher();
    }

    @AfterEach
    public void tearDown() {
        new DESCipherProvider(new SystemEnvironment()).resetCipher();
    }

    @Test
    public void shouldGenerateAValidAndSafeDESKey() throws Exception {
        DESCipherProvider desCipherProvider = new DESCipherProvider(new SystemEnvironment());
        byte[] key = desCipherProvider.getKey();
        assertThat(DESKeySpec.isWeak(key, 0), is(false));
    }

}
