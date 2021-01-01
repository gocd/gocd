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
package com.thoughtworks.go.domain;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class BaseCollectionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldReturnFirstOrLastItem() {
        Object item1 = new Object();
        Object item2 = new Object();
        BaseCollection collection = new BaseCollection(item1, item2);
        assertThat(collection.first(), is(item1));
        assertThat(collection.last(), is(item2));
    }

    @Test
    public void shouldReturnNullForEmptyCollection() {
        BaseCollection collection = new BaseCollection();
        assertThat(collection.first(), is(nullValue()));
        assertThat(collection.last(), is(nullValue()));
    }

    @Test
    public void shouldReplaceOldItemWithNewItem() {
        Object oldItem = new Object();
        Object newItem = new Object();

        BaseCollection collection = new BaseCollection(oldItem);

        assertThat(collection.size(), is(1));
        assertThat(collection.first(), is(oldItem));

        collection.replace(oldItem, newItem);

        assertThat(collection.size(), is(1));
        assertThat(collection.first(), is(newItem));
    }

    @Test
    public void shouldReplaceAtTheIndexOfOldItem() {
        Object newItem = new Object();
        BaseCollection collection = new BaseCollection(new Object(), new Object());

        collection.replace(1, newItem);

        assertThat(collection.last(), is(newItem));
    }

    @Test
    public void shouldThrowAnExceptionWhenItemToBeReplacedIsNotFound() {
        String oldItem = "foo";
        String newItem = "bar";

        BaseCollection collection = new BaseCollection("foobar");

        thrown.expect(IndexOutOfBoundsException.class);
        thrown.expectMessage("There is no object at index '-1' in this collection of java.lang.String");

        collection.replace(oldItem, newItem);
    }

    @Test
    public void shouldThrowAnExceptionWhenIndexIsInvalid() {
        Object newItem = new Object();
        BaseCollection collection = new BaseCollection(new Object(), new Object());

        thrown.expect(IndexOutOfBoundsException.class);
        thrown.expectMessage("There is no object at index '-1' in this collection of java.lang.Object");

        collection.replace(-1, newItem);
    }

    @Test
    public void shouldThrowAnExceptionWhenIndexIsGreaterThanSizeOfCollection() {
        Object newItem = new Object();
        BaseCollection collection = new BaseCollection(new Object(), new Object());

        thrown.expect(IndexOutOfBoundsException.class);
        thrown.expectMessage("There is no object at index '3' in this collection of java.lang.Object");

        collection.replace(3, newItem);
    }

}



