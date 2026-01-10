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
package com.thoughtworks.go.domain;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class BaseCollectionTest {
    @Test
    public void shouldReplaceOldItemWithNewItem() {
        Object oldItem = new Object();
        Object newItem = new Object();

        BaseCollection<Object> collection = new BaseCollection<>(oldItem);

        assertThat(collection.size()).isEqualTo(1);
        assertThat(collection.getFirst()).isEqualTo(oldItem);

        collection.replaceIfNotEmpty(oldItem, newItem);

        assertThat(collection.size()).isEqualTo(1);
        assertThat(collection.getFirst()).isEqualTo(newItem);
    }

    @Test
    public void shouldReplaceAtTheIndexOfOldItem() {
        Object newItem = new Object();
        BaseCollection<Object> collection = new BaseCollection<>(new Object(), new Object());

        collection.setIfNotEmpty(1, newItem);

        assertThat(collection.getLast()).isEqualTo(newItem);
    }

    @Test
    public void shouldThrowAnExceptionWhenItemToBeReplacedIsNotFound() {
        String oldItem = "foo";
        String newItem = "bar";

        BaseCollection<Object> collection = new BaseCollection<>("foobar");

        assertThatThrownBy(() -> collection.replaceIfNotEmpty(oldItem, newItem))
                .isExactlyInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("There is no object at index '-1' in this collection of java.lang.String");
    }

    @Test
    public void setShouldThrowAnExceptionWhenIndexIsInvalid() {
        Object newItem = new Object();
        BaseCollection<Object> collection = new BaseCollection<>(new Object(), new Object());

        assertThatThrownBy(() -> collection.setIfNotEmpty(-1, newItem))
                .isExactlyInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("There is no object at index '-1' in this collection of java.lang.Object");
    }

    @Test
    public void shouldReplaceByPredicateWithNewItem() {
        BaseCollection<String> collection = new BaseCollection<>("hello1", "hello2", "world");

        Predicate<String> prefixedByHello = s -> s.startsWith("hello");
        collection.replaceIfNotEmpty(prefixedByHello, "replaced");

        assertThat(collection).containsExactly("replaced", "hello2", "world");
    }

    @Test
    public void replaceShouldNotThrowExceptionIfEmpty() {
        BaseCollection<String> collection = new BaseCollection<>();
        Predicate<String> prefixedByHello = s -> s.startsWith("hello");
        collection.replaceIfNotEmpty(prefixedByHello, "replaced");

        assertThat(collection).isEmpty();
    }

    @Test
    public void replaceShouldThrowExceptionIfInvalid() {
        BaseCollection<String> collection = new BaseCollection<>("world");

        Predicate<String> prefixedByHello = s -> s.startsWith("hello");
        assertThatThrownBy(() -> collection.replaceIfNotEmpty(prefixedByHello, "replaced"))
            .isExactlyInstanceOf(IndexOutOfBoundsException.class)
            .hasMessage("There is no object at index '-1' in this collection of java.lang.String");
    }

    @Test
    public void shouldThrowAnExceptionWhenIndexIsGreaterThanSizeOfCollection() {
        Object newItem = new Object();
        BaseCollection<Object> collection = new BaseCollection<>(new Object(), new Object());

        assertThatThrownBy(() -> collection.setIfNotEmpty(3, newItem))
                .isExactlyInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("There is no object at index '3' in this collection of java.lang.Object");
    }

    @Test
    public void shouldRemoveFirstIfWithEmptyCollection() {
        BaseCollection<String> collection = new BaseCollection<>();
        assertThat(collection.removeFirstIf(o -> true)).isFalse();
        assertThat(collection.removeFirstIf(o -> false)).isFalse();
        assertThat(collection).isEmpty();
    }

    @Test
    public void shouldRemoveFirstIfOnlyFirstElement() {
        BaseCollection<String> collection = new BaseCollection<>("hello1", "hello2", "world");
        Predicate<String> prefixedByHello = s -> s.startsWith("hello");
        assertThat(collection.removeFirstIf(prefixedByHello)).isTrue();
        assertThat(collection).containsExactly("hello2", "world");
        assertThat(collection.removeFirstIf(prefixedByHello)).isTrue();
        assertThat(collection).containsExactly("world");
        assertThat(collection.removeFirstIf(prefixedByHello)).isFalse();
        assertThat(collection).containsExactly("world");
    }

}



