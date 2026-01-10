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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.IntStream;


public class BaseCollection<T> extends ArrayList<T> {
    public BaseCollection() {}

    public BaseCollection(Collection<T> elements) {
        super(elements);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public BaseCollection(T... items) {
        this(Arrays.asList(items));
    }

    public void replaceIfNotEmpty(T oldItem, T newItem) {
        if (!isEmpty()) {
            setIfNotEmpty(indexOf(oldItem), newItem);
        }
    }

    protected void replaceIfNotEmpty(Predicate<? super T> filter, T newItem) {
        if (!isEmpty()) {
            setIfNotEmpty(indexOfMatcher(filter), newItem);
        }
    }

    protected int indexOfMatcher(Predicate<? super T> filter) {
        return IntStream.range(0, size())
            .filter(i -> filter.test(get(i)))
            .findFirst()
            .orElse(-1);
    }

    public void setIfNotEmpty(int indexOfOldItem, T newItem) {
        if (isEmpty()) {
            return;
        }

        if (indexOfOldItem < 0 || indexOfOldItem >= size()) {
            throw new IndexOutOfBoundsException(String.format("There is no object at index '%s' in this collection of %s", indexOfOldItem, this.getFirst().getClass().getName()));
        }

        set(indexOfOldItem, newItem);
    }

    /**
     * Similar to {@link #removeIf(Predicate)} but only removes the first matching element. More efficient when
     * you are removing by ID/only expect one match
     */
    public boolean removeFirstIf(@NotNull Predicate<? super T> filter) {
        for (int i = 0, size = size(); i < size; i++) {
            if (filter.test(get(i))) {
                remove(i);
                return true;
            }
        }
        return false;
    }

}
