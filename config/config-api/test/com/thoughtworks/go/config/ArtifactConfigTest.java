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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ArtifactConfigTest {
    @Test
    public void validate_shouldFailIfSourceIsEmpty() {
        ArtifactConfig artifactPlan = new ArtifactConfig(null, "bar");
        artifactPlan.validate(ConfigSaveValidationContext.forChain(new JobConfig("jobname")));
        assertThat(artifactPlan.errors().on(ArtifactConfig.SRC), is("Job 'jobname' has an artifact with an empty source"));
    }

    @Test
    public void validate_shouldFailIfDestDoesNotMatchAFilePattern() {
        ArtifactConfig artifactPlan = new ArtifactConfig("foo/bar", "..");
        artifactPlan.validate(null);
        assertThat(artifactPlan.errors().on(ArtifactConfig.DEST), is("Invalid destination path. Destination path should match the pattern (([.]\\/)?[.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ])"));
    }

    @Test
    public void validate_shouldNotFailWhenDestinationIsNotSet() {
        ArtifactConfig artifactPlan = new ArtifactConfig(null, null);
        artifactPlan.setSource("source");
        artifactPlan.validate(null);
        assertThat(artifactPlan.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldErrorOutWhenDuplicateArtifactConfigsExists() {
        List<Artifact> plans = new ArrayList<>();
        ArtifactConfig existingPlan = new ArtifactConfig("src", "dest");
        plans.add(existingPlan);
        ArtifactConfig artifactPlan = new ArtifactConfig("src", "dest");

        artifactPlan.validateUniqueness(plans);

        assertThat(artifactPlan.errors().isEmpty(), is(false));
        assertThat(artifactPlan.errors().on(ArtifactConfig.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactPlan.errors().on(ArtifactConfig.DEST), is("Duplicate artifacts defined."));
        assertThat(existingPlan.errors().isEmpty(), is(false));
        assertThat(existingPlan.errors().on(ArtifactConfig.SRC), is("Duplicate artifacts defined."));
        assertThat(existingPlan.errors().on(ArtifactConfig.DEST), is("Duplicate artifacts defined."));
    }
}