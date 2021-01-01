/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;
import java.util.List;

import static java.lang.String.format;

public class ConsoleOutMatcher {

    public static TypeSafeMatcher<String> printedEnvVariable(final String key, final Object value) {
        return new TypeSafeMatcher<String>() {
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

    public static TypeSafeMatcher<String> printedPreparingInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Start to prepare %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]" + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedBuildingInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Start to build %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]" + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedUploadingInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Start to upload %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]" + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedJobCompletedInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Job completed %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]"
                        + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedJobCanceledInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Job is canceled %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]"
                        + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedBuildFailed() {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                return StringUtils.contains(consoleOut.toLowerCase(), "build failed");
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected console to contain [build failed] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<List> containsState(final Object jobState) {
        return new TypeSafeMatcher<List>() {
            private List states;

            @Override
            public boolean matchesSafely(List states) {
                this.states = states;
                return states.contains(jobState);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + jobState + "] but was " + states);
            }
        };
    }

    public static TypeSafeMatcher<List> containsResult(final Object jobResult) {
        return new TypeSafeMatcher<List>() {
            private List results;

            @Override
            public boolean matchesSafely(List states) {
                this.results = states;
                return states.contains(jobResult);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + jobResult + "] but was " + results);
            }
        };
    }

    public static TypeSafeMatcher<String> printedUploadingFailure(final File file) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                this.message = "Failed to upload " + file.getAbsolutePath();
                return StringUtils.contains(consoleOut.toLowerCase(), message.toLowerCase());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedRuleDoesNotMatchFailure(final String root, final String rule) {
        return new TypeSafeMatcher<String>() {
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

    public static TypeSafeMatcher<String> printedExcRunIfInfo(final String command, final String status) {
        return printedExcRunIfInfo(command, "", status);

    }

    public static TypeSafeMatcher<String> printedExcRunIfInfo(final String command, final String args,
                                                              final String status) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                if (StringUtils.isEmpty(args)) {
                    this.message = format("[%s] Current job status: %s", GoConstants.PRODUCT_NAME, status);
                    this.message = format("[%s] Task: %s", GoConstants.PRODUCT_NAME, command);
                } else {
                    this.message = format("[%s] Current job status: %s", GoConstants.PRODUCT_NAME, status);
                    this.message = format("[%s] Task: %s %s", GoConstants.PRODUCT_NAME, command, args);
                }
                return StringUtils.contains(consoleOut, message);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedAppsMissingInfoOnUnix(final String app) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                message = format("Please make sure [%s] can be executed on this agent", app);
                return StringUtils.contains(consoleOut, message);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedAppsMissingInfoOnWindows(final String app) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            @Override
            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                message = format("'%s' is not recognized as an internal or external command", app);
                return StringUtils.contains(consoleOut, message);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<List<UploadEntry>> uploadFileToDestination(final File file, final String dest) {
        return new TypeSafeMatcher<List<UploadEntry>>() {
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
