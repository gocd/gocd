/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.ArtifactType;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluggableArtifactConfigTest {
    @Test
    public void shouldCreatePluggableArtifact() {
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID", create("Foo", false, "Bar"));

        assertThat(artifactConfig.getId(), is("Artifact-ID"));
        assertThat(artifactConfig.getStoreId(), is("Store-ID"));
        assertThat(artifactConfig.getArtifactType(), is(ArtifactType.plugin));
        assertThat(artifactConfig.getArtifactTypeValue(), is("Pluggable Artifact"));
        assertThat(artifactConfig.get(0), is(create("Foo", false, "Bar")));
    }

    @Test
    public void validate_shouldValidatePluggableArtifactStoreId() {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID");
        final ArtifactStores artifactStores = mock(ArtifactStores.class);
        assertFalse(artifactConfig.hasErrors());

        when(validationContext.artifactStores()).thenReturn(artifactStores);
        when(artifactStores.find("Store-ID")).thenReturn(null);

        artifactConfig.validate(validationContext);

        assertTrue(artifactConfig.hasErrors());
        assertThat(artifactConfig.errors().getAll(), hasSize(1));
        assertThat(artifactConfig.errors().getAllOn("storeId"), hasSize(1));
        assertThat(artifactConfig.errors().on("storeId"), is("Artifact store with id `Store-ID` does not exist."));
    }

    @Test
    public void validate_shouldValidateUniquenessOnId() {
        final PluggableArtifactConfig existingConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID");
        final List<Artifact> artifactConfigs = Arrays.asList(existingConfig);

        final PluggableArtifactConfig newConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID");
        newConfig.validateUniqueness(artifactConfigs);

        assertTrue(newConfig.hasErrors());
        assertTrue(existingConfig.hasErrors());

        assertThat(newConfig.errors().on("id"), is("Duplicate pluggable artifacts  with id `Artifact-ID` defined."));
        assertThat(existingConfig.errors().on("id"), is("Duplicate pluggable artifacts  with id `Artifact-ID` defined."));
    }

    @Test
    public void validate_shouldValidateArtifactPropertiesConfig() {
        final PluggableArtifactConfig existingConfig = new PluggableArtifactConfig("id1", "Store-ID", create("Foo", false, "Bar"));
        final List<Artifact> artifactConfigs = Arrays.asList(existingConfig);

        final PluggableArtifactConfig newConfig = new PluggableArtifactConfig("id2", "Store-ID", create("Foo", false, "Bar"));
        newConfig.validateUniqueness(artifactConfigs);

        assertTrue(newConfig.hasErrors());
        assertTrue(existingConfig.hasErrors());

        assertThat(newConfig.errors().on("id"), is("Duplicate pluggable artifacts  configuration defined."));
        assertThat(existingConfig.errors().on("id"), is("Duplicate pluggable artifacts  configuration defined."));
    }

    @Test
    public void shouldSerializeToJson() {
        final PluggableArtifactConfig config = new PluggableArtifactConfig("id1", "Store-ID", create("Foo", false, "Bar"));

        final String actual = config.toJSON();

        assertThat(actual, is("{\"configuration\":{\"Foo\":\"Bar\"},\"id\":\"id1\",\"storeId\":\"Store-ID\"}"));
    }
}