/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.utils;

import static junit.framework.Assert.assertTrue;

import java.util.regex.Pattern;

import static com.thoughtworks.go.utils.Timeout.TWENTY_SECONDS;
import static com.thoughtworks.go.utils.Timeout.TWO_MINUTES;
import com.thoughtworks.go.util.FuncVarArg;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class Assertions {
    public static <T> void assertNeverHappens(T obj, Matcher<T> matcher) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < TWENTY_SECONDS.inMillis()) {
            assertThat(obj, not(matcher));
            sleepOneSec();
        }
    }

    public static <T> void assertWillHappen(T obj, Matcher<T> matcher) {
        assertWillHappen(obj, matcher, TWO_MINUTES);
    }

    public static <T> void assertWillHappen(T obj, Matcher<T> matcher, Timeout timeout) {
        assertWillHappen(obj, matcher, timeout.inMillis());
    }

    public static <T> void assertWillHappen(T obj, Matcher<T> matcher, long timeInMillis) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeInMillis) {
            try {
                if (matcher.matches(obj)) {
                    return;
                }
            } catch (RuntimeException e) {
                System.err.println("Caught " + e + " while waiting for matcher " + matcher.getClass().getSimpleName()
                        + " to complete, will continue waiting till timeout");
            }
            sleepOneSec();
        }
        assertThat(String.format("Expected the following would happen in %s milliseconds ", timeInMillis), obj, matcher);
    }

    public static <T> void assertAlwaysHappens(T obj, Matcher<T> matcher) {
        assertAlwaysHappens(obj, matcher, TWENTY_SECONDS);
    }

    public static <T> void assertAlwaysHappens(T obj, Matcher<T> matcher, Timeout timeout) {
        assertAlwaysHappens(obj, matcher, timeout.inMillis());
    }

    public static <T> void assertAlwaysHappens(T obj, Matcher<T> matcher, long timeoutInMillis) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutInMillis) {
            assertThat("Expected the following would always happen in %s milliseconds ", obj, matcher);
            sleepOneSec();
        }
    }

    public static void sleepOneSec() {
        sleepMillis(1000);
    }

    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static <T> T waitFor(Timeout timeout, FuncVarArg<T,Object> func) {
        long end = System.currentTimeMillis() + timeout.inMillis();
        Exception e = null;
        while (true) {
            T answer;
            try {
                if ((answer = func.call()) != null) {
                    return answer;
                }
            } catch (Exception caught) {
                e = caught;
            }

            boolean timedout = System.currentTimeMillis() > end;
            if (timedout) {
                break;
            } else {
                sleepOneSec();
            }
        }
        String msg = e == null ? "wait timed out after " + timeout + " for: " + func.toString() : e.getMessage();
        throw new RuntimeException(msg, e);
    }

    public static void waitUntil(Timeout timeout, Predicate predicate) {
        long end = System.currentTimeMillis() + timeout.inMillis();
        Exception e = null;
        while (true) {
            try {
                if (predicate.call()) {
                    return;
                }
            } catch (Exception caught) {
                System.err.println("retrying after catching exception in Assertions.waitUntil: " + caught);
                e = caught;
            }

            boolean timedout = System.currentTimeMillis() > end;
            if (timedout) {
                break;
            } else {
                sleepOneSec();
            }
        }
        String msg = e == null ? "wait timed out after " + timeout + " for: " + predicate.toString() : e.getMessage();
        throw new RuntimeException(msg, e);
    }

    public static org.hamcrest.Matcher<String> containsPattern(final String strPattern) {
        final Pattern pattern = Pattern.compile(strPattern);

        return new BaseMatcher<String>() {

            public boolean matches(Object item) {
                return pattern.matcher(((String) item)).find();
            }

            public void describeTo(Description description) {
                description.appendText("expected to match pattern " + pattern);
            }

        };
    }
    

    public static void assertOverTime(Timeout timeout, FuncVarArg<Boolean,Object> func) {
        long end = System.currentTimeMillis() + timeout.inMillis();
        while (true) {
            assertTrue(func.call());
            boolean timedout = System.currentTimeMillis() > end;
            if (timedout) {
                break;
            } else {
                sleepOneSec();
            }
        }
    }

    public interface Predicate {
        boolean call() throws Exception;
    }

    public static abstract class Assertion<T> implements Predicate {
        public boolean call() {
            try {
                assertThat(actual(), Is.is(expected()));
            } catch (AssertionError e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        public abstract T actual();

        public abstract T expected();
    }
}
