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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class BaseCollectionTest {
    @Test
    public void shouldReturnFirstOrLastItem() {
        Object item1 = new Object();
        Object item2 = new Object();
        BaseCollection collection = new BaseCollection(item1, item2);
        assertThat(collection.first()).isEqualTo(item1);
        assertThat(collection.last()).isEqualTo(item2);
    }

    @Test
    public void shouldReturnNullForEmptyCollection() {
        BaseCollection collection = new BaseCollection();
        assertThat(collection.first()).isNull();
        assertThat(collection.last()).isNull();
    }

    @Test
    public void shouldReplaceOldItemWithNewItem() {
        Object oldItem = new Object();
        Object newItem = new Object();

        BaseCollection collection = new BaseCollection(oldItem);

        assertThat(collection.size()).isEqualTo(1);
        assertThat(collection.first()).isEqualTo(oldItem);

        collection.replace(oldItem, newItem);

        assertThat(collection.size()).isEqualTo(1);
        assertThat(collection.first()).isEqualTo(newItem);
    }

    @Test
    public void shouldReplaceAtTheIndexOfOldItem() {
        Object newItem = new Object();
        BaseCollection collection = new BaseCollection(new Object(), new Object());

        collection.replace(1, newItem);

        assertThat(collection.last()).isEqualTo(newItem);
    }

    @Test
    public void shouldThrowAnExceptionWhenItemToBeReplacedIsNotFound() {
        String oldItem = "foo";
        String newItem = "bar";

        BaseCollection collection = new BaseCollection("foobar");

        assertThatThrownBy(() -> collection.replace(oldItem, newItem))
                .isExactlyInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("There is no object at index '-1' in this collection of java.lang.String");
    }

    @Test
    public void shouldThrowAnExceptionWhenIndexIsInvalid() {
        Object newItem = new Object();
        BaseCollection collection = new BaseCollection(new Object(), new Object());

        assertThatThrownBy(() -> collection.replace(-1, newItem))
                .isExactlyInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("There is no object at index '-1' in this collection of java.lang.Object");
    }

    @Test
    public void shouldThrowAnExceptionWhenIndexIsGreaterThanSizeOfCollection() {
        Object newItem = new Object();
        BaseCollection collection = new BaseCollection(new Object(), new Object());

        assertThatThrownBy(() -> collection.replace(3, newItem))
                .isExactlyInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("There is no object at index '3' in this collection of java.lang.Object");
    }

}



