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

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductionIVProviderTest {

    @Test
    public void shouldGenerateDifferentCipherTextEveryTime() throws NoSuchAlgorithmException {
        ProductionIVProvider ivProvider = new ProductionIVProvider();
        byte[] iv1 = ivProvider.createIV();
        byte[] iv2 = ivProvider.createIV();

        assertThat(iv1).isNotEqualTo(iv2);
    }

    @Test
    public void shouldGenerateA16ByteIV() throws NoSuchAlgorithmException {
        ProductionIVProvider ivProvider = new ProductionIVProvider();

        byte[] iv = ivProvider.createIV();
        assertThat(iv).hasSize(16);
    }
}
