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
package com.thoughtworks.go.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class TestUtils {

    protected TestUtils() {

    }

    public static TypeSafeMatcher<String> contains(final String toMatch) {
        return new TypeSafeMatcher<>() {
            @Override
            public boolean matchesSafely(String underTest) {
                return underTest.contains(toMatch);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("The actual string does not contain the expected string.");
            }
        };
    }

    public static void copyAndClose(InputStream fileInputStream, OutputStream fileOutputStream) {
        try (InputStream is = fileInputStream; OutputStream os = fileOutputStream) {
            is.transferTo(os);
        } catch (IOException e) {
            bomb(e);
        }
    }

    public static void sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
