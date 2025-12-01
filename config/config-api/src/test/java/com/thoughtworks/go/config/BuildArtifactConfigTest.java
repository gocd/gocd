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

public class BuildArtifactConfigTest {
    @Test
    public void validate_shouldFailIfSourceIsEmpty() {
        BuildArtifactConfig artifactPlan = new BuildArtifactConfig(null, "bar");
        artifactPlan.validate(ConfigSaveValidationContext.forChain(new JobConfig("jobname")));
        assertThat(artifactPlan.errors().firstErrorOn(BuiltinArtifactConfig.SRC)).isEqualTo("Job 'jobname' has an artifact with an empty source");
    }

    @Test
    public void validate_shouldFailIfDestDoesNotMatchAFilePattern() {
        BuildArtifactConfig artifactPlan = new BuildArtifactConfig("foo/bar", "..");
        artifactPlan.validate(null);
        assertThat(artifactPlan.errors().firstErrorOn(BuiltinArtifactConfig.DEST)).isEqualTo("Invalid destination path. Destination path should match the pattern (([.]\\/)?[.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ])");
    }

    @Test
    public void validate_shouldNotFailWhenDestinationIsNotSet() {
        BuildArtifactConfig artifactPlan = new BuildArtifactConfig(null, null);
        artifactPlan.setSource("source");
        artifactPlan.validate(null);
        assertThat(artifactPlan.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldErrorOutWhenDuplicateArtifactConfigsExists() {
        List<ArtifactTypeConfig> plans = new ArrayList<>();
        BuildArtifactConfig existingPlan = new BuildArtifactConfig("src", "dest");
        plans.add(existingPlan);
        BuildArtifactConfig artifactPlan = new BuildArtifactConfig("src", "dest");

        artifactPlan.validateUniqueness(plans);

        assertThat(artifactPlan.errors().isEmpty()).isFalse();
        assertThat(artifactPlan.errors().firstErrorOn(BuiltinArtifactConfig.SRC)).isEqualTo("Duplicate artifacts defined.");
        assertThat(artifactPlan.errors().firstErrorOn(BuiltinArtifactConfig.DEST)).isEqualTo("Duplicate artifacts defined.");
        assertThat(existingPlan.errors().isEmpty()).isFalse();
        assertThat(existingPlan.errors().firstErrorOn(BuiltinArtifactConfig.SRC)).isEqualTo("Duplicate artifacts defined.");
        assertThat(existingPlan.errors().firstErrorOn(BuiltinArtifactConfig.DEST)).isEqualTo("Duplicate artifacts defined.");
    }

    @Test
    public void validate_shouldNotFailWhenComparingBuildAndTestArtifacts() {
        List<ArtifactTypeConfig> plans = new ArrayList<>();
        TestArtifactConfig testArtifactConfig = new TestArtifactConfig("src", "dest");
        plans.add(testArtifactConfig);
        BuildArtifactConfig buildArtifactConfig = new BuildArtifactConfig("src", "dest");

        buildArtifactConfig.validateUniqueness(plans);

        assertThat(buildArtifactConfig.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldAllowOverridingDefaultArtifactDestination() {
        BuildArtifactConfig artifactConfig = new BuildArtifactConfig("src", "dest");
        assertThat(artifactConfig.getDestination()).isEqualTo("dest");

        TestArtifactConfig testArtifactConfig = new TestArtifactConfig("src", "destination");
        assertThat(testArtifactConfig.getDestination()).isEqualTo("destination");
    }

    @Test
    public void shouldNotOverrideDefaultArtifactDestinationWhenNotSpecified() {
        BuildArtifactConfig artifactConfig = new BuildArtifactConfig("src", null);
        assertThat(artifactConfig.getDestination()).isEqualTo("");

        TestArtifactConfig testArtifactConfig = new TestArtifactConfig("src", null);
        assertThat(testArtifactConfig.getDestination()).isEqualTo("testoutput");
    }
}
