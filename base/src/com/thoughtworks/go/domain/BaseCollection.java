/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;


public class BaseCollection<T> extends ArrayList<T> {
    public BaseCollection(List<T> elements) {
        super(elements);
    }

    public BaseCollection() {
    }

    public BaseCollection(T... items) {
        this(asList(items));
    }

    public T first() {
        if (this.isEmpty()) {
            return null;
        }
        return this.get(0);
    }

    public T last() {
        if (this.isEmpty()) {
            return null;
        }
        return this.get(this.size() - 1);
    }

}
