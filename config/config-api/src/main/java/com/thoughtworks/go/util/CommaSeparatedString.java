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

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class CommaSeparatedString {
    private static final String COMMA = ",";

    public static @Nullable String normalizeToNull(String commaSeparatedStr) {
        return joinToNull(commaSeparatedStrToTrimmed(commaSeparatedStr).distinct().sorted());
    }

    public static @Nullable String append(@Nullable String commaSeparatedStr, @Nullable List<String> entriesToAdd) {
        if (entriesToAdd == null || entriesToAdd.isEmpty()) {
            return commaSeparatedStr;
        }

        return joinToNull(Stream.concat(commaSeparatedStrToTrimmed(commaSeparatedStr), trimmedWithoutBlanks(entriesToAdd.stream())));
    }

    public static @Nullable String remove(@Nullable String commaSeparatedStr, @Nullable List<String> entriesToRemove) {
        if (entriesToRemove == null || entriesToRemove.isEmpty() || commaSeparatedStr == null || commaSeparatedStr.isBlank()) {
            return commaSeparatedStr;
        }

        Set<String> finalEntriesToRemove = distinctNonBlankEntries(entriesToRemove);
        return joinToNull(
            commaSeparatedStrToTrimmed(commaSeparatedStr)
                .filter(s -> !finalEntriesToRemove.contains(s))
        );
    }

    private static Set<String> distinctNonBlankEntries(@NotNull Collection<String> entriesToRemove) {
        return entriesToRemove.isEmpty()
            ? Collections.emptySet()
            : trimmedWithoutBlanks(entriesToRemove.stream()).collect(Collectors.toSet());
    }

    public static List<String> commaSeparatedStrToList(@Nullable String commaSeparatedStr) {
        return commaSeparatedStrToTrimmed(commaSeparatedStr).collect(Collectors.toList());
    }

    public static @NotNull Stream<String> commaSeparatedStrToTrimmed(@Nullable String commaSeparatedStr) {
        return commaSeparatedStr == null || commaSeparatedStr.isBlank()
            ? Stream.empty()
            : trimmedWithoutBlanks(Arrays.stream(commaSeparatedStr.split(COMMA)));
    }

    private static @NotNull Stream<String> trimmedWithoutBlanks(Stream<String> stream) {
        return stream.filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty());
    }

    private static @Nullable String joinToNull(@NotNull Stream<String> stream) {
        String result = stream.distinct().collect(Collectors.joining(COMMA));
        return result.isEmpty() ? null : result;
    }

}
