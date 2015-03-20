/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.thoughtworks.go.domain.BaseCollection;
import org.junit.Test;

public class BaseCollectionTest {

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
}



