/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.matchers;

import com.thoughtworks.go.util.GoConstants;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AbstractAssert;

import java.io.File;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class ConsoleOutMatcherJunit5 extends AbstractAssert<ConsoleOutMatcherJunit5, String> {
    private boolean invert = false;

    public ConsoleOutMatcherJunit5(String consoleOut) {
        this(consoleOut, false);
    }

    private ConsoleOutMatcherJunit5(String consoleOut, boolean invert) {
        super(consoleOut, ConsoleOutMatcherJunit5.class);
        this.invert = invert;
    }

    public static ConsoleOutMatcherJunit5 assertConsoleOut(String actual) {
        return new ConsoleOutMatcherJunit5(actual);
    }

    public ConsoleOutMatcherJunit5 not() {
        return new ConsoleOutMatcherJunit5(actual, true);
    }

    public ConsoleOutMatcherJunit5 printedEnvVariable(final String key, final Object value) {
        String set = format("environment variable '%s' to value '%s'", key, value);
        String override = format("environment variable '%s' with value '%s'", key, value);
        failIf(!StringUtils.contains(actual, set) && !StringUtils.contains(actual, override), "Expected console to contain [<%s>] or [<%s>]  but was <%s>.", set, override, actual);
        return this;
    }

    public ConsoleOutMatcherJunit5 printedPreparingInfo(final Object jobIdentifier) {
        return expectConsoleToContain(format("Start to prepare %s", jobIdentifier.toString()));
    }

    public ConsoleOutMatcherJunit5 printedBuildingInfo(final Object jobIdentifier) {
        return expectConsoleToContain(format("Start to build %s", jobIdentifier.toString()));
    }

    public ConsoleOutMatcherJunit5 printedUploadingInfo(final Object jobIdentifier) {
        return expectConsoleToContain(format("Start to upload %s", jobIdentifier.toString()));
    }

    public ConsoleOutMatcherJunit5 printedJobCanceledInfo(final Object jobIdentifier) {
        return expectConsoleToContain(format("Job is canceled %s", jobIdentifier.toString()));
    }

    public ConsoleOutMatcherJunit5 printedJobCompletedInfo(final Object jobIdentifier) {
        return expectConsoleToContain(format("Job completed %s", jobIdentifier.toString()));
    }

    public ConsoleOutMatcherJunit5 printedBuildFailed() {
        final String buildFailed = "build failed";
        return failIf(!actual.toLowerCase().contains(buildFailed), "Expected console to contain [<%s>] but was <%s>.", buildFailed, actual);
    }

    public ConsoleOutMatcherJunit5 printedUploadingFailure(final File file) {
        return expectConsoleToContain(String.format("Failed to upload %s", file.getAbsolutePath()));
    }

    public ConsoleOutMatcherJunit5 printedRuleDoesNotMatchFailure(final String root, final String rule) {
        return expectConsoleToContain(String.format("The rule [%s] cannot match any resource under [%s]", rule, root));
    }

    public ConsoleOutMatcherJunit5 printedExcRunIfInfo(final String command, final String status) {
        return printedExcRunIfInfo(command, "", status);
    }

    public ConsoleOutMatcherJunit5 printedExcRunIfInfo(final String command, final String args, final String status) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isEmpty(args)) {
            builder.append(format("[%s] Current job status: %s", GoConstants.PRODUCT_NAME, status)).append("\n")
                    .append(format("[%s] Task: %s", GoConstants.PRODUCT_NAME, command));
        } else {
            builder.append(format("[%s] Current job status: %s", GoConstants.PRODUCT_NAME, status)).append("\n")
                    .append(format("[%s] Task: %s %s", GoConstants.PRODUCT_NAME, command, args));
        }

        return expectConsoleToContain(builder.toString());
    }

    public ConsoleOutMatcherJunit5 printedAppsMissingInfoOnUnix(final String app) {
        return expectConsoleToContain(format("Please make sure [%s] can be executed on this agent", app));
    }

    public ConsoleOutMatcherJunit5 printedAppsMissingInfoOnWindows(final String app) {
        return expectConsoleToContain(format("'%s' is not recognized as an internal or external command", app));
    }

    public ConsoleOutMatcherJunit5 matchUsingRegex(final String stringContainingRegex) {
        final boolean condition = !Pattern.compile(stringContainingRegex, Pattern.DOTALL).matcher(actual).find();
        return failIf(condition, "Expected console to contain [<%s>] but was <%s>.", stringContainingRegex, actual);
    }

    private ConsoleOutMatcherJunit5 expectConsoleToContain(String stdout) {
        return failIf(!StringUtils.contains(actual, stdout), "Expected console to contain [<%s>] but was <%s>.", stdout, actual);
    }

    private ConsoleOutMatcherJunit5 failIf(boolean condition, String message, Object... args) {
        if (invert) {
            if (!condition) {
                failWithMessage(message, args);
            }
        } else {
            if (condition) {
                failWithMessage(message, args);
            }
        }

        return this;
    }
}
