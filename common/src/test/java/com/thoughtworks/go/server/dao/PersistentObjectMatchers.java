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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.PersistentObject;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.Description;

public class PersistentObjectMatchers {
    public static Matcher<PersistentObject> hasSameId(final PersistentObject expected) {
        return new TypeSafeMatcher<PersistentObject>() {
            public String message;

            @Override
            public boolean matchesSafely(PersistentObject item) {
                message = "should have same database id. Expected " + expected.getId() + " but was " + item.getId();
                return expected.getId() == item.getId();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(message);
            }
        };

    }
}
