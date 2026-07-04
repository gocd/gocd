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
package com.thoughtworks.go.server.dao;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
class NullableMaps {
    public static Map<String, Object> nullableMapOf(
        @NotNull String k1, @Nullable Object v1
    ) {
        var map = new HashMap<String, Object>(1);
        addIfKeyNotNull(map, k1, v1);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Object> nullableMapOf(
        @NotNull String k1, @Nullable Object v1,
        @Nullable String k2, @Nullable Object v2
    ) {
        var map = new HashMap<String, Object>(2);
        addIfKeyNotNull(map, k1, v1);
        addIfKeyNotNull(map, k2, v2);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Object> nullableMapOf(
        @NotNull String k1, @Nullable Object v1,
        @Nullable String k2, @Nullable Object v2,
        @Nullable String k3, @Nullable Object v3
    ) {
        var map = new HashMap<String, Object>(3);
        addIfKeyNotNull(map, k1, v1);
        addIfKeyNotNull(map, k2, v2);
        addIfKeyNotNull(map, k3, v3);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Object> nullableMapOf(
        @NotNull String k1, @Nullable Object v1,
        @Nullable String k2, @Nullable Object v2,
        @Nullable String k3, @Nullable Object v3,
        @Nullable String k4, @Nullable Object v4
    ) {
        var map = new HashMap<String, Object>(3);
        addIfKeyNotNull(map, k1, v1);
        addIfKeyNotNull(map, k2, v2);
        addIfKeyNotNull(map, k3, v3);
        addIfKeyNotNull(map, k4, v4);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Object> nullableMapOf(
        @NotNull String k1, @Nullable Object v1,
        @Nullable String k2, @Nullable Object v2,
        @Nullable String k3, @Nullable Object v3,
        @Nullable String k4, @Nullable Object v4,
        @Nullable String k5, @Nullable Object v5
    ) {
        var map = new HashMap<String, Object>(3);
        addIfKeyNotNull(map, k1, v1);
        addIfKeyNotNull(map, k2, v2);
        addIfKeyNotNull(map, k3, v3);
        addIfKeyNotNull(map, k4, v4);
        addIfKeyNotNull(map, k5, v5);
        return Collections.unmodifiableMap(map);
    }

    private static void addIfKeyNotNull(@UnknownNullability Map<String, Object> map, @Nullable String key, @Nullable Object value) {
        if (key != null) {
            map.put(key, value);
        }
    }
}
