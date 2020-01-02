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

public class CommandLineException extends RuntimeException {
    private final ConsoleResult result;

    public CommandLineException(String message) {
        super(message);
        this.result = ConsoleResult.unknownResult();
    }

    public CommandLineException(String message, Exception ex) {
        super(message, ex);
        this.result = ConsoleResult.unknownResult();
    }

    public CommandLineException(CommandLine command, ConsoleResult result) {
        super("Error performing command: " + command.describe() + "\n" + result.describe());
        this.result = result;
    }


    public ConsoleResult getResult() {
        return result;
    }
}