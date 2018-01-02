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

import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

import static com.thoughtworks.go.utils.Timeout.TWENTY_SECONDS;
import static com.thoughtworks.go.utils.Timeout.TWO_MINUTES;
import static org.junit.Assert.assertThat;

public class Assertions {
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
        } catch (InterruptedException ignored) {
        }
    }

    public static void waitUntil(Timeout timeout, Predicate predicate) {
        waitUntil(timeout, predicate, 1000);
    }

    public static void waitUntil(Timeout timeout, Predicate predicate, int sleepInMillis) {
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
                sleepMillis(sleepInMillis);
            }
        }
        String msg = e == null ? "wait timed out after " + timeout + " for: " + predicate.toString() : e.getMessage();
        throw new RuntimeException(msg, e);
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
