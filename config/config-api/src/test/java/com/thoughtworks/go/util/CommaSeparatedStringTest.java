/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.util.CommaSeparatedString.append;
import static com.thoughtworks.go.util.CommaSeparatedString.remove;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommaSeparatedStringTest {

    @Nested
    class Append {
        @Test
        public void shouldReturnACommaSeparatedStringWithEntriesToAppendWhenOriginalStringIsNULL() {
            String result = append(null, Arrays.asList("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        public void shouldReturnACommaSeparatedStringWithEntriesToAppendWhenOriginalStringIsEmpty() {
            String result = append("", Arrays.asList("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        public void shouldReturnMergedCommaSeparatedStringWhenOriginalStringContainsEntries() {
            String result = append("e2", Arrays.asList("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e2,e1,e3");
        }

        @Test
        public void shouldReturnOriginalCommaSeparatedStringWhenOriginalStringContainsAllEntries() {
            String result = append("e1,e2,e3", Arrays.asList("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        public void shouldReturnOriginalCommaSeparatedStringWhenEntriesAreNull() {
            String result = append("e1,e2,e3", null);
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        public void shouldReturnOriginalCommaSeparatedStringWhenEntriesAreEmpty() {
            String result = append("e1,e2,e3", Collections.emptyList());
            assertThat(result).isEqualTo("e1,e2,e3");
        }
    }


    @Nested
    class Remove {
        @Test
        public void shouldReturnANullCommaSeparatedStringWithoutEntriesToRemoveWhenOriginalStringIsNULL() {
            String result = remove(null, null);
            assertNull(result);
        }

        @Test
        public void shouldReturnABlankCommaSeparatedStringWithoutEntriesToAppendWhenOriginalStringIsEmpty() {
            String result = remove("", null);
            assertThat(result).isEqualTo("");
        }

        @Test
        public void shouldRemoveEntriesToRemoveWhenOriginalStringContainsEntries() {
            String result = remove("e1,e2,e3", Arrays.asList("e1", "e3"));
            assertThat(result).isEqualTo("e2");
        }

        @Test
        public void shouldReturnOriginalCommaSeparatedStringWhenOriginalStringDoesNotContainsEntries1() {
            String result = remove("e1,e2,e3", Arrays.asList("e4"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        public void shouldReturnOriginalCommaSeparatedStringWhenOriginalStringDoesNotContainsEntries2() {
            String result = remove("", Arrays.asList("e1", "e2"));
            assertThat(result).isEqualTo("");
        }

        @Test
        public void shouldReturnOriginalCommaSeparatedStringWhenOriginalStringDoesNotContainsEntries3() {
            String result = remove(null, Arrays.asList("e1", "e2"));
            assertNull(result);
        }

        @Test
        public void shouldReturnNullCommaSeparatedStringWhenItContainsAllEntries() {
            String result = remove("e1,e2", Arrays.asList("e1", "e2"));
            assertNull(result);
        }
    }


}