/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AllConfigErrorsTest {
    @Test
    public void shouldGetAConsolidatedListOfErrorsAsMessage() {
        AllConfigErrors errors = new AllConfigErrors();
        errors.add(error("key1"));
        errors.add(error("key2"));
        errors.add(error("key3"));
        assertThat(errors.asString(), is("error on key1, error on key2, error on key3"));
    }

    private ConfigErrors error(String fieldName) {
        ConfigErrors errors1 = new ConfigErrors();
        errors1.add(fieldName, "error on " + fieldName);
        return errors1;
    }
}