/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.presentation.environment;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


public class EnvironmentPipelineModelTest {
    @Test
    public void shouldUnderstandWhenAssociatedWithGivenEnvironment() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo", "env");
        assertThat(foo.isAssociatedWithEnvironment("env")).isTrue();
        assertThat(foo.isAssociatedWithEnvironment("env2")).isFalse();
        assertThat(foo.isAssociatedWithEnvironment(null)).isFalse();
        foo = new EnvironmentPipelineModel("foo");
        assertThat(foo.isAssociatedWithEnvironment("env")).isFalse();
        assertThat(foo.isAssociatedWithEnvironment("env2")).isFalse();
        assertThat(foo.isAssociatedWithEnvironment(null)).isFalse();
    }


    @Test
    public void shouldUnderstandWhenAssociatedWithADifferentEnvironment() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo", "env");
        assertThat(foo.isAssociatedWithEnvironmentOtherThan("env")).isFalse();
        assertThat(foo.isAssociatedWithEnvironmentOtherThan("env2")).isTrue();
        assertThat(foo.isAssociatedWithEnvironmentOtherThan(null)).isTrue();
        foo = new EnvironmentPipelineModel("foo");
        assertThat(foo.isAssociatedWithEnvironmentOtherThan("env")).isFalse();
        assertThat(foo.isAssociatedWithEnvironmentOtherThan("env2")).isFalse();
        assertThat(foo.isAssociatedWithEnvironmentOtherThan(null)).isFalse();
    }

    @Test
    public void hasEnvironmentAssociated_shouldReturnTrueWhenAPipelineIsAssociatedWithAnEnvironment() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo");
        assertThat(foo.hasEnvironmentAssociated()).isFalse();
    }

    @Test
    public void hasEnvironmentAssociated_shouldReturnFalseWhenAPipelineIsNotAssociatedWithAnEnvironment() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo","env-name");
        assertThat(foo.hasEnvironmentAssociated()).isTrue();
    }

    @Test
    public void shouldSortOnPipelineName() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo","env-name");
        EnvironmentPipelineModel bar = new EnvironmentPipelineModel("bar");
        EnvironmentPipelineModel baz = new EnvironmentPipelineModel("baz");

        List<EnvironmentPipelineModel> models = Stream.of(foo, bar, baz).sorted().toList();

        assertThat(models.get(0)).isEqualTo(bar);
        assertThat(models.get(1)).isEqualTo(baz);
        assertThat(models.get(2)).isEqualTo(foo);
    }

    @Test
    public void shouldIgnorecaseWhileSortingOnPipelineName() {
        EnvironmentPipelineModel first = new EnvironmentPipelineModel("first");
        EnvironmentPipelineModel First = new EnvironmentPipelineModel("First","env-name");
        EnvironmentPipelineModel Third = new EnvironmentPipelineModel("Third");
        EnvironmentPipelineModel second = new EnvironmentPipelineModel("second");

        List<EnvironmentPipelineModel> models = Stream.of(first, First, Third, second).sorted().toList();

        assertThat(models.get(0)).isEqualTo(first);
        assertThat(models.get(1)).isEqualTo(First);
        assertThat(models.get(2)).isEqualTo(second);
        assertThat(models.get(3)).isEqualTo(Third);
    }

}
