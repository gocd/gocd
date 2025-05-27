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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.crypto.spec.DESKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
@ExtendWith(ResetCipher.class)
public class DESCipherProviderTest {

    private final SystemEnvironment env = new SystemEnvironment();

    @Test
    public void shouldCacheEmptyKeyIfNotFound() {
        byte[] key = new DESCipherProvider(env).getKey();
        assertThat(key).isEmpty();
        byte[] key2 = new DESCipherProvider(env).getKey();
        assertThat(key2).isEmpty();
        assertThat(key2).isSameAs(key);
    }

    @Test
    public void shouldBeAbleToLoadExistingCipher(ResetCipher resetCipher) throws Exception {
        resetCipher.setupDESCipherFile();
        byte[] key = new DESCipherProvider(env).getKey();
        assertThat(key).hasSize(8).isEqualTo(Hex.decodeHex(ResetCipher.DES_CIPHER_HEX));
        assertThat(DESKeySpec.isWeak(key, 0)).isFalse();

        assertThat(new DESCipherProvider(env).getKey()).isSameAs(key);
    }

}
