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
package com.thoughtworks.go.server.cache;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StageState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheKeyGeneratorTest {
    private CacheKeyGenerator cacheKeyGenerator;

    @BeforeEach
    void setUp() {
        cacheKeyGenerator = new CacheKeyGenerator(Pipeline.class);
    }

    @Test
    void shouldGenerateCacheKey() {
        final String generatedCacheKey = cacheKeyGenerator.generate("foo", "bar", 1, 1L);
        assertThat(generatedCacheKey).isEqualTo("com.thoughtworks.go.domain.Pipeline.$foo.$bar.$1.$1");
    }

    @Test
    void shouldConsiderNullAsEmptyStringAndGenerateCacheKey() {
        final String generatedCacheKey = cacheKeyGenerator.generate("foo", "bar", null, 1L);
        assertThat(generatedCacheKey).isEqualTo("com.thoughtworks.go.domain.Pipeline.$foo.$bar.$.$1");
    }

    @Test
    void shouldAlwaysReturnInternedString() {
        final String generatedCacheKey = cacheKeyGenerator.generate("foo", "bar", new CaseInsensitiveString("1"), 1L);
        assertThat(generatedCacheKey == "com.thoughtworks.go.domain.Pipeline.$foo.$bar.$1.$1")
                .describedAs("Using '==' to check returned key is interned String").isTrue();
    }

    @Test
    void shouldAllowBooleanInArgs() {
        final String generatedCacheKey = cacheKeyGenerator.generate("foo", "bar", true, false);
        assertThat(generatedCacheKey).isEqualTo("com.thoughtworks.go.domain.Pipeline.$foo.$bar.$true.$false");
    }

    @Test
    void shouldAllowEnumInArgs() {
        final String generatedCacheKey = cacheKeyGenerator.generate("foo", "bar", StageState.Passed);
        assertThat(generatedCacheKey).isEqualTo("com.thoughtworks.go.domain.Pipeline.$foo.$bar.$Passed");
    }

    @Test
    void shouldErrorOutWhenObjectOfUnsupportedTypeIsGivenToGenerateCacheKey() {
        assertThatThrownBy(() -> cacheKeyGenerator.generate("foo", "bar", new Object(), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Type class java.lang.Object is not allowed here!");
    }

    @Test
    void shouldConvertCaseInsensitiveStringToLowerCase() {
        final String generatedCacheKey = cacheKeyGenerator.generate("Foo", "bAR", new CaseInsensitiveString("FAST"), 1L);
        assertThat(generatedCacheKey).isEqualTo("com.thoughtworks.go.domain.Pipeline.$Foo.$bAR.$fast.$1");
    }
}
