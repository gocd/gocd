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

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.config.Resources;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

public class ResourcesTest {

    @Test
    public void shouldTrimResourceNames() {

        Resources resources = new Resources();
        resources.add(new Resource("foo"));
        resources.add(new Resource("foo      "));
        assertThat(resources.size(), is(1));

        Resources newResources = new Resources();
        newResources.add(new Resource("foo       "));
        newResources.add(new Resource("foo  "));
        assertThat(newResources.size(), is(1));
    }

    @Test
    public void shouldCompareBasedOnSimilarResourceList() {
        Resources resourcesA = new Resources();
        Resources resourcesB = new Resources();
        resourcesA.add(new Resource("xyz"));
        resourcesA.add(new Resource("aaa"));
        resourcesB.add(new Resource("xyz"));
        resourcesB.add(new Resource("aaa"));
        assertThat(resourcesA.compareTo(resourcesB), is(0));
    }

    @Test
    public void shouldCompareBasedOnResourceListItHas() {
        Resources resourcesA = new Resources();
        Resources resourcesB = new Resources();
        resourcesA.add(new Resource("xyz"));
        resourcesA.add(new Resource("aaa"));
        resourcesB.add(new Resource("xyz"));
        resourcesB.add(new Resource("bbb"));
        assertThat(resourcesA.compareTo(resourcesB), is(lessThan(0)));
        assertThat(resourcesB.compareTo(resourcesA), is(greaterThan(0)));
    }


    @Test
    public void shouldUnderstandLesserLengthResourcesAsLesser() {
        Resources resourcesA = new Resources();
        Resources resourcesB = new Resources();
        resourcesA.add(new Resource("xyz"));
        resourcesB.add(new Resource("xyz"));
        resourcesB.add(new Resource("zzz"));
        assertThat(resourcesA.compareTo(resourcesB), is(lessThan(0)));
        assertThat(resourcesB.compareTo(resourcesA), is(greaterThan(0)));
    }

    @Test
    public void shouldNotBombIfNoResourcesPresent() {
        assertThat(new Resources(new Resource("xyz")).compareTo(new Resources()), is(greaterThan(0)));
    }

    @Test
    public void shouldIgnoreCaseNamesOfResources() {
        Resources resources = new Resources();
        resources.add(new Resource("Eoo"));
        resources.add(new Resource("eoo"));
        assertThat(resources.size(), is(1));
    }

    @Test
    public void shouldGetAllResourcesNames() {
        Resources resources = new Resources();
        resources.add(new Resource("Eoo"));
        resources.add(new Resource("Poo"));
        List<String> names = new ArrayList<String>();
        names.add("Eoo");
        names.add("Poo");
        List<String> resourceNames = resources.resourceNames();
        assertThat(resourceNames, is(names));
    }

    @Test
    public void shouldNotAllowBlankNames() {

        Resources resources = new Resources();
        resources.add(new Resource(""));
        assertThat(resources.size(), is(0));
        resources.add(new Resource("   "));
        assertThat(resources.size(), is(0));

    }

    @Test
    public void shouldNotAddDuplicateResources() {
        Resources expected = new Resources();
        expected.add(new Resource("jdk1.4"));
        expected.add(new Resource("jdk1.5"));

        Resources actual = new Resources();
        actual.add(new Resource("jdk1.4"));
        actual.add(new Resource("jdk1.5"));
        actual.add(new Resource("Jdk1.5"));
        assertThat(expected, is(actual));
    }

    @Test
    public void shouldHaveNiceConvenienceConstructorThatDoesSomeNiftyParsing() {
        Resources actual = new Resources("mou, fou");
        assertThat(actual.toString(), is("fou | mou"));
    }

    @Test
    public void shouldNotBeAbleToAddResourceWithWhiteSpaceAsName() {
        Resources actual = new Resources();
        actual.add(new Resource(" "));
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldReturnSortedResourceNamesAsStringRepresention() {
        Resources actual = new Resources();
        actual.add(new Resource("jdk1.4"));
        actual.add(new Resource("linux"));
        actual.add(new Resource("gentoo"));
        actual.add(new Resource("jdk1.5"));
        actual.add(new Resource("Jdk1.5"));
        assertThat(actual.size(), is(4));
        assertThat(actual.toString(), is("gentoo | jdk1.4 | jdk1.5 | linux"));
    }

    @Test
    public void shouldReturnListOfResoucesAsCommaSeperatedList() {
        Resources actual = new Resources();
        actual.add(new Resource("  a  "));
        actual.add(new Resource("   b"));
        actual.add(new Resource("c"));
        assertThat(actual.exportToCsv(), is("a, b, c, "));
    }

    @Test
    public void shouldClearAndSetPrimitiveAttributes() {
        Resources resources = new Resources();
        String csv = "a, b,   c,d   ";
        resources.add(new Resource("old_resource"));
        assertThat(resources.size(), is(1));
        resources.importFromCsv(csv);
        assertThat(resources.size(), is(4));
        assertThat(resources.exportToCsv(), is("a, b, c, d, "));
    }

    @Test
    public void shouldValidateTree(){
        Resource resource1 = new Resource("a#");
        Resource resource2 = new Resource("b");
        Resources resources = new Resources(resource1, resource2);
        resources.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()));
        assertThat(resource1.errors().size(), is(1));
        assertThat(resource1.errors().firstError(), is(String.format("Resource name 'a#' is not valid. Valid names much match '%s'", Resource.VALID_REGEX)));
        assertThat(resource2.errors().isEmpty(), is(true));
    }
}
