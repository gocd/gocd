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

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;
import java.util.List;

import static java.lang.String.format;

public class ConsoleOutMatcher {

    public static TypeSafeMatcher<String> printedEnvVariable(final String key, final Object value) {
        return new TypeSafeMatcher<>() {
            private String consoleOut;
            public String set;
            public String override;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                set = format("environment variable '%s' to value '%s'", key, value);
                override = format("environment variable '%s' with value '%s'", key, value);
                return StringUtils.contains(consoleOut, set) || StringUtils.contains(consoleOut, override);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + set + "] or [" + override + "]" + " but was " + consoleOut);
            }
        };
    }

    public static <T> TypeSafeMatcher<List<T>> containsResult(final T jobResult) {
        return new TypeSafeMatcher<>() {
            private List<?> results;

            @Override
            public boolean matchesSafely(List<T> states) {
                this.results = states;
                return states.contains(jobResult);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + jobResult + "] but was " + results);
            }
        };
    }

    public static TypeSafeMatcher<String> printedRuleDoesNotMatchFailure(final String root, final String rule) {
        return new TypeSafeMatcher<>() {
            public String consoleOut;
            public String message;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                this.message = "The rule [" + rule + "] cannot match any resource under [" + root + "]";

                return StringUtils.contains(consoleOut, message);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<List<UploadEntry>> uploadFileToDestination(final File file, final String dest) {
        return new TypeSafeMatcher<>() {
            private List<UploadEntry> entries;
            private UploadEntry uploadEntry;

            @Override
            public boolean matchesSafely(List<UploadEntry> entries) {
                this.entries = entries;

                uploadEntry = new UploadEntry(file, dest);
                return entries.contains(uploadEntry);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + uploadEntry + "] but was " + entries);
            }
        };
    }
}
