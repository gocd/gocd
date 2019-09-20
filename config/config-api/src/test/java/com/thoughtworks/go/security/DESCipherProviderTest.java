/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@EnableRuleMigrationSupport
class DESCipherProviderTest {

    @Rule
    public final ResetCipher resetCipher = new ResetCipher();

    @Test
    void shouldLoadCipherIfPresent() throws Exception {
        resetCipher.setupDESCipherFile();
        DESCipherProvider desCipherProvider = new DESCipherProvider(new SystemEnvironment());
        byte[] key = desCipherProvider.getKey();
        assertThat(key).containsExactly(decodeHex("269298bc31c44620"));
    }

    @Test
    void shouldBailIfCipherNotPresent() {
        DESCipherProvider desCipherProvider = new DESCipherProvider(new SystemEnvironment());
        assertThatCode(() -> {
            desCipherProvider.getKey();
        }).isInstanceOf(IllegalStateException.class).hasMessage("You seem to be loading a cipher from a file that does not exist.");
    }
}
