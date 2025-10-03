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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsoleResult implements SecretRedactor {
    private final int returnValue;
    private final List<String> output;
    private final List<String> error;
    private final List<CommandArgument> arguments;
    private final List<SecretRedactor> secrets;
    private final boolean failOnNonZeroReturn;

    public ConsoleResult(int returnValue, List<String> output, List<String> error, List<CommandArgument> arguments, List<SecretRedactor> secrets) {
        this(returnValue, output, error, arguments, secrets, true);
    }

    public ConsoleResult(int returnValue, List<String> output, List<String> error, List<CommandArgument> arguments, List<SecretRedactor> secrets, boolean failOnNonZeroReturn) {
        this.returnValue = returnValue;
        this.output = output;
        this.error = new ArrayList<>(error);
        this.arguments = arguments;
        this.secrets = secrets;
        this.failOnNonZeroReturn = failOnNonZeroReturn;
    }

    public List<String> output() {
        return output;
    }

    public List<String> error() {
        return error;
    }

    @NotNull
    @Override
    public SecretRedactor.Redactable redactFrom(@NotNull Redactable toRedact) {
        if (toRedact.isBlank()) {
            return toRedact;
        }
        return SecretRedactor.redactRepeatably(toRedact, arguments, secrets);
    }

    public List<String> outputForDisplay() {
        return forDisplay(output);
    }

    public int returnValue() {
        return returnValue;
    }

    public String outputAsString() {
        return String.join("\n", output());
    }

    public String outputForDisplayAsString() {
        return String.join("\n", outputForDisplay());
    }

    public String errorAsString() {
        return String.join("\n", error());
    }

    public String errorForDisplayAsString() {
        return String.join("\n", forDisplay(error));
    }

    private List<String> forDisplay(List<String> from) {
        return from.stream().map(this::redactFrom).toList();
    }

    public boolean failed() {
        // Some git commands return non-zero return value for a "successful" command (e.g. git config --get-regexp)
        // In such a scenario, we can't simply rely on return value to tell whether a command is successful or not
        return failOnNonZeroReturn && returnValue() != 0;
    }

    public String describe() {
        return
                "--- EXIT CODE (" + returnValue() + ") ---\n"
                + "--- STANDARD OUT ---\n" + outputForDisplayAsString() + "\n"
                + "--- STANDARD ERR ---\n" + errorForDisplayAsString() + "\n"
                + "---\n";
    }

    public static ConsoleResult unknownResult() {
        return new ConsoleResult(-1, List.of(), List.of("Unknown result."), List.of(), List.of());
    }

    public RuntimeException redactFrom(RuntimeException rawException) {
        return (RuntimeException) redactFrom((Exception) rawException);
    }

    public Exception redactFrom(Exception rawException) {
        Redactable message = redactFromRepeatably(rawException.toString());
        Optional<Redactable> causeMessage = Optional.ofNullable(rawException.getCause()).map(t -> redactFromRepeatably(t.toString()));

        if (causeMessage.map(Redactable::wasRedacted).orElse(false)) {
            return new RedactedException(message, causeMessage.get(), rawException);
        } else if (message.wasRedacted()) {
            return new RedactedException(message, rawException);
        } else {
            return rawException;
        }
    }

    static class RedactedException extends RuntimeException {
        public RedactedException(Redactable message, Throwable copyMe) {
            super(String.format("Redacted: %s", message.toString()), copyMe.getCause());
            setStackTrace(copyMe.getStackTrace());
        }

        public RedactedException(Redactable message, Redactable causeMessage, Throwable copyMe) {
            super(String.format("Redacted: %s", message.toString()), new RedactedException(causeMessage, copyMe.getCause()));
            setStackTrace(copyMe.getStackTrace());
        }
    }
}
