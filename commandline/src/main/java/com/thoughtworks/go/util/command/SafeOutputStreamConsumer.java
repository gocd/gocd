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

import java.util.ArrayList;
import java.util.List;

public class SafeOutputStreamConsumer implements ConsoleOutputStreamConsumer {
    private final List<CommandArgument> arguments = new ArrayList<>();
    private final List<SecretRedactor> secrets = new ArrayList<>();
    private final ConsoleOutputStreamConsumer consumer;

    public SafeOutputStreamConsumer(ConsoleOutputStreamConsumer consumer) {
        this.consumer = consumer;
    }

    public void addArgument(CommandArgument argument) {
        arguments.add(argument);
    }

    @Override
    public void taggedStdOutput(String tag, String line) {
        consumer.taggedStdOutput(tag, redactSecretsFrom(line));
    }

    @Override
    public void taggedErrOutput(String tag, String line) {
        consumer.taggedErrOutput(tag, redactSecretsFrom(line));
    }

    @Override
    public void stdOutput(String line) {
        consumer.stdOutput(redactSecretsFrom(line));
    }

    @Override
    public void errOutput(String line) {
        consumer.errOutput(redactSecretsFrom(line));
    }

    private String redactSecretsFrom(String line) {
        return SecretRedactor.redact(line, arguments, secrets);
    }

    public void addArguments(List<CommandArgument> arguments) {
        this.arguments.addAll(arguments);
    }

    public void addSecrets(List<SecretRedactor> secrets) {
        this.secrets.addAll(secrets);
    }
}
