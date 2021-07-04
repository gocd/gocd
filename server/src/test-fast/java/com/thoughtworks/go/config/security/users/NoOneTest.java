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
package com.thoughtworks.go.config.security.users;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NoOneTest {
    @Test
    public void shouldSayItContainsNoUsersAlways() throws Exception {
        Users noUsers = NoOne.INSTANCE;

        assertThat(noUsers.contains("abc"), is(false));
        assertThat(noUsers.contains("def"), is(false));
        assertThat(noUsers.contains(null), is(false));
        assertThat(noUsers.contains(""), is(false));
    }
}
