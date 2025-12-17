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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecretRedactorTest {

    @Test
    public void shouldUseNewRedactableAndConvertWhenNecessary() {
        SecretRedactor redacting = line -> line.next("xxx");
        assertThat(redacting.redactFrom("")).isEqualTo("xxx");
    }

    @Test
    public void shouldUseNewRedactableWhenNecessary() {
        SecretRedactor redacting = line -> line.next("xxx");
        assertThat(redacting.redactFromRepeatably("").value()).isEqualTo("xxx");
    }

    @Test
    public void shouldNotHaveNullToString() {
        SecretRedactor.Redactable redactable = SecretRedactor.Redactable.of(null);
        assertThat(redactable.wasRedacted()).isFalse();
        assertThat(redactable.value()).isEqualTo(null);
        assertThat(redactable.toString()).isEmpty();
    }

    @Test
    public void shouldRetainOriginalWhenNoSecret() {
        SecretRedactor.Redactable redactable = SecretRedactor.Redactable.of("hello world");
        assertThat(redactable.value()).isEqualTo("hello world");
        assertThat(redactable.next("hello world")).isSameAs(redactable);
    }

    @Test
    public void shouldSetFlagWhenAltered() {
        SecretRedactor.Redactable redactable = SecretRedactor.Redactable.of("hello world");
        assertThat(redactable.wasRedacted()).isFalse();
        assertThat(SecretRedactor.Redactable.of("hello world").next("hello world2")).satisfies(r -> {
            assertThat(r.wasRedacted()).isTrue();
            assertThat(r.value()).isEqualTo("hello world2");
        });
    }

    @Test
    public void shouldRedactFromLists() {
        assertThat(SecretRedactor.redact("hello world",
            List.of(r -> r.next(r.value() + " a1"), r -> r.next(r.value() + " a2")),
            List.of(r -> r.next(r.value() + " b1"), r -> r.next(r.value() + " b2"))
        )).isEqualTo("hello world a1 a2 b1 b2");

        assertThat(SecretRedactor.redactRepeatably(SecretRedactor.Redactable.of("hello world"),
            List.of(r -> r.next(r.value() + " a1")),
            List.of(r -> r.next(r.value() + " b1"))
        )).satisfies(r -> {
            assertThat(r.wasRedacted()).isTrue();
            assertThat(r.value()).isEqualTo("hello world a1 b1");
        });

        assertThat(SecretRedactor.redactRepeatably(SecretRedactor.Redactable.of("hello world"),
            List.of(r -> r.next("hello world"), r -> r.next("hello world")),
            List.of(r -> r.next("hello world"), r -> r.next("hello world"))
        )).satisfies(r -> {
            assertThat(r.wasRedacted()).isFalse();
            assertThat(r.value()).isEqualTo("hello world");
        });
    }

}