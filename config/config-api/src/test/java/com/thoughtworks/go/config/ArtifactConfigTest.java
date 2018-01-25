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

import com.thoughtworks.go.config.helper.ValidationContextMother;
import com.thoughtworks.go.domain.ArtifactType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArtifactConfigTest {
    @Test
    public void validate_shouldFailIfSourceIsEmpty() {
        ArtifactConfig artifactPlan = new ArtifactConfig(ArtifactType.file, null, "bar");
        artifactPlan.validate(ConfigSaveValidationContext.forChain(new JobConfig("jobname")));
        assertThat(artifactPlan.errors().on(ArtifactConfig.SRC), is("Job 'jobname' has an artifact with an empty source"));
    }

    @Test
    public void validate_shouldFailIfDestDoesNotMatchAFilePattern() {
        ArtifactConfig artifactPlan = new ArtifactConfig(ArtifactType.file, "foo/bar", "..");
        artifactPlan.validate(null);
        assertThat(artifactPlan.errors().on(ArtifactConfig.DEST), is("Invalid destination path. Destination path should match the pattern (([.]\\/)?[.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ])"));
    }

    @Test
    public void validate_shouldNotFailWhenDestinationIsNotSet() {
        ArtifactConfig artifactPlan = new ArtifactConfig(ArtifactType.file, null, null);
        artifactPlan.setSource("source");
        artifactPlan.validate(null);
        assertThat(artifactPlan.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldErrorOutWhenDuplicateArtifactConfigsExists() {
        List<ArtifactConfig> artifactConfigs = new ArrayList<>();
        ArtifactConfig existingArtifactConfig = new ArtifactConfig(ArtifactType.file, "src", "dest");
        artifactConfigs.add(existingArtifactConfig);
        ArtifactConfig artifactPlan = new ArtifactConfig(ArtifactType.file, "src", "dest");

        artifactPlan.validateUniqueness(artifactConfigs);

        assertThat(artifactPlan.errors().isEmpty(), is(false));
        assertThat(artifactPlan.errors().on(ArtifactConfig.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactPlan.errors().on(ArtifactConfig.DEST), is("Duplicate artifacts defined."));
        assertThat(existingArtifactConfig.errors().isEmpty(), is(false));
        assertThat(existingArtifactConfig.errors().on(ArtifactConfig.SRC), is("Duplicate artifacts defined."));
        assertThat(existingArtifactConfig.errors().on(ArtifactConfig.DEST), is("Duplicate artifacts defined."));
    }

    //Pluggable artifact

    @Test
    public void shouldCreatePluggableArtifact() {
        final ArtifactConfig artifactConfig = new ArtifactConfig("Artifact-ID", "Store-ID", create("Foo", false, "Bar"));

        assertThat(artifactConfig.getId(), is("Artifact-ID"));
        assertThat(artifactConfig.getStoreId(), is("Store-ID"));
        assertThat(artifactConfig.getArtifactType(), is(ArtifactType.plugin));
        assertThat(artifactConfig.getArtifactTypeValue(), is("Pluggable Artifact"));
        assertThat(artifactConfig.get(0), is(create("Foo", false, "Bar")));
    }

    @Test
    public void validate_shouldValidatePluggableArtifactStoreId() {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final ArtifactConfig artifactConfig = new ArtifactConfig("Artifact-ID", "Store-ID");
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
    public void validate_shouldValidateArtifactPropertiesConfigurationKeyUniqueness() {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final ArtifactConfig artifactConfig = new ArtifactConfig("Artifact-ID", "Store-ID", create("Foo", false, "Bar"), create("Foo", true, "Bar"));
        final ArtifactStores artifactStores = mock(ArtifactStores.class);
        assertFalse(artifactConfig.hasErrors());

        when(validationContext.artifactStores()).thenReturn(artifactStores);
        when(artifactStores.find("Store-ID")).thenReturn(new ArtifactStore("Store-ID", "pluginId"));

        artifactConfig.validate(validationContext);

        assertThat(artifactConfig.get(0).errors().getAllOn("configurationKey"), is(Arrays.asList("Duplicate key 'Foo' found for Pluggable Artifact")));
        assertThat(artifactConfig.get(1).errors().getAllOn("configurationKey"), is(Arrays.asList("Duplicate key 'Foo' found for Pluggable Artifact")));
    }

    @Test
    public void validate_shouldValidateUniquenessOnId() {
        final ArtifactConfig existingConfig = new ArtifactConfig("Artifact-ID", "Store-ID");
        final List<ArtifactConfig> artifactConfigs = new ArrayList<>();
        artifactConfigs.add(existingConfig);

        final ArtifactConfig newConfig = new ArtifactConfig("Artifact-ID", "Store-ID");
        newConfig.validateUniqueness(artifactConfigs);

        assertTrue(newConfig.hasErrors());
        assertTrue(existingConfig.hasErrors());

        assertThat(newConfig.errors().on("id"), is("Duplicate pluggable artifacts  with id `Artifact-ID` defined."));
        assertThat(existingConfig.errors().on("id"), is("Duplicate pluggable artifacts  with id `Artifact-ID` defined."));
    }

    @Test
    public void validate_shouldValidateArtifactPropertiesConfig() {
        final ArtifactConfig existingConfig = new ArtifactConfig("id1", "Store-ID", create("Foo", false, "Bar"));
        final List<ArtifactConfig> artifactConfigs = new ArrayList<>();
        artifactConfigs.add(existingConfig);

        final ArtifactConfig newConfig = new ArtifactConfig("id2", "Store-ID", create("Foo", false, "Bar"));
        newConfig.validateUniqueness(artifactConfigs);

        assertTrue(newConfig.hasErrors());
        assertTrue(existingConfig.hasErrors());

        assertThat(newConfig.errors().on("id"), is("Duplicate pluggable artifacts  configuration defined."));
        assertThat(existingConfig.errors().on("id"), is("Duplicate pluggable artifacts  configuration defined."));
    }

    @Test
    public void validate_shouldNotErrorWhenArtifactPropertiesConfigurationIsSameForDifferentStores() {
        final ArtifactConfig existingConfig = new ArtifactConfig("id1", "storeId1", create("Foo", false, "Bar"));
        final List<ArtifactConfig> artifactConfigs = new ArrayList<>();
        artifactConfigs.add(existingConfig);

        final ArtifactConfig newConfig = new ArtifactConfig("id2", "storeId2", create("Foo", false, "Bar"));
        newConfig.validateUniqueness(artifactConfigs);

        assertFalse(newConfig.hasErrors());
        assertFalse(existingConfig.hasErrors());

        assertNull(newConfig.errors().on("id"));
        assertNull(existingConfig.errors().on("id"));
    }

    @Test
    public void shouldSerializeToJson() {
        final ArtifactConfig config = new ArtifactConfig("id1", "Store-ID", create("Foo", false, "Bar"));

        final String actual = config.toJSON();

        assertThat(actual, is("{\"configuration\":{\"Foo\":\"Bar\"},\"id\":\"id1\",\"storeId\":\"Store-ID\"}"));
    }

    @Test
    public void validateTree_shouldValidatePresenceOfId() {
        final ArtifactConfig artifactConfig = new ArtifactConfig("", "s3");
        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("s3", "cd.go.s3"));

        final boolean result = artifactConfig.validateTree(ValidationContextMother.validationContext(artifactStores));

        assertFalse(result);
    }

    @Test
    public void validateTree_shouldValidateNullId() {
        ArtifactConfig artifactConfig = new ArtifactConfig(null, "s3");

        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("s3", "cd.go.s3"));

        final boolean result = artifactConfig.validateTree(ValidationContextMother.validationContext(artifactStores));

        assertFalse(result);
    }

    @Test
    public void validateTree_presenceStoreId() {
        ArtifactConfig artifactConfig = new ArtifactConfig("installer", "");

        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("s3", "cd.go.s3"));

        final boolean result = artifactConfig.validateTree(ValidationContextMother.validationContext(artifactStores));

        assertFalse(result);
    }

    @Test
    public void validateTree_presenceOfStoreIdInArtifactStores() {
        ArtifactConfig artifactConfig = new ArtifactConfig("installer", "s3");

        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("docker", "cd.go.docker"));

        final boolean result = artifactConfig.validateTree(ValidationContextMother.validationContext(artifactStores));

        assertFalse(result);
    }
}