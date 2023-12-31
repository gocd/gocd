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
package com.thoughtworks.go.util.comparator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AlphaAsciiCollectionComparatorTest {
    private class Foo implements Comparable<Foo> {
        private final String value;

        Foo(String value) {
            this.value = value;
        }

        @Override public String toString() {
            return value;
        }

        @Override
        public int compareTo(Foo other) {
            return value.compareTo(other.value);
        }
    }

    @Test
    public void shouldCompareSortedCollections() {
        AlphaAsciiCollectionComparator<Foo> comparator = new AlphaAsciiCollectionComparator<>();
        assertThat(comparator.compare(List.of(new Foo("foo"), new Foo("quux")), List.of(new Foo("foo"), new Foo("bar"))), greaterThan(0));
        assertThat(comparator.compare(List.of(new Foo("foo"), new Foo("abc")), List.of(new Foo("foo"), new Foo("bar"))), lessThan(0));
        assertThat(comparator.compare(List.of(new Foo("foo"), new Foo("bar")), List.of(new Foo("bar"), new Foo("foo"))), is(0));
    }
}
