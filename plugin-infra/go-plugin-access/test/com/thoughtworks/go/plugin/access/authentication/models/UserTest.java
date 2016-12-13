/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.authentication.models;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class UserTest {

    @Test
    public void shouldAbleToDeserializeUserJSON() throws Exception {

        String json = "[\n" +
                "  {\n" +
                "    \"username\": \"bob\",\n" +
                "    \"display_name\": \"Bob\",\n" +
                "    \"email\": \"bob@example.com\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"username\": \"jdoe\",\n" +
                "    \"display_name\": \"John Doe\",\n" +
                "    \"email\": \"jdoe@example.com\"\n" +
                "  }\n" +
                "]";

        List<User> users = User.fromJSONList(json);

        assertThat(users, hasSize(2));
        assertThat(users.get(0), is(new User("bob", "Bob", "bob@example.com")));
        assertThat(users.get(1), is(new User("jdoe", "John Doe", "jdoe@example.com")));

    }
}