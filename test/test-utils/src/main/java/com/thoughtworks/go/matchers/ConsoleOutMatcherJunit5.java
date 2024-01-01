/*
 * Copyright 2024 Thoughtworks, Inc.
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

import java.util.regex.Pattern;

import static java.lang.String.format;

@SuppressWarnings("UnusedReturnValue")
public class ConsoleOutMatcherJunit5 extends AbstractAssert<ConsoleOutMatcherJunit5, String> {

    public ConsoleOutMatcherJunit5(String consoleOut) {
        super(consoleOut, ConsoleOutMatcherJunit5.class);
    }

    public static ConsoleOutMatcherJunit5 assertConsoleOut(String actual) {
        return new ConsoleOutMatcherJunit5(actual);
    }

    public ConsoleOutMatcherJunit5 contains(final String str) {
        if (!StringUtils.contains(actual, str)) {
            failWithMessage("Expected console to contain [<%s>] but was <%s>.", str, actual);
        }
        return this;
    }

    public ConsoleOutMatcherJunit5 printedPreparingInfo(final Object jobIdentifier) {
        return contains(format("Start to prepare %s", jobIdentifier.toString()));
    }

    public ConsoleOutMatcherJunit5 printedBuildingInfo(final Object jobIdentifier) {
        return contains(format("Start to build %s", jobIdentifier.toString()));
    }

    public ConsoleOutMatcherJunit5 printedUploadingInfo(final Object jobIdentifier) {
        return contains(format("Start to upload %s", jobIdentifier.toString()));
    }

    public ConsoleOutMatcherJunit5 printedBuildFailed() {
        final String buildFailed = "build failed";
        if (!StringUtils.contains(actual.toLowerCase(), buildFailed)) {
            failWithMessage("Expected console to contain [<%s>] but was <%s>.", buildFailed, actual);
        }
        return this;
    }

    public ConsoleOutMatcherJunit5 printedRuleDoesNotMatchFailure(final String root, final String rule) {
        return contains(String.format("The rule [%s] cannot match any resource under [%s]", rule, root));
    }

    public ConsoleOutMatcherJunit5 printedExcRunIfInfo(final String command, final String status) {
        return printedExcRunIfInfo(command, "", status);
    }

    public ConsoleOutMatcherJunit5 printedExcRunIfInfo(final String command, final String args, final String status) {
        contains(buildJobStatusString(status));
        return contains(buildTaskInfoString(command, args));
    }

    public ConsoleOutMatcherJunit5 doesNotContainExcRunIfInfo(final String command, final String args) {
        return doesNotContain(buildTaskInfoString(command, args));
    }

    public ConsoleOutMatcherJunit5 printedAppsMissingInfoOnUnix(final String app) {
        return contains(format("Please make sure [%s] can be executed on this agent", app));
    }

    public ConsoleOutMatcherJunit5 printedAppsMissingInfoOnWindows(final String app) {
        return contains(format("'%s' is not recognized as an internal or external command", app));
    }

    public ConsoleOutMatcherJunit5 matchUsingRegex(final String stringContainingRegex) {
        final boolean condition = Pattern.compile(stringContainingRegex, Pattern.DOTALL).matcher(actual).find();
        if (!condition) {
            failWithMessage("Expected console to contain [<%s>] but was <%s>.", stringContainingRegex, actual);
        }
        return this;
    }

    public ConsoleOutMatcherJunit5 doesNotContain(String str) {
        if (StringUtils.contains(actual, str)) {
            failWithMessage("Expected console to not contain [<%s>] but was <%s>.", str, actual);
        }
        return this;
    }


    private String buildJobStatusString(String status) {
        return format("[%s] Current job status: %s", GoConstants.PRODUCT_NAME, status);
    }

    private String buildTaskInfoString(String command, String args) {
        if (StringUtils.isEmpty(args)) {
            return format("[%s] Task: %s", GoConstants.PRODUCT_NAME, command);
        }
        return format("[%s] Task: %s %s", GoConstants.PRODUCT_NAME, command, args);
    }

}
