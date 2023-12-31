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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.server.messaging.StubScheduleCheckCompletedListener;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.HashSet;
import java.util.List;

public class ScheduleCheckMatcher {
    public static Matcher<String[]> scheduleCheckCompleted(final StubScheduleCheckCompletedListener listener) {
        return new TypeSafeMatcher<>() {
            private List<String> expected;
            private List<String> actual;

            @Override
            public boolean matchesSafely(String[] expected) {
                this.expected = List.of(expected);
                this.actual = listener.pipelines;
                return new HashSet<>(actual).containsAll(this.expected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Expect to complete material checking for %s but got %s", expected, actual));
            }
        };
    }

}
