/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.util.CommaSeparatedString.append;
import static com.thoughtworks.go.util.CommaSeparatedString.remove;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommaSeparatedStringTest {
    @Nested
    class Append {
        @Test
        void shouldAppendValidListOfEntriesToNullCommaSeparatedString() {
            String result = append(null, asList("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        void shouldAppendValidListOfEntriesToEmptyCommaSeparatedString() {
            String result = append("", asList("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        void shouldAppendAndMergeListOfEntriesWithOriginalCommaSeparatedString() {
            String result = append("e2", asList("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e2,e1,e3");
        }

        @Test
        void shouldDoNothingWhenOriginalCommaSeparatedStringContainsAllEntriesInTheList() {
            String result = append("e1,e2,e3", asList("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        void shouldDoNothingWhenListOfEntriesIsNull() {
            String result = append("e1,e2,e3", null);
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        void shouldDoNothingWhenListOfEntriesIsEmpty() {
            String result = append("e1,e2,e3", emptyList());
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        void shouldAppendEntriesAfterRemovingLeadingAndTrailingSpaces() {
            String result = append("e1", asList(" e2 ", "", "   e3", "e4 "));
            assertThat(result).isEqualTo("e1,e2,e3,e4");
        }

        @Test
        void shouldNotAppendNullEntriesInTheList() {
            String result = append("e1", asList(null, "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }
    }

    @Nested
    class Remove {
        @Test
        void shouldDoNothingWhenEntriesToRemoveIsNull() {
            String result = remove(null, null);
            assertNull(result);

            result = remove("", null);
            assertThat(result).isEqualTo("");

            result = remove("e1,e2", null);
            assertThat(result).isEqualTo("e1,e2");
        }

        @Test
        void shouldRemoveEntriesToRemoveWhenOriginalStringContainsEntries() {
            String result = remove("e1,e2,e3", asList("e1", "e3"));
            assertThat(result).isEqualTo("e2");
        }

        @Test
        void shouldDoNothingWhenEntriesToRemoveDoesNotContainsEntriesInOriginalCommaSeparatedString() {
            String result = remove("e1,e2,e3", singletonList("e4"));
            assertThat(result).isEqualTo("e1,e2,e3");

            result = remove("", asList("e1", "e2"));
            assertThat(result).isEqualTo("");

            result = remove(null, asList("e1", "e2"));
            assertNull(result);
        }

        @Test
        void shouldRemoveAllEntriesWhenListOfEntriesToRemoveContainsAllEntriesInOriginalCommaSeparatedString() {
            String result = remove("e1,e2", asList("e1", "e2"));
            assertNull(result);
        }
    }
}