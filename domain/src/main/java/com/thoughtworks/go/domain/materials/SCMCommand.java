/*
 * Copyright 2023 Thoughtworks, Inc.
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

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.go.util.MaterialFingerprintTag;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.ConsoleResult;

/**
 * @understands: SCMCommand
 */
public abstract class SCMCommand {
    private static final int RETRY_SLEEP = 5000;
    private static final Logger LOG = LoggerFactory.getLogger(SCMCommand.class);
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

    protected int runWithRetries(CommandLine commandLine, ConsoleOutputStreamConsumer outputStreamConsumer, int retries, String... input) {
        int code = 0;
        for (int retryCount = 0; retryCount < retries; retryCount++) {
            code = run(commandLine, outputStreamConsumer, input);
            if (code == 0) {
                break;
            }
            log(outputStreamConsumer, "Run attempt %d of %d failed", retryCount + 1, retries);
            if (retryCount < retries - 1) {
                log(outputStreamConsumer, "Waiting %d seconds before retrying", RETRY_SLEEP / 1000);
                try {
                    Thread.sleep(RETRY_SLEEP);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(format("run interrupted after %d attempts", retries));
                }
            }
        }
        return code;
    }

    /**
     * Conveniently runs commands sequentially on a given console, aborting on the first failure.
     *
     * @param console  collects console output
     * @param commands the set of sequential commands
     * @return the exit status of the last executed command
     */
    protected int runCascade(ConsoleOutputStreamConsumer console, CommandLine... commands) {
        int code = 0;

        // Doing this via streams is awkward; it's hard to *both* collect the return code
        // *AND* exit iteration early. Curse Java for only giving us fake closures!
        //
        // My un-amusement is "effectively final" 😒.
        for (CommandLine cmd : commands) {
            code = run(cmd, console);
            if (code != 0) {
                break;
            }
        }

        return code;
    }

    /**
     * Conveniently runs commands sequentially on a given console, retrying on failure.
     * @param console collects console output
     * @param retries number of times to retry
     * @param commands the set of sequential commands
     * @return the exit status of the last executed command
     */
    protected int runCascadeWithRetries(ConsoleOutputStreamConsumer console, int retries, CommandLine... commands) {
        int code = 0;

        // Doing this via streams is awkward; it's hard to *both* collect the return code
        // *AND* exit iteration early. Curse Java for only giving us fake closures!
        //
        // My un-amusement is "effectively final" 😒.
        for (CommandLine cmd : commands) {
            code = runWithRetries(cmd, console, retries);
            if (code != 0) {
                break;
            }
        }

        return code;
    }

    private void log(ConsoleOutputStreamConsumer outputStreamConsumer, String message, Object... args) {
        LOG.debug(format(message, args));
        outputStreamConsumer.stdOutput(format("[SCMCMD] " + message, args));
    }
}
