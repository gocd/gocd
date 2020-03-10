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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.config.ResourceConfigs;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourcesTest {
    @Test
    void shouldConvertResourceConfigsToListOfResource() {
        final Resources resources = new Resources(new ResourceConfigs("foo,bar"));

        assertThat(resources).hasSize(2);
        assertThat(resources).containsExactly(new Resource("foo"), new Resource("bar"));
    }

    @Test
    void shouldConvertResourceListToResourceConfigs() {
        final Resources resources = new Resources(new Resource("foo"), new Resource("bar"));

        final ResourceConfigs resourceConfigs = resources.toResourceConfigs();

        assertThat(resourceConfigs.size()).isEqualTo(2);
        assertThat(resourceConfigs.get(0)).isEqualTo(new ResourceConfig("foo"));
        assertThat(resourceConfigs.get(1)).isEqualTo(new ResourceConfig("bar"));
    }

    @Test
    void shouldConvertCommaSeparatedResourcesStringToResources() {
        final Resources resources = new Resources("foo,bar,baz");

        assertThat(resources).hasSize(3);
        assertThat(resources).containsExactly(new Resource("foo"), new Resource("bar"), new Resource("baz"));
    }
}
