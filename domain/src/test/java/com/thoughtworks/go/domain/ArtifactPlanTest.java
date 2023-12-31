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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ArtifactTypeConfigs;
import com.thoughtworks.go.config.BuildArtifactConfig;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.config.TestArtifactConfig;
import com.thoughtworks.go.work.GoPublisher;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ArtifactPlanTest {

    @TempDir
    private Path testFolder;

    @BeforeEach
    public void setUp() throws Exception {
        Files.createDirectories(testFolder.resolve("src"));
    }

    @Test
    public void shouldPublishArtifacts() {
        final GoPublisher publisher = mock(GoPublisher.class);
        final ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");

        artifactPlan.publishBuiltInArtifacts(publisher, testFolder.toFile());
        verify(publisher).upload(testFolder.resolve("src").toFile(), "dest");
    }

    @Test
    public void shouldIgnoreIdAndBuildIdAsPartOfEqualAndHashCodeCheck() {
        final ArtifactPlan installer_1 = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");
        installer_1.setId(100);
        installer_1.setBuildId(1000);

        final ArtifactPlan installer_2 = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");
        installer_2.setId(200);
        installer_2.setBuildId(2000);

        assertThat(installer_1).isEqualTo(installer_2);
    }

    @Test
    public void shouldNormalizePath() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "folder\\src", "folder\\dest");
        assertThat(artifactPlan.getSrc()).isEqualTo("folder/src");
        assertThat(artifactPlan.getDest()).isEqualTo("folder/dest");
    }

    @Test
    public void shouldProvideAppendFilePathToDest() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "test/**/*/a.log", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
            new File("pipelines/pipelineA/test/a/b/a.log"))).isEqualTo("logs/a/b");
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenUsingDoubleStart() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "**/*/a.log", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
            new File("pipelines/pipelineA/test/a/b/a.log"))).isEqualTo("logs/test/a/b");
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenPathProvidedAreSame() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "test/a/b/a.log", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
            new File("pipelines/pipelineA/test/b/a.log"))).isEqualTo("logs");
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenUsingSingleStarToMatchFile() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "test/a/b/*.log", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
            new File("pipelines/pipelineA/test/a/b/a.log"))).isEqualTo("logs");
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenPathMatchingAtTheRoot() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "*.jar", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
            new File("pipelines/pipelineA/a.jar"))).isEqualTo("logs");
    }

    @Test
    public void shouldTrimThePath() {
        assertThat(new ArtifactPlan(ArtifactPlanType.file, "pkg   ", "logs "))
            .isEqualTo(new ArtifactPlan(ArtifactPlanType.file, "pkg", "logs"));
    }

    @Test
    public void toArtifactPlans_shouldConvertArtifactConfigsToArtifactPlanList() {
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("id", "storeId", create("Foo", true, "Bar"));
        final ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs(Arrays.asList(
            new BuildArtifactConfig("source", "destination"),
            new TestArtifactConfig("test-source", "test-destination"),
            artifactConfig
        ));

        final List<ArtifactPlan> artifactPlans = ArtifactPlan.toArtifactPlans(artifactTypeConfigs);

        assertThat(artifactPlans).containsExactlyInAnyOrder(
            new ArtifactPlan(ArtifactPlanType.file, "source", "destination"),
            new ArtifactPlan(ArtifactPlanType.unit, "test-source", "test-destination"),
            new ArtifactPlan(artifactConfig.toJSON())
        );
    }

    @Test
    public void shouldConvertPluggableArtifactConfigToArtifactPlans() {
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("ID", "StoreID", create("Foo", true, "Bar"), create("Baz", false, "Car"));

        final ArtifactPlan artifactPlan = new ArtifactPlan(artifactConfig);

        assertThat(artifactPlan.getArtifactPlanType()).isEqualTo(ArtifactPlanType.external);
        assertThat(artifactPlan.getPluggableArtifactConfiguration())
            .hasSize(3)
            .containsEntry("id", "ID")
            .containsEntry("storeId", "StoreID")
            .hasEntrySatisfying("configuration", configuration -> assertThat(configuration)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .hasSize(2)
                .containsEntry("Foo", "Bar")
                .containsEntry("Baz", "Car"));

    }
}
