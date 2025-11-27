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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceConfigsTest {

    @Test
    public void shouldTrimResourceNames() {

        ResourceConfigs resourceConfigs = new ResourceConfigs();
        resourceConfigs.add(new ResourceConfig("foo"));
        resourceConfigs.add(new ResourceConfig("foo      "));
        assertThat(resourceConfigs.size()).isEqualTo(1);

        ResourceConfigs newResourceConfigs = new ResourceConfigs();
        newResourceConfigs.add(new ResourceConfig("foo       "));
        newResourceConfigs.add(new ResourceConfig("foo  "));
        assertThat(newResourceConfigs.size()).isEqualTo(1);
    }

    @Test
    public void shouldIgnoreCaseNamesOfResources() {
        ResourceConfigs resourceConfigs = new ResourceConfigs();
        resourceConfigs.add(new ResourceConfig("Eoo"));
        resourceConfigs.add(new ResourceConfig("eoo"));
        assertThat(resourceConfigs.size()).isEqualTo(1);
    }

    @Test
    public void shouldGetAllResourcesNames() {
        ResourceConfigs resourceConfigs = new ResourceConfigs();
        resourceConfigs.add(new ResourceConfig("Eoo"));
        resourceConfigs.add(new ResourceConfig("Poo"));
        List<String> names = new ArrayList<>();
        names.add("Eoo");
        names.add("Poo");
        List<String> resourceNames = resourceConfigs.resourceNames();
        assertThat(resourceNames).isEqualTo(names);
    }

    @Test
    public void shouldNotAllowBlankNames() {

        ResourceConfigs resourceConfigs = new ResourceConfigs();
        resourceConfigs.add(new ResourceConfig(""));
        assertThat(resourceConfigs.size()).isEqualTo(0);
        resourceConfigs.add(new ResourceConfig("   "));
        assertThat(resourceConfigs.size()).isEqualTo(0);

    }

    @Test
    public void shouldNotAddDuplicateResources() {
        ResourceConfigs expected = new ResourceConfigs();
        expected.add(new ResourceConfig("jdk1.4"));
        expected.add(new ResourceConfig("jdk1.5"));

        ResourceConfigs actual = new ResourceConfigs();
        actual.add(new ResourceConfig("jdk1.4"));
        actual.add(new ResourceConfig("jdk1.5"));
        actual.add(new ResourceConfig("Jdk1.5"));
        assertThat((List<? extends ResourceConfig>) expected).isEqualTo(actual);
    }

    @Test
    public void shouldHaveNiceConvenienceConstructorThatDoesSomeNiftyParsing() {
        ResourceConfigs actual = new ResourceConfigs("mou, fou");
        assertThat(actual.toString()).isEqualTo("fou | mou");
    }

    @Test
    public void shouldNotBeAbleToAddResourceWithWhiteSpaceAsName() {
        ResourceConfigs actual = new ResourceConfigs();
        actual.add(new ResourceConfig(" "));
        assertThat(actual.size()).isEqualTo(0);
    }

    @Test
    public void shouldReturnSortedResourceNamesAsStringRepresention() {
        ResourceConfigs actual = new ResourceConfigs();
        actual.add(new ResourceConfig("jdk1.4"));
        actual.add(new ResourceConfig("linux"));
        actual.add(new ResourceConfig("gentoo"));
        actual.add(new ResourceConfig("jdk1.5"));
        actual.add(new ResourceConfig("Jdk1.5"));
        assertThat(actual.size()).isEqualTo(4);
        assertThat(actual.toString()).isEqualTo("gentoo | jdk1.4 | jdk1.5 | linux");
    }

    @Test
    public void shouldClearAndSetPrimitiveAttributes() {
        ResourceConfigs resourceConfigs = new ResourceConfigs();
        String csv = "a, b,   c,d   ";
        resourceConfigs.add(new ResourceConfig("old_resource"));
        assertThat(resourceConfigs.size()).isEqualTo(1);
        resourceConfigs.importFromCsv(csv);
        assertThat(resourceConfigs.size()).isEqualTo(4);
    }

    @Test
    public void shouldValidateTree() {
        ResourceConfig resourceConfig1 = new ResourceConfig("a#");
        ResourceConfig resourceConfig2 = new ResourceConfig("b");
        ResourceConfigs resourceConfigs = new ResourceConfigs(resourceConfig1, resourceConfig2);
        resourceConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()));
        assertThat(resourceConfig1.errors().size()).isEqualTo(1);
        assertThat(resourceConfig1.errors().firstError()).isEqualTo(String.format("Resource name 'a#' is not valid. Valid names much match '%s'", ResourceConfig.VALID_REGEX));
        assertThat(resourceConfig2.errors().isEmpty()).isTrue();
    }
}
