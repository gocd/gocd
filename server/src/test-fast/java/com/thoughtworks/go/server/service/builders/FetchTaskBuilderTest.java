/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.builder.FetchPluggableArtifactBuilder;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Date;

import static com.thoughtworks.go.remote.work.artifact.ArtifactsPublisher.PLUGGABLE_ARTIFACT_METADATA_FOLDER;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FetchTaskBuilderTest {
    private static final String LABEL = "1.01";
    private UpstreamPipelineResolver resolver;
    private FetchTaskBuilder fetchTaskBuilder;
    private BuilderFactory builderFactory;
    private GoConfigService goConfigService;

    @Before
    public void setUp() {
        CruiseConfig config = GoConfigMother.configWithPipelines("random_pipeline", "uppest_lookalike", "uppest_stream", "upstreams_peer", "upstream", "downstream", "dummy", "cruise");

        PipelineConfig randomPipeline = config.pipelineConfigByName(new CaseInsensitiveString("random_pipeline"));
        randomPipeline.add(StageConfigMother.stageConfig("random-stage1", new JobConfigs(new JobConfig("random-job1"))));

        PipelineConfig uppestLookalike = config.pipelineConfigByName(new CaseInsensitiveString("uppest_lookalike"));
        uppestLookalike.add(StageConfigMother.stageConfig("uppest-stage1", new JobConfigs(new JobConfig("uppest-job1"))));

        PipelineConfig uppestStream = config.pipelineConfigByName(new CaseInsensitiveString("uppest_stream"));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage1", new JobConfigs(new JobConfig("uppest-job1"))));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage2", new JobConfigs(new JobConfig("uppest-job2"))));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage3", new JobConfigs(new JobConfig("uppest-job3"))));
        uppestStream.getStage("uppest-stage1").jobConfigByConfigName("uppest-job1").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("installer", "s3"));
        uppestStream.getStage("uppest-stage2").jobConfigByConfigName("uppest-job2").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("installer", "s3"));

        PipelineConfig upstream = config.pipelineConfigByName(new CaseInsensitiveString("upstream"));
        upstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage2")));
        upstream.add(StageConfigMother.stageConfig("up-stage1", new JobConfigs(new JobConfig("up-job1"))));
        upstream.add(StageConfigMother.stageConfig("up-stage2", new JobConfigs(new JobConfig("up-job2"))));

        PipelineConfig downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1")));
        downstream.get(0).getJobs().get(0).addTask(new FetchTask(new CaseInsensitiveString("foo"), new CaseInsensitiveString("bar"), new CaseInsensitiveString("baz"), "abcd", "efg"));

        config.getArtifactStores().add(new ArtifactStore("s3", "cd.go.s3"));

        resolver = mock(UpstreamPipelineResolver.class);
        builderFactory = mock(BuilderFactory.class);
        goConfigService = mock(GoConfigService.class);
        fetchTaskBuilder = new FetchTaskBuilder(goConfigService);

        when(goConfigService.artifactStores()).thenReturn(config.getArtifactStores());
        when(goConfigService.getCurrentConfig()).thenReturn(config);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void FetchTask_shouldUseNameAndLabelFromPipelineWhenPipelineNameOfFetchTaskIsEmpty() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setSrcfile("a.txt");
        Pipeline pipeline = pipeline(LABEL);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier().getPipelineName(), is("cruise"));
        assertThat(builder.getJobIdentifier().getPipelineLabel(), is(LABEL));
    }

    @Test
    public void FetchTask_shouldUseCorrectStageCounterWhenFetchingFromSamePipelineAndStageThatHasBeenRun() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("linux-firefox"), "", "");
        Pipeline pipeline = pipelineWithStage("mingle", 1, "label-1", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), Matchers.is(new JobIdentifier("mingle", 1, "label-1", "dev", "2", "linux-firefox", 0L)));
    }

    @Test
    public void FetchTask_shouldUseCorrectStageCounterWhenFetchingFromDependencyStage() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("linux-firefox"), "", "");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "label-1", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), is(new JobIdentifier("mingle", 1, "label-1", "dev", "2", "linux-firefox", 0L)));
    }

    @Test
    public void FetchTask_shouldUseLatestStageWhenFetchingFromDifferentStageInDependencyPipeline() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("ft"), new CaseInsensitiveString("linux-firefox"), "", "");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "label-1", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), is(new JobIdentifier("mingle", 1, "label-1", "ft", "latest", "linux-firefox", 0L)));
    }

    @Test
    public void FetchTask_shouldNotSupportFetchingArtifactsFromPipelineWhichIsNotADependentMaterial() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("any-pipeline"), new CaseInsensitiveString("ft"), new CaseInsensitiveString("linux-firefox"), "", "");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "10", "dev", 2);

        try {
            fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);
            fail("should not support fetching artifacts from a pipeline which is not dependency material");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline [cruise] tries to fetch artifact from "
                    + "job [any-pipeline/ft/linux-firefox] which is not a dependency material"));
        }

    }

    @Test
    public void FetchTask_shouldFindStageCounterFromDependenciesWhenPipelineNameIsDifferent() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString(""), "", "");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "10", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier().getStageCounter(), is("2"));
    }

    @Test
    public void FetchTask_shouldFetchFromCorrectAncestorStageInstance_InCaseOfLinerDependency() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("uppest/up/down"), new CaseInsensitiveString("uppest-stage"), new CaseInsensitiveString("uppest-job"), "src", "dest");
        Pipeline pipeline = pipelineWithDepencencyMaterial("downest", "down", 1, "down-1", "down-stage", 2);

        DependencyMaterialRevision revisionOfDown = DependencyMaterialRevision.create("down/1/down-stage/2", "down-1");
        when(resolver.buildCauseFor(revisionOfDown.getPipelineName(), revisionOfDown.getPipelineCounter())).thenReturn(pipelineWithDepencencyMaterial("down", "up", 5, "up-5", "up-stage", 3).getBuildCause());

        DependencyMaterialRevision revisionOfUp = DependencyMaterialRevision.create("up/5/up-stage/3", "up-5");
        when(resolver.buildCauseFor(revisionOfUp.getPipelineName(), revisionOfUp.getPipelineCounter())).thenReturn(pipelineWithDepencencyMaterial("up", "uppest", 3, "uppest-3", "uppest-stage", 4).getBuildCause());

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        verify(resolver).buildCauseFor(revisionOfDown.getPipelineName(), revisionOfDown.getPipelineCounter());
        verify(resolver).buildCauseFor(revisionOfUp.getPipelineName(), revisionOfUp.getPipelineCounter());

        assertThat(builder.getJobIdentifier(), is(new JobIdentifier("uppest", 3, "uppest-3", "uppest-stage", "4", "uppest-job", 0l)));
    }

    @Test
    public void FetchTask_shouldGetTheRightDestAndJobLocatorOnAgent() {
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "label-1", "dev", 2);
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", "dest");

        FetchHandler fetchHandler = fetchTaskBuilder.getHandler(fetchTask, pipeline.getName());
        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(fetchHandler, is(new FileHandler(new File("pipelines/cruise/dest"), getSrc())));
        assertThat(builder.jobLocatorForDisplay(), is("mingle/label-1/dev/2/one"));
    }

    @Test
    public void FetchTask_shouldUsePipelineCounterWhenFetchingArtifactFromDependentPipeline() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("linux-firefox"), "log.xml", "dest");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "label-1", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.artifactLocator(), is("mingle/1/dev/2/linux-firefox/log.xml"));
    }

    @Test
    public void FetchTask_describeForSamePipeline() throws Exception {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString(""), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise.zip", "dest\\subfolder");
        fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline(LABEL), resolver);
        assertThat(fetchTask.describe(),
                is("fetch artifact [cruise.zip] => [dest/subfolder] from [cruise/dev/windows-3]"));
    }

    @Test
    public void FetchTask_shouldNormalizeDestOnAgent() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", "dest\\pavan");
        FetchHandler fetchHandler = fetchTaskBuilder.getHandler(fetchTask, "cruise");
        assertThat(fetchHandler, is(new FileHandler(new File("pipelines/cruise/dest/pavan"), getSrc())));
    }

    @Test
    public void FetchTask_shouldSupportNullForDest() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", null);
        FetchHandler fetchHandler = fetchTaskBuilder.getHandler(fetchTask, "cruise");
        assertThat(fetchHandler, is(new FileHandler(new File("pipelines/cruise"), getSrc())));
    }

    @Test
    public void FetchTask_shouldUseTheDirectoryHandler() {
        Pipeline pipeline = pipelineWithStage("mingle", 1, LABEL, "dev", 1);
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString(pipeline.getName()), new CaseInsensitiveString(pipeline.getFirstStage().getName()), new CaseInsensitiveString("windows-3"), "", "dest\\subfolder");
        fetchTask.setSrcdir("log");
        FetchHandler actual = fetchTaskBuilder.getHandler(fetchTask, pipeline.getName());
        File folderOnAgent = new File("pipelines/mingle/dest/subfolder");
        assertThat(actual, is(new DirHandler("log", folderOnAgent)));
    }

    @Test
    public void FetchTask_shouldUseTheFileHandler() {
        Pipeline pipeline = pipelineWithStage("mingle", 1, LABEL, "dev", 1);
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString(pipeline.getName()), new CaseInsensitiveString(pipeline.getFirstStage().getName()), new CaseInsensitiveString("windows-3"), "cruise.zip", "dest\\subfolder");
        FetchHandler actual = fetchTaskBuilder.getHandler(fetchTask, pipeline.getName());
        File folderOnAgent = new File("pipelines/mingle/dest/subfolder");
        assertThat(actual, is(new FileHandler(new File(folderOnAgent, "cruise.zip"), getSrc())));
    }

    @Test
    public void FetchTask_shouldThrowExceptionWithAppropriateMessageWhenAncestorPipelineChanged() {
        Pipeline downInstance = pipelineWithDepencencyMaterial("down", "up3", 1, "up3-label", "up3-stage", 1);
        Pipeline up3Instance = pipelineWithDepencencyMaterial("up3", "up2", 1, "up2-label", "up2-stage", 1);
        Pipeline up2Instance = pipelineWithDepencencyMaterial("up2", "old", 1, "old-label", "old-stage", 1);

        when(resolver.buildCauseFor("up3", 1)).thenReturn(up3Instance.getBuildCause());
        when(resolver.buildCauseFor("up2", 1)).thenReturn(up2Instance.getBuildCause());

        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("up1/up2/up3"), new CaseInsensitiveString("up3-stage"), new CaseInsensitiveString("up3-job"), "src", "dest");
        try {

            fetchTaskBuilder.createBuilder(builderFactory, fetchTask, downInstance, resolver);
            fail("should have failed");
        } catch (Exception e) {
            assertEquals(String.format("Pipeline [down] could not fetch artifact [%s]. Unable to resolve revision for [up1] from build cause", fetchTask), e.getMessage());
        } finally {
            verify(resolver).buildCauseFor("up3", 1);
            verify(resolver).buildCauseFor("up2", 1);
        }
    }

    @Test
    public void FetchTask_shouldThrowExceptionWithAppropriateMessageWhenFetchArtifactPipelinePathBroken() {
        Pipeline downInstance = pipelineWithDepencencyMaterial("down", "up3", 1, "up3-label", "up3-stage", 1);
        Pipeline up3Instance = pipelineWithDepencencyMaterial("up3", "old", 1, "old-label", "old-stage", 1);

        when(resolver.buildCauseFor("up3", 1)).thenReturn(up3Instance.getBuildCause());

        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("up1/up2/up3"), new CaseInsensitiveString("up3-stage"), new CaseInsensitiveString("up3-job"), "src", "dest");
        try {

            fetchTaskBuilder.createBuilder(builderFactory, fetchTask, downInstance, resolver);
            fail("should have failed");
        } catch (Exception e) {
            assertEquals(String.format("Pipeline [down] could not fetch artifact [%s]. Unable to resolve revision for [up2] from build cause", fetchTask), e.getMessage());
        } finally {
            verify(resolver).buildCauseFor("up3", 1);
        }
    }

    @Test
    public void FetchPluggableArtifactTask_shouldUseNameAndLabelFromPipelineWhenPipelineNameOfTaskIsEmpty() {
        final PipelineConfig cruise = goConfigService.getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString("cruise"));
        cruise.getStage("stage").jobConfigByConfigName("job").artifactTypeConfigs().add(new PluggableArtifactConfig("installer", "s3"));

        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(null, new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "installer");
        Pipeline pipeline = pipeline(LABEL);

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier().getPipelineName(), is("cruise"));
        assertThat(builder.getJobIdentifier().getPipelineLabel(), is(LABEL));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldUseCorrectStageCounterWhenFetchingFromSamePipelineAndStageThatHasBeenRun() {
        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "installer");
        Pipeline pipeline = pipelineWithStage("uppest_stream", 1, "label-1", "uppest-stage1", 2);

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), Matchers.is(new JobIdentifier("uppest_stream", 1, "label-1", "uppest-stage1", "2", "uppest-job1", 0L)));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldUseCorrectStageCounterWhenFetchingFromDependencyStage() {
        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "installer");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "uppest_stream", 1, "label-1", "uppest-stage1", 2);

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), is(new JobIdentifier("uppest_stream", 1, "label-1", "uppest-stage1", "2", "uppest-job1", 0L)));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldUseLatestStageWhenFetchingFromDifferentStageInDependencyPipeline() {
        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "installer");

        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "uppest_stream", 1, "label-1", "uppest-stage1", 2);

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), is(new JobIdentifier("uppest_stream", 1, "label-1", "uppest-stage2", "latest", "uppest-job2", 0L)));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldNotSupportFetchingArtifactsFromPipelineWhichIsNotADependentMaterial() {
        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("any-pipeline"), new CaseInsensitiveString("ft"), new CaseInsensitiveString("linux-firefox"), "s3");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "10", "dev", 2);

        try {
            fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);
            fail("should not support fetching artifacts from a pipeline which is not dependency material");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline [cruise] tries to fetch artifact from "
                    + "job [any-pipeline/ft/linux-firefox] which is not a dependency material"));
        }

    }

    @Test
    public void FetchPluggableArtifactTask_shouldFindStageCounterFromDependenciesWhenPipelineNameIsDifferent() {
        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "installer");

        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "uppest_stream", 1, "label-1", "uppest-stage1", 2);

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier().getStageCounter(), is("2"));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldFetchFromCorrectAncestorStageInstance_InCaseOfLinerDependency() {
        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream/downstream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "installer");
        Pipeline pipeline = pipelineWithDepencencyMaterial("downest", "downstream", 1, "down-1", "stage", 2);

        DependencyMaterialRevision revisionOfDown = DependencyMaterialRevision.create("downstream/1/stage/2", "down-1");
        when(resolver.buildCauseFor(revisionOfDown.getPipelineName(), revisionOfDown.getPipelineCounter())).thenReturn(pipelineWithDepencencyMaterial("downstream", "upstream", 5, "up-5", "stage", 3).getBuildCause());

        DependencyMaterialRevision revisionOfUp = DependencyMaterialRevision.create("upstream/5/stage/3", "up-5");
        when(resolver.buildCauseFor(revisionOfUp.getPipelineName(), revisionOfUp.getPipelineCounter())).thenReturn(pipelineWithDepencencyMaterial("upstream", "uppest_stream", 3, "uppest-3", "uppest-stage1", 4).getBuildCause());

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        verify(resolver).buildCauseFor(revisionOfDown.getPipelineName(), revisionOfDown.getPipelineCounter());
        verify(resolver).buildCauseFor(revisionOfUp.getPipelineName(), revisionOfUp.getPipelineCounter());

        assertThat(builder.getJobIdentifier(), is(new JobIdentifier("uppest_stream", 3, "uppest-3", "uppest-stage1", "4", "uppest-job1", 0l)));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldGetTheRightDestAndJobLocatorOnAgent() {
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "uppest_stream", 1, "10", "uppest_stage1", 2);

        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "installer");

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier().buildLocatorForDisplay(), is("uppest_stream/10/uppest-stage1/latest/uppest-job1"));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldUsePipelineCounterWhenFetchingArtifactFromDependentPipeline() {
        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "installer");

        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "uppest_stream", 1, "label-1", "uppest-stage1", 2);

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.metadataFileLocator(), is("uppest_stream/1/uppest-stage1/2/uppest-job1/cd.go.s3.json"));
    }

    @Test
    public void FetchPluggableArtifactTask_describeForSamePipeline() {
        final PipelineConfig cruise = goConfigService.getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString("cruise"));
        cruise.getStage("stage").jobConfigByConfigName("job").artifactTypeConfigs().add(new PluggableArtifactConfig("installer", "s3"));

        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString(""), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "installer");
        fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline(LABEL), resolver);
        assertThat(fetchTask.describe(),
                is("fetch pluggable artifact using [installer] from [cruise/stage/job]"));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldNormalizeDestOnAgent() {
        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "installer");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "uppest_stream", 1, "label-1", "uppest-stage1", 2);

        FetchPluggableArtifactBuilder builder = (FetchPluggableArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        final FileHandler expectedHandler = new FileHandler(new File("pipelines/cruise/cd.go.s3.json"), PLUGGABLE_ARTIFACT_METADATA_FOLDER + "/cd.go.s3.json");

        assertThat(builder.getHandler(), is(expectedHandler));
    }

    @Test
    public void FetchPluggableArtifactTask_shouldThrowExceptionWithAppropriateMessageWhenAncestorPipelineChanged() {
        Pipeline downInstance = pipelineWithDepencencyMaterial("down", "up3", 1, "up3-label", "up3-stage", 1);
        Pipeline up3Instance = pipelineWithDepencencyMaterial("up3", "up2", 1, "up2-label", "up2-stage", 1);
        Pipeline up2Instance = pipelineWithDepencencyMaterial("up2", "old", 1, "old-label", "old-stage", 1);

        when(resolver.buildCauseFor("up3", 1)).thenReturn(up3Instance.getBuildCause());
        when(resolver.buildCauseFor("up2", 1)).thenReturn(up2Instance.getBuildCause());

        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("up1/up2/up3"), new CaseInsensitiveString("up3-stage"), new CaseInsensitiveString("up3-job"), "s3");
        try {

            fetchTaskBuilder.createBuilder(builderFactory, fetchTask, downInstance, resolver);
            fail("should have failed");
        } catch (Exception e) {
            assertEquals(String.format("Pipeline [down] could not fetch artifact [%s]. Unable to resolve revision for [up1] from build cause", fetchTask), e.getMessage());
        } finally {
            verify(resolver).buildCauseFor("up3", 1);
            verify(resolver).buildCauseFor("up2", 1);
        }
    }

    @Test
    public void FetchPluggableArtifactTask_shouldThrowExceptionWithAppropriateMessageWhenFetchArtifactPipelinePathBroken() {
        Pipeline downInstance = pipelineWithDepencencyMaterial("down", "up3", 1, "up3-label", "up3-stage", 1);
        Pipeline up3Instance = pipelineWithDepencencyMaterial("up3", "old", 1, "old-label", "old-stage", 1);

        when(resolver.buildCauseFor("up3", 1)).thenReturn(up3Instance.getBuildCause());

        FetchPluggableArtifactTask fetchTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("up1/up2/up3"), new CaseInsensitiveString("up3-stage"), new CaseInsensitiveString("up3-job"), "s3");
        try {

            fetchTaskBuilder.createBuilder(builderFactory, fetchTask, downInstance, resolver);
            fail("should have failed");
        } catch (Exception e) {
            assertEquals(String.format("Pipeline [down] could not fetch artifact [%s]. Unable to resolve revision for [up2] from build cause", fetchTask), e.getMessage());
        } finally {
            verify(resolver).buildCauseFor("up3", 1);
        }
    }

    private Pipeline pipelineWithStage(String pipelineName, int pipelineCounter, String label, String stagename, int stageCounter) {
        Stage stage = StageMother.custom(stagename);
        stage.setCounter(stageCounter);
        Pipeline pipeline = PipelineMother.pipeline(pipelineName, stage);
        pipeline.setCounter(pipelineCounter);
        pipeline.setLabel(label);
        return pipeline;
    }

    private Pipeline pipelineWithDepencencyMaterial(String currentPipeline, String upstreamPipelineName,
                                                    int upstreamPipelineCounter, String upstreamPipelineLabel,
                                                    String upstreamStageName,
                                                    int upstreamStageCounter) {
        Pipeline pipeline = PipelineMother.pipeline(currentPipeline, new NullStage("Stage"));
        pipeline.setBuildCause(
                buildCauseWithDependencyMaterial(upstreamPipelineName, upstreamPipelineCounter, upstreamPipelineLabel,
                        upstreamStageName, upstreamStageCounter));
        return pipeline;
    }

    private BuildCause buildCauseWithDependencyMaterial(String upstreamPipelineName,
                                                        int upstreamPipelineCounter,
                                                        String upstreamPipelineLabel,
                                                        String upstreamStageName,
                                                        int upstreamStageCounter) {
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        MaterialRevisions materialRevisions = new MaterialRevisions();
        DependencyMaterialRevision materialRevision = DependencyMaterialRevision.create(upstreamPipelineName,
                upstreamPipelineCounter, upstreamPipelineLabel, upstreamStageName, upstreamStageCounter);

        MaterialRevision withRevision = materialRevision.convert(new DependencyMaterial(
                new CaseInsensitiveString(upstreamPipelineName), new CaseInsensitiveString(upstreamStageName)), new Date());
        materialRevisions.addRevision(withRevision);
        buildCause.setMaterialRevisions(materialRevisions);
        return buildCause;
    }

    private String getSrc() {
        return "";
    }

    private Pipeline pipeline(String label) {
        Pipeline pipeline = PipelineMother.pipeline("cruise", new NullStage("stage"));
        pipeline.setLabel(label);
        return pipeline;
    }

}
