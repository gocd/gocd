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
package com.thoughtworks.go.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;

public class CachedDigestUtils {

    private static final Cache<String, String> sha256DigestCache = Caffeine
        .newBuilder()
        .maximumSize(1024)
        .build();
    private static final Cache<String, String> sha512DigestCache = Caffeine
        .newBuilder()
        .maximumSize(1024)
        .build();

    @SneakyThrows // compute() doesn't throw checked exceptions
    public static String sha512_256Hex(String data) {
        return sha512DigestCache.get(data, DigestUtils::sha512_256Hex);
    }

    @SneakyThrows // compute() doesn't throw checked exceptions
    public static String sha256Hex(String data) {
        return sha256DigestCache.get(data, DigestUtils::sha256Hex);
    }
}
