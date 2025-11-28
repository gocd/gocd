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
package com.thoughtworks.go.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigErrors extends HashMap<String, List<String>> implements Serializable {

    public boolean present() {
        return !isEmpty();
    }

    public void add(String fieldName, String msg) {
        List<String> msgList = computeIfAbsent(fieldName, k -> new ArrayList<>());
        if (!msgList.contains(msg)) {
            msgList.add(msg);
        }
    }

    public @NotNull List<String> getAll() {
        return streamAll().collect(Collectors.toList());
    }

    private @NotNull Stream<String> streamAll() {
        return values().stream().flatMap(Collection::stream).filter(Objects::nonNull);
    }

    public @NotNull List<String> getAllOn(String fieldName) {
        return getOrDefault(fieldName, new ArrayList<>());
    }

    public @Nullable String firstErrorOn(String fieldName) {
        return getOrDefault(fieldName, Collections.emptyList()).stream().findFirst().orElse(null);
    }

    public @Nullable String firstError() {
        return values().stream().findFirst().flatMap(errors -> errors.stream().findFirst()).orElse(null);
    }

    public void addAll(ConfigErrors configErrors) {
        configErrors.forEach((fieldName, errors) -> errors.forEach(error -> add(fieldName, error)));
    }

    public String asString() {
        return streamAll().distinct().collect(Collectors.joining(", "));
    }
}
