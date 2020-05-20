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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.util.MaterialFingerprintTag;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.ConsoleResult;

/**
 * @understands: SCMCommand
 */
public abstract class SCMCommand {
    protected String materialFingerprint;

    public SCMCommand(String materialFingerprint) {
        this.materialFingerprint = materialFingerprint;
    }

    public ConsoleResult runOrBomb(CommandLine commandLine, boolean failOnNonZeroReturn, String... input) {
        return commandLine.runOrBomb(failOnNonZeroReturn, new MaterialFingerprintTag(materialFingerprint), input);
    }

    public ConsoleResult runOrBomb(CommandLine commandLine, String... input) {
        return commandLine.runOrBomb(new MaterialFingerprintTag(materialFingerprint), input);
    }

    protected int run(CommandLine commandLine, ConsoleOutputStreamConsumer outputStreamConsumer, String... input) {
        return commandLine.run(outputStreamConsumer, new MaterialFingerprintTag(materialFingerprint), input);
    }
}
