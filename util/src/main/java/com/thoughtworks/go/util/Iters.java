/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import java.util.Collection;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

public class Iters {
    private Iters() {
    }

    @SafeVarargs
    public static Set<String> cat(Set<String>... sets) {
        return stream(sets).flatMap(Collection::stream).collect(toSet());
    }

    public static <T> T first(Collection<T> vals) {
        return vals.stream().findFirst().orElseThrow();
    }

    public static String sortJoin(Collection<String> values, String delim) {
        return values.stream().sorted().collect(joining(delim));
    }
}
