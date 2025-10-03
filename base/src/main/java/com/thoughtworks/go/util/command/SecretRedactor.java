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
package com.thoughtworks.go.util.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public interface SecretRedactor {

    @NotNull
    Redactable redactFrom(@NotNull Redactable toRedact);

    default String redactFrom(String toRedact) {
        return redactFromRepeatably(toRedact).value;
    }

    default Redactable redactFromRepeatably(String toRedact) {
        return redactFrom(Redactable.of(toRedact));
    }

    record Redactable(String value, boolean wasRedacted) {
        @Override
        public @NotNull String toString() {
            return value == null ? "" : value;
        }

        public Redactable next(String toRedact) {
            return this.value.equals(toRedact) ? this : new Redactable(toRedact, true);
        }

        public boolean isBlank() {
            return value == null || value.isBlank();
        }

        public static Redactable of(String toRedact) {
            return new Redactable(toRedact, false);
        }
    }

    static String redact(String toRedact, List<? extends SecretRedactor> a, List<? extends SecretRedactor> b) {
        return redactRepeatably(Redactable.of(toRedact), a, b).value();
    }

    static Redactable redactRepeatably(Redactable toRedact, List<? extends SecretRedactor> a, List<? extends SecretRedactor> b) {
        return Stream.concat(a.stream(), b.stream())
            .reduce(toRedact, (v, s) -> s.redactFrom(v), (s1, s2) -> s1);
    }
}
