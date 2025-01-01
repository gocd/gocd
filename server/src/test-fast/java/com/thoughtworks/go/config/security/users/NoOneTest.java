/*
 * Copyright Thoughtworks, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

public class NoOneTest {
    @Test
    public void shouldSayItContainsNoUsersAlways() throws Exception {
        Users noUsers = NoOne.INSTANCE;

        assertThat(noUsers.contains("abc")).isFalse();
        assertThat(noUsers.contains("def")).isFalse();
        assertThat(noUsers.contains(null)).isFalse();
        assertThat(noUsers.contains("")).isFalse();
    }
}
