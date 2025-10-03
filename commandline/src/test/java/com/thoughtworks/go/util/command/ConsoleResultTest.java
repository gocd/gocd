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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleResultTest {
    private final List<CommandArgument> args = List.of(new StringArgument("foo"), new PasswordArgument("bar"));
    private final List<SecretRedactor> secrets = List.of(new PasswordArgument("quux"));
    private final ConsoleResult result = new ConsoleResult(0, List.of(" foo ", " bar ", " baz ", " abc "), List.of(" quux ", " bang "), args, secrets);

    @Test
    void shouldRedactFromExceptionNoCause() {
        Exception topException = new RuntimeException("foo bar abc");
        Exception redacted = result.redactFrom(topException);

        assertThat(redacted.getMessage()).isEqualTo("Redacted: java.lang.RuntimeException: foo ****** abc");
        assertThat(redacted.getStackTrace()).isEqualTo(topException.getStackTrace());

        assertThat(redacted.getCause()).isEqualTo(null);
    }

    @Test
    void shouldRedactFromExceptionNoCauseIfNoOp() {
        Exception topException = new RuntimeException("nothing secret");
        Exception redacted = result.redactFrom(topException);
        assertThat(redacted).isEqualTo(topException);
    }

    @Test
    void shouldRedactFromExceptionAndCauseBothNoOp() {
        Exception innerException = new Exception("nothing secret");
        Exception topException = new RuntimeException("nothing secret", innerException);
        Exception redacted = result.redactFrom(topException);
        assertThat(redacted).isEqualTo(topException);
    }

    @Test
    void shouldRedactFromExceptionAndCauseTopOnly() {
        Exception innerException = new Exception("nothing secret");
        Exception topException = new RuntimeException("foo bar abc", innerException);
        Exception redacted = result.redactFrom(topException);

        assertThat(redacted.getMessage()).isEqualTo("Redacted: java.lang.RuntimeException: foo ****** abc");
        assertThat(redacted.getStackTrace()).isEqualTo(topException.getStackTrace());
        assertThat(redacted.getCause()).isEqualTo(innerException);
    }

    @Test
    void shouldRedactFromExceptionCauseOnly() {
        Exception innerException = new Exception("baz quux baz");
        Exception topException = new RuntimeException("nothing secret", innerException);
        Exception redacted = result.redactFrom(topException);

        assertThat(redacted.getMessage()).isEqualTo("Redacted: java.lang.RuntimeException: nothing secret");
        assertThat(redacted.getStackTrace()).isEqualTo(topException.getStackTrace());

        assertThat(redacted.getCause().getMessage()).isEqualTo("Redacted: java.lang.Exception: baz ****** baz");
        assertThat(redacted.getCause().getStackTrace()).isEqualTo(innerException.getStackTrace());
    }

    @Test
    void shouldRedactFromExceptionAndCause() {
        Exception innerException = new Exception("baz quux baz");
        Exception topException = new RuntimeException("foo bar abc", innerException);
        Exception redacted = result.redactFrom(topException);

        assertThat(redacted.getMessage()).isEqualTo("Redacted: java.lang.RuntimeException: foo ****** abc");
        assertThat(redacted.getStackTrace()).isEqualTo(topException.getStackTrace());

        assertThat(redacted.getCause().getMessage()).isEqualTo("Redacted: java.lang.Exception: baz ****** baz");
        assertThat(redacted.getCause().getStackTrace()).isEqualTo(innerException.getStackTrace());
    }

    @Test
    void shouldRedactFromShouldNotFailForNull() {
        List<CommandArgument> commands = new ArrayList<>();
        commands.add(new PasswordArgument("foo"));
        List<SecretRedactor> secretStrings = new ArrayList<>();
        secretStrings.add(new PasswordArgument("foo"));
        ConsoleResult result = new ConsoleResult(10, new ArrayList<>(), new ArrayList<>(), commands, secretStrings);

        String strRedacted = result.redactFrom((String) null);
        assertThat(strRedacted).isEqualTo(null);

        SecretRedactor.Redactable redacted = result.redactFromRepeatably(null);
        assertThat(redacted).isEqualTo(SecretRedactor.Redactable.of(null));
        assertThat(redacted.value()).isNull();
    }

    @Test
    void shouldDescribeResult() {
        List<CommandArgument> args = List.of(new StringArgument("foo"), new PasswordArgument("bar"));
        List<SecretRedactor> secrets = List.of(new PasswordArgument("quux"));
        ConsoleResult result = new ConsoleResult(42, List.of(" foo ", " bar ", " baz ", " abc "), List.of(" quux ", " bang "), args, secrets);
        assertThat(result.describe()).contains("--- EXIT CODE (42) ---");
        assertThat(result.describe()).contains("--- STANDARD OUT ---");
        assertThat(result.describe()).contains("--- STANDARD ERR ---");
    }
}
