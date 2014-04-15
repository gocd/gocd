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

package com.thoughtworks.go.util;

import com.thoughtworks.go.util.json.Json;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public final class JsonContains extends TypeSafeMatcher<Json> {

    private final Json expected;

    private JsonContains(Json expected) {
        this.expected = expected;
    }

    public boolean matchesSafely(Json original) {
        return original.contains(expected);
    }

    public void describeTo(Description description) {
        description.appendText("to contain " + expected.toString());
    }

    public static JsonContains contains(Json json) {
        return new JsonContains(json);
    }
}
