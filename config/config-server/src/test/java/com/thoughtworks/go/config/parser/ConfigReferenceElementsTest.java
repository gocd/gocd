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
package com.thoughtworks.go.config.parser;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigReferenceElementsTest {

    @Test
    public void shouldShouldAddReferenceElement() {
        ConfigReferenceElements configReferenceElements = new ConfigReferenceElements();
        Object referenceElement = new Object();
        configReferenceElements.add("collection", "id", referenceElement);
        assertThat(configReferenceElements.get("collection", "id"), is(referenceElement));
    }

    @Test
    public void shouldReturnNullReferenceElementWhenCollectionIsMissing(){
        ConfigReferenceElements configReferenceElements = new ConfigReferenceElements();
        assertThat(configReferenceElements.get("missing-collection", "id"), is(nullValue()));
    }

    @Test
    public void shouldReturnNullReferenceElementWhenIdIsMissingInCollection(){
        ConfigReferenceElements configReferenceElements = new ConfigReferenceElements();
        Object referenceElement = new Object();
        configReferenceElements.add("collection", "id", referenceElement);
        assertThat(configReferenceElements.get("collection", "other-id"), is(nullValue()));
    }
}
