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
package com.thoughtworks.go.presentation.environment;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;


public class EnvironmentPipelineModelTest {
    @Test
    public void shouldUnderstandWhenAssociatedWithGivenEnvironment() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo", "env");
        assertThat(foo.isAssociatedWithEnvironment("env"), is(true));
        assertThat(foo.isAssociatedWithEnvironment("env2"), is(false));
        assertThat(foo.isAssociatedWithEnvironment(null), is(false));
        foo = new EnvironmentPipelineModel("foo");
        assertThat(foo.isAssociatedWithEnvironment("env"), is(false));
        assertThat(foo.isAssociatedWithEnvironment("env2"), is(false));
        assertThat(foo.isAssociatedWithEnvironment(null), is(false));
    }


    @Test
    public void shouldUnderstandWhenAssiciatedWithADifferentEnvironment() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo", "env");
        assertThat(foo.isAssociatedWithEnvironmentOtherThan("env"), is(false));
        assertThat(foo.isAssociatedWithEnvironmentOtherThan("env2"), is(true));
        assertThat(foo.isAssociatedWithEnvironmentOtherThan(null), is(true));
        foo = new EnvironmentPipelineModel("foo");
        assertThat(foo.isAssociatedWithEnvironmentOtherThan("env"), is(false));
        assertThat(foo.isAssociatedWithEnvironmentOtherThan("env2"), is(false));
        assertThat(foo.isAssociatedWithEnvironmentOtherThan(null), is(false));
    }

    @Test
    public void hasEnvironmentAssociated_shouldReturnTrueWhenAPipelineIsAsscoiatedWithAnEnvironment() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo");
        assertThat(foo.hasEnvironmentAssociated(), is(false));
    }

    @Test
    public void hasEnvironmentAssociated_shouldReturnFalseWhenAPipelineIsNotAsscoiatedWithAnEnvironment() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo","env-name");
        assertThat(foo.hasEnvironmentAssociated(), is(true));
    }

    @Test
    public void shouldSortOnPipelineName() {
        EnvironmentPipelineModel foo = new EnvironmentPipelineModel("foo","env-name");
        EnvironmentPipelineModel bar = new EnvironmentPipelineModel("bar");
        EnvironmentPipelineModel baz = new EnvironmentPipelineModel("baz");

        List<EnvironmentPipelineModel> models = Arrays.asList(foo, bar, baz);
        Collections.sort(models);

        assertThat(models.get(0), is(bar));
        assertThat(models.get(1), is(baz));
        assertThat(models.get(2), is(foo));
    }

    @Test
    public void shouldIgnorecaseWhileSortingOnPipelineName() {
        EnvironmentPipelineModel first = new EnvironmentPipelineModel("first");
        EnvironmentPipelineModel First = new EnvironmentPipelineModel("First","env-name");
        EnvironmentPipelineModel Third = new EnvironmentPipelineModel("Third");
        EnvironmentPipelineModel second = new EnvironmentPipelineModel("second");

        List<EnvironmentPipelineModel> models = Arrays.asList(first, First, Third, second);
        Collections.sort(models);

        assertThat(models.get(0), is(first));
        assertThat(models.get(1), is(First));
        assertThat(models.get(2), is(second));
        assertThat(models.get(3), is(Third));
    }

}
