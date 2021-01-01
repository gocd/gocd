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
package com.thoughtworks.go.util;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JsonValueTest {

    @Test
    public void shouldBeAbleToGetString() {
        assertThat(new JsonValue("{\"key\": \"value\"}").getString("key"), is("value"));
    }

    @Test
    public void shouldKeepDoubleQuotesInJsonValue() {
        assertThat(new JsonValue("{\"key\": \"va\\\"lue\"}").getString("key"), is("va\\\"lue"));
    }

    @Test
    public void shouldBeAbleToGetObject() {
        String json = "{\"key\": {\"innerKey\" : \"value\"}}";
        JsonValue jsonValue = new JsonValue(json);
        JsonValue inner = jsonValue.getObject("key");
        assertThat(inner.getString("innerKey"), is("value"));
    }

    @Test
    public void shouldThrowExceptionWithFriendlyMessageWhenGivenWrongKeyType() throws Exception {
        try {
            new JsonValue("[\"value1\", \"value2\"]").getObject("key");
            fail("should throw exception if given wrong key type");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Key 'key' does not refer to any attribute of JSONArray"));
        }
    }
}
