/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.matchers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.thoughtworks.go.util.GoConstants;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static java.lang.String.format;

public class ConsoleOutMatcher {
    public static TypeSafeMatcher<String> printedDeprecatedEnvVariable(final String key, final Object value) {
        final TypeSafeMatcher<String> matcher = printedEnvVariable(key, value);
        return new TypeSafeMatcher<String>() {
            private String consoleOut;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                return matcher.matches(consoleOut) && hasDeprecatedMessage(consoleOut);
            }

            private boolean hasDeprecatedMessage(String consoleOut) {
                return StringUtils.contains(consoleOut, deprecatedMessage());
            }

            private String deprecatedMessage() {
                return format("value '%s' (Deprecated. Use '%s' instead.)",
                        value, key.replace("CRUISE_", "GO_"));
            }

            public void describeTo(Description description) {                
                description.appendText(deprecatedMessage());
            }
        };
    }

    public static TypeSafeMatcher<String> printedEnvVariable(final String key, final Object value) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String set;
            public String override;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                set = format("environment variable '%s' to value '%s'", key, value);
                override = format("environment variable '%s' with value '%s'", key, value);
                return StringUtils.contains(consoleOut, set) || StringUtils.contains(consoleOut, override);
            }

            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + set + "] or [" + override + "]" + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedPreparingInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Start to prepare %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]" + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedBuildingInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Start to build %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]" + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedUploadingInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Start to upload %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]" + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedJobCompletedInfo(final Object jobIdentifer) {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Job completed %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

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

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                stdout = format("Job is canceled %s", jobIdentifer.toString());
                return StringUtils.contains(consoleOut, stdout);
            }

            public void describeTo(Description description) {
                description.appendText("expected console to contain [" + stdout + "]"
                        + " but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedBuildFailed() {
        return new TypeSafeMatcher<String>() {
            private String consoleOut;
            public String stdout;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                return StringUtils.contains(consoleOut.toLowerCase(), "build failed");
            }

            public void describeTo(Description description) {
                description.appendText("expected console to contain [build failed] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<List> containsState(final Object jobState) {
        return new TypeSafeMatcher<List>() {
            private List states;

            public boolean matchesSafely(List states) {
                this.states = states;
                return states.contains(jobState);
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + jobState + "] but was " + states);
            }
        };
    }

    public static TypeSafeMatcher<List> containsResult(final Object jobResult) {
        return new TypeSafeMatcher<List>() {
            private List results;

            public boolean matchesSafely(List states) {
                this.results = states;
                return states.contains(jobResult);
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + jobResult + "] but was " + results);
            }
        };
    }

    public static TypeSafeMatcher<String> printedUploadingFailure(final File file) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                this.message = "Failed to upload " + file.getAbsolutePath();
                return StringUtils.contains(consoleOut.toLowerCase(), message.toLowerCase());
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedNotFoundFailure(final File file) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            public boolean matchesSafely(String consoleOut) {
                try {
                    this.consoleOut = consoleOut;
                    this.message = "Failed to find " + file.getCanonicalPath();
                    return StringUtils.contains(consoleOut, message);
                } catch (IOException e) {
                    return false;
                }
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedNotUploadedFailure(final String... names) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                StringBuilder builder = new StringBuilder();
                for (String name : names) {
                    builder.append('[').append(name).append(']');
                }
                this.message = "Failed to upload " + builder;

                return StringUtils.contains(consoleOut, message);
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedRuleDoesNotMatchFailure(final String root, final String rule) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                this.message = "The rule [" + rule + "] cannot match any resource under [" + root + "]";

                return StringUtils.contains(consoleOut, message);
            }

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

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                if (StringUtils.isEmpty(args)) {
                    this.message = format("[%s] Current job status: %s.", GoConstants.PRODUCT_NAME, status);
                    this.message = format("[%s] Start to execute task: <exec command=\"%s\" />.", GoConstants.PRODUCT_NAME, command);
                } else {
                    this.message = format("[%s] Current job status: %s.", GoConstants.PRODUCT_NAME, status);
                    this.message = format("[%s] Start to execute task: <exec command=\"%s\" args=\"%s\" />.", GoConstants.PRODUCT_NAME, command, args);
                }
                return StringUtils.contains(consoleOut, message);
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedAppsMissingInfoOnUnix(final String app) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                message = format("Please make sure [%s] can be executed on this agent", app);
                return StringUtils.contains(consoleOut, message);
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<String> printedAppsMissingInfoOnWindows(final String app) {
        return new TypeSafeMatcher<String>() {
            public String consoleOut;
            public String message;

            public boolean matchesSafely(String consoleOut) {
                this.consoleOut = consoleOut;
                message = format("'%s' is not recognized as an internal or external command", app);
                return StringUtils.contains(consoleOut, message);
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + message + "] but was " + consoleOut);
            }
        };
    }

    public static TypeSafeMatcher<List<UploadEntry>> uploadFileToDestination(final File file, final String dest) {
        return new TypeSafeMatcher<List<UploadEntry>>() {
            private List<UploadEntry> entries;
            private UploadEntry uploadEntry;

            public boolean matchesSafely(List<UploadEntry> entries) {
                this.entries = entries;

                uploadEntry = new UploadEntry(file, dest);
                return entries.contains(uploadEntry);
            }

            public void describeTo(Description description) {
                description.appendText("Expected console to contain [" + uploadEntry + "] but was " + entries);
            }
        };
    }
}
