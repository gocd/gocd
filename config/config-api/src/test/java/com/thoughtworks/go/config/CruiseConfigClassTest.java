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
package com.thoughtworks.go.config;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CruiseConfigClassTest {

    private ConfigCache configCache = new ConfigCache();

    @Test
    public void shouldFindAllFields() {
        GoConfigClassWriter fooBarClass = new GoConfigClassWriter(FooBar.class, configCache, null);
        assertThat(fooBarClass.getAllFields(new FooBar()).size(), is(3));
    }

    @Test
    public void shouldFindAllFieldsInBaseClass() {
        GoConfigClassWriter fooBarClass = new GoConfigClassWriter(DerivedFooBar.class, configCache, null);
        assertThat(fooBarClass.getAllFields(new DerivedFooBar()).size(), is(4));
    }

}

class FooBar {
    @SuppressWarnings({"PMD.UnusedPrivateField", "unused"}) private String value;
    @SuppressWarnings({"PMD.UnusedPrivateField", "unused"}) private String data;
    @SuppressWarnings({"PMD.UnusedPrivateField", "unused"}) protected String moreData;
}

class DerivedFooBar extends FooBar {
    @SuppressWarnings({"PMD.UnusedPrivateField", "unused"}) protected String derivedClassData;
}
