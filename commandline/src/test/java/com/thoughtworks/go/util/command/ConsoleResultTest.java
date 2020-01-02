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
package com.thoughtworks.go.util.command;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleResultTest {

    @Test
    void shouldSmudgeExceptionMessagesForNestedExceptions() {
        List<CommandArgument> args = Arrays.asList(new StringArgument("foo"), new PasswordArgument("bar"));
        List<SecretString> secrets = Arrays.asList((SecretString) new PasswordArgument("quux"));
        ConsoleResult result = new ConsoleResult(0, Arrays.asList(" foo ", " bar ", " baz ", " abc "), Arrays.asList(" quux ", " bang "), args, secrets);
        Exception innerException = new Exception("baz quux baz");
        Exception topException = new RuntimeException("foo bar abc", innerException);
        Exception exception = result.smudgedException(topException);

        assertThat(exception.getMessage()).isEqualTo("foo ****** abc");
        assertThat(exception).isSameAs(topException);

        assertThat(exception.getCause().getMessage()).isEqualTo("baz ****** baz");
        assertThat(exception.getCause()).isSameAs(innerException);
    }

    @Test
    void shouldReplaceSecretInfoShouldNotFailForNull() {
        ArrayList<CommandArgument> commands = new ArrayList<>();
        commands.add(new PasswordArgument("foo"));
        ArrayList<SecretString> secretStrings = new ArrayList<>();
        secretStrings.add(new PasswordArgument("foo"));
        ConsoleResult result = new ConsoleResult(10, new ArrayList<>(), new ArrayList<>(), commands, secretStrings);
        assertThat(result.replaceSecretInfo(null)).isNull();
    }

    @Test
    void shouldDescribeResult() {
        List<CommandArgument> args = Arrays.asList(new StringArgument("foo"), new PasswordArgument("bar"));
        List<SecretString> secrets = Arrays.asList((SecretString) new PasswordArgument("quux"));
        ConsoleResult result = new ConsoleResult(42, Arrays.asList(" foo ", " bar ", " baz ", " abc "), Arrays.asList(" quux ", " bang "), args, secrets);
        assertThat(result.describe()).contains("--- EXIT CODE (42) ---");
        assertThat(result.describe()).contains("--- STANDARD OUT ---");
        assertThat(result.describe()).contains("--- STANDARD ERR ---");
    }
}
