/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ArtifactPlanTest {
    private File testFolder;
    private File srcFolder;

    @Before
    public void setUp() {
        testFolder = new File("test.com");
        srcFolder = new File(testFolder, "src");
        srcFolder.mkdirs();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(testFolder);
    }

    @Test
    public void shouldPublishArtifacts() {
        final DefaultGoPublisher publisher = mock(DefaultGoPublisher.class);
        final ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");

        artifactPlan.publishBuiltInArtifacts(publisher, testFolder);
        verify(publisher).upload(new File(testFolder, "src"), "dest");
    }

    @Test
    public void shouldIgnoreIdAndBuildIdAsPartOfEqualAndHashCodeCheck() {
        final ArtifactPlan installer_1 = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");
        installer_1.setId(100);
        installer_1.setBuildId(1000);

        final ArtifactPlan installer_2 = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");
        installer_2.setId(200);
        installer_2.setBuildId(2000);

        assertTrue(installer_1.equals(installer_2));
    }

    @Test
    public void shouldNormalizePath() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "folder\\src", "folder\\dest");
        assertThat(artifactPlan.getSrc(), is("folder/src"));
        assertThat(artifactPlan.getDest(), is("folder/dest"));
    }

    @Test
    public void shouldProvideAppendFilePathToDest() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "test/**/*/a.log", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/test/a/b/a.log")), is("logs/a/b"));
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenUsingDoubleStart() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "**/*/a.log", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/test/a/b/a.log")), is("logs/test/a/b"));
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenPathProvidedAreSame() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "test/a/b/a.log", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/test/b/a.log")), is("logs"));
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenUsingSingleStarToMatchFile() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "test/a/b/*.log", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/test/a/b/a.log")), is("logs"));
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenPathMatchingAtTheRoot() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "*.jar", "logs");
        assertThat(artifactPlan.destinationURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/a.jar")), is("logs"));
    }

    @Test
    public void shouldTrimThePath() {
        assertThat(new ArtifactPlan(ArtifactPlanType.file, "pkg   ", "logs "),
                is(new ArtifactPlan(ArtifactPlanType.file, "pkg", "logs")));
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

        assertThat(artifactPlans, containsInAnyOrder(
                new ArtifactPlan(ArtifactPlanType.file, "source", "destination"),
                new ArtifactPlan(ArtifactPlanType.unit, "test-source", "test-destination"),
                new ArtifactPlan(artifactConfig.toJSON())
        ));
    }

    @Test
    public void shouldConvertPluggableArtifactConfigToArtifactPlans() {
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("ID", "StoreID", create("Foo", true, "Bar"), create("Baz", false, "Car"));

        final ArtifactPlan artifactPlan = new ArtifactPlan(artifactConfig);

        assertThat(artifactPlan.getArtifactPlanType(), is(ArtifactPlanType.external));
        assertThat(artifactPlan.getPluggableArtifactConfiguration().size(), is(3));
        assertThat(artifactPlan.getPluggableArtifactConfiguration(), hasEntry("id", "ID"));
        assertThat(artifactPlan.getPluggableArtifactConfiguration(), hasEntry("storeId", "StoreID"));

        final Map<String, String> configuration = (Map<String, String>) artifactPlan.getPluggableArtifactConfiguration().get("configuration");
        assertThat(configuration.size(), is(2));
        assertThat(configuration, hasEntry("Foo", "Bar"));
        assertThat(configuration, hasEntry("Baz", "Car"));
    }
}
