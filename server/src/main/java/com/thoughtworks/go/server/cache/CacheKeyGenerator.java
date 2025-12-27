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
package com.thoughtworks.go.server.cache;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CacheKeyGenerator {
    private static final String DELIMITER = ".$";
    private final Class<?> clazz;

    public CacheKeyGenerator(Class<?> clazz) {
        this.clazz = clazz;
    }

    public @NotNull String generate(@NotNull String identifier, long arg) {
        return String.join(DELIMITER, clazz.getName(), identifier, Long.toString(arg)).intern();
    }

    public @NotNull String generate(@NotNull String identifier, String arg) {
        return String.join(DELIMITER, clazz.getName(), identifier, toStringSafe(arg)).intern();
    }

    public @NotNull String generate(@NotNull String identifier, Object... args) {
        return Stream.concat(
                Stream.of(clazz.getName(), identifier),
                Arrays.stream(args).map(CacheKeyGenerator::validateArg).map(CacheKeyGenerator::toStringSafe))
            .collect(Collectors.joining(DELIMITER))
            .intern();
    }

    private static String toStringSafe(String arg) {
        return arg == null ? "" : arg;
    }

    private static String toStringSafe(Object arg) {
        if (arg instanceof CaseInsensitiveString) {
            return ((CaseInsensitiveString) arg).toLower();
        } else {
            return arg == null ? "" : arg.toString();
        }
    }

    private static Object validateArg(Object arg) {
        if (isAllowed(arg)) {
            return arg;
        }
        throw new IllegalArgumentException("Type " + arg.getClass() + " is not allowed here!");
    }

    private static boolean isAllowed(Object arg) {
        return arg == null || arg instanceof String || arg instanceof CaseInsensitiveString ||
                arg instanceof Number || arg instanceof Boolean || arg instanceof Enum;
    }
}
