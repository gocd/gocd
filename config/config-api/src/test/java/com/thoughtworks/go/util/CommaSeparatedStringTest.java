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
package com.thoughtworks.go.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.util.CommaSeparatedString.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class CommaSeparatedStringTest {

    @Nested
    class Sanitization {
        @Test
        void sanitizeShouldNormalizeBlankEntriesToNull() {
            assertThat(normalizeToNull(null)).isNull();
            assertThat(normalizeToNull("")).isNull();
            assertThat(normalizeToNull(" ")).isNull();
            assertThat(normalizeToNull(",")).isNull();
            assertThat(normalizeToNull(" , ,,")).isNull();
        }

        @Test
        void sanitizeShouldSortDistinct() {
            assertThat(normalizeToNull("b,a")).isEqualTo("a,b");
            assertThat(normalizeToNull("b, a , b,,")).isEqualTo("a,b");
        }
    }

    @Nested
    class Append {
        @Test
        void shouldAppendValidListOfEntriesToNullCommaSeparatedString() {
            String result = append(null, List.of("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        void shouldAppendValidListOfEntriesToEmptyCommaSeparatedString() {
            String result = append("", List.of("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }

        @Test
        void shouldAppendAndMergeListOfEntriesWithOriginalCommaSeparatedString() {
            String result = append("e2", List.of("e1", "e2", "e3"));
            assertThat(result).isEqualTo("e2,e1,e3");
        }

        @Test
        void shouldDoNothingWhenOriginalCommaSeparatedStringContainsAllEntriesInTheList() {
            String result = append("e1,e2,e3", List.of("e1", "e2", "e3"));
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
            String result = append("e1", List.of(" e2 ", "", "   e3", "e4 "));
            assertThat(result).isEqualTo("e1,e2,e3,e4");
        }

        @Test
        void shouldNotAppendNullEntriesInTheList() {
            String result = append("e1", Arrays.asList(null, "e2", "e3"));
            assertThat(result).isEqualTo("e1,e2,e3");
        }
    }

    @Nested
    class Remove {
        @Test
        void shouldDoNothingWhenEntriesToRemoveIsNull() {
            assertThat(remove(null, null)).isNull();
            assertThat(remove("", null)).isEmpty();
            assertThat(remove("e1,e2", null)).isEqualTo("e1,e2");
        }

        @Test
        void shouldRemoveEntriesToRemoveWhenOriginalStringContainsEntries() {
            String result = remove("e1,e2,e3", List.of("e1", "e3"));
            assertThat(result).isEqualTo("e2");
        }

        @Test
        void shouldDoNothingWhenEntriesToRemoveDoesNotContainsEntriesInOriginalCommaSeparatedString() {
            assertThat(remove("e1,e2,e3", List.of("e4"))).isEqualTo("e1,e2,e3");

            assertThat(remove("", List.of("e1", "e2"))).isEmpty();

            assertThat(remove(null, List.of("e1", "e2"))).isNull();
        }

        @Test
        void shouldRemoveAllEntriesWhenListOfEntriesToRemoveContainsAllEntriesInOriginalCommaSeparatedString() {
            String result = remove("e1,e2", List.of("e1", "e2"));
            assertThat(result).isNull();
        }
    }
}