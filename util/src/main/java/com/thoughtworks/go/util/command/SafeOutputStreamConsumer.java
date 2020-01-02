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

import java.util.ArrayList;
import java.util.List;

public class SafeOutputStreamConsumer implements ConsoleOutputStreamConsumer {
    private List<CommandArgument> arguments = new ArrayList<>();
    private List<SecretString> secrets = new ArrayList<>();
    private final ConsoleOutputStreamConsumer consumer;

    public SafeOutputStreamConsumer(ConsoleOutputStreamConsumer consumer) {
        this.consumer = consumer;
    }

    public void addArgument(CommandArgument argument) {
        arguments.add(argument);
    }

    @Override
    public void taggedStdOutput(String tag, String line) {
        consumer.taggedStdOutput(tag, replaceSecretInfo(line));
    }

    @Override
    public void taggedErrOutput(String tag, String line) {
        consumer.taggedErrOutput(tag, replaceSecretInfo(line));
    }

    @Override
    public void stdOutput(String line) {
        consumer.stdOutput(replaceSecretInfo(line));
    }

    @Override
    public void errOutput(String line) {
        consumer.errOutput(replaceSecretInfo(line));
    }

    private String replaceSecretInfo(String line) {
        for (CommandArgument argument : arguments) {
            line = argument.replaceSecretInfo(line);
        }
        for (SecretString secret : secrets) {
            line = secret.replaceSecretInfo(line);
        }
        return line;
    }

    public void addArguments(List<CommandArgument> arguments) {
        this.arguments.addAll(arguments);
    }

    public void addSecrets(List<SecretString> secrets) {
        this.secrets.addAll(secrets);
    }

    public void addSecret(SecretString secret) {
        this.secrets.add(secret);
    }
}
