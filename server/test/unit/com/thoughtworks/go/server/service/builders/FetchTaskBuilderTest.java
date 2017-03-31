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

package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FetchTaskBuilderTest {
    private static final String LABEL = "1.01";
    private UpstreamPipelineResolver resolver;
    private FetchTaskBuilder fetchTaskBuilder;
    private BuilderFactory builderFactory;

    @Before
    public void setUp() {
        CruiseConfig config = GoConfigMother.configWithPipelines("random_pipeline", "uppest_lookalike", "uppest_stream", "upstreams_peer", "upstream", "downstream", "dummy");

        PipelineConfig randomPipeline = config.pipelineConfigByName(new CaseInsensitiveString("random_pipeline"));
        randomPipeline.add(StageConfigMother.stageConfig("random-stage1", new JobConfigs(new JobConfig("random-job1"))));

        PipelineConfig uppestLookalike = config.pipelineConfigByName(new CaseInsensitiveString("uppest_lookalike"));
        uppestLookalike.add(StageConfigMother.stageConfig("uppest-stage1", new JobConfigs(new JobConfig("uppest-job1"))));

        PipelineConfig uppestStream = config.pipelineConfigByName(new CaseInsensitiveString("uppest_stream"));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage1", new JobConfigs(new JobConfig("uppest-job1"))));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage2", new JobConfigs(new JobConfig("uppest-job2"))));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage3", new JobConfigs(new JobConfig("uppest-job3"))));

        PipelineConfig upstream = config.pipelineConfigByName(new CaseInsensitiveString("upstream"));
        upstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage2")));
        upstream.add(StageConfigMother.stageConfig("up-stage1", new JobConfigs(new JobConfig("up-job1"))));
        upstream.add(StageConfigMother.stageConfig("up-stage2", new JobConfigs(new JobConfig("up-job2"))));

        PipelineConfig downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1")));
        downstream.get(0).getJobs().get(0).addTask(new FetchTask(new CaseInsensitiveString("foo"), new CaseInsensitiveString("bar"), new CaseInsensitiveString("baz"), "abcd", "efg"));

        resolver = mock(UpstreamPipelineResolver.class);
        builderFactory = mock(BuilderFactory.class);
        fetchTaskBuilder = new FetchTaskBuilder();
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldUseNameAndLabelFromPipelineWhenPipelineNameOfFetchTaskIsEmpty() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setSrcfile("a.txt");
        Pipeline pipeline = pipeline(LABEL);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier().getPipelineName(), is("cruise"));
        assertThat(builder.getJobIdentifier().getPipelineLabel(), is(LABEL));
    }

    @Test
    public void shouldUseCorrectStageCounterWhenFetchingFromSamePipelineAndStageThatHasBeenRun() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("linux-firefox"), "", "");
        Pipeline pipeline = pipelineWithStage("mingle", 1, "label-1", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), Matchers.is(new JobIdentifier("mingle", 1, "label-1", "dev", "2", "linux-firefox", 0L)));
    }

    @Test
    public void shouldUseCorrectStageCounterWhenFetchingFromDependencyStage() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("linux-firefox"), "", "");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "label-1", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), is(new JobIdentifier("mingle", 1, "label-1", "dev", "2", "linux-firefox", 0L)));
    }

    @Test
    public void shouldUseLatestStageWhenFetchingFromDifferentStageInDependencyPipeline() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("ft"), new CaseInsensitiveString("linux-firefox"), "", "");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "label-1", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier(), is(new JobIdentifier("mingle", 1, "label-1", "ft", "latest", "linux-firefox", 0L)));
    }

    @Test
    public void shouldNotSupportFetchingArtifactsFromPipelineWhichIsNotADependentMaterial() {
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
    public void shouldFindStageCounterFromDependenciesWhenPipelineNameIsDifferent() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString(""), "", "");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "10", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.getJobIdentifier().getStageCounter(), is("2"));
    }

    @Test
    public void shouldFetchFromCorrectAncestorStageInstance_InCaseOfLinerDependency() {
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
    public void shouldGetTheRightDestAndJobLocatorOnAgent() {
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "label-1", "dev", 2);
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", "dest");

        FetchHandler fetchHandler = fetchTaskBuilder.getHandler(fetchTask, pipeline.getName());
        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(fetchHandler, is(new FileHandler(new File("cruise/dest"), getSrc())));
        assertThat(builder.jobLocatorForDisplay(), is("mingle/label-1/dev/2/one"));
    }

    @Test
    public void shouldUsePipelineCounterWhenFetchingArtifactFromDependentPipeline() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("linux-firefox"), "log.xml", "dest");
        Pipeline pipeline = pipelineWithDepencencyMaterial("cruise", "mingle", 1, "label-1", "dev", 2);

        FetchArtifactBuilder builder = (FetchArtifactBuilder) fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline, resolver);

        assertThat(builder.artifactLocator(), is("mingle/1/dev/2/linux-firefox/log.xml"));
    }

    @Test
    public void describeForSamePipeline() throws Exception {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString(""), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise.zip", "dest\\subfolder");
        fetchTaskBuilder.createBuilder(builderFactory, fetchTask, pipeline(LABEL), resolver);
        assertThat(fetchTask.describe(),
                is("fetch artifact [cruise.zip] => [dest/subfolder] from [cruise/dev/windows-3]"));
    }

    @Test
    public void shouldNormalizeDestOnAgent() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", "dest\\pavan");
        FetchHandler fetchHandler = fetchTaskBuilder.getHandler(fetchTask, "cruise");
        assertThat(fetchHandler, is(new FileHandler(new File("cruise/dest/pavan"), getSrc())));
    }

    @Test
    public void shouldSupportNullForDest() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", null);
        FetchHandler fetchHandler = fetchTaskBuilder.getHandler(fetchTask, "cruise");
        assertThat(fetchHandler, is(new FileHandler(new File("cruise"), getSrc())));
    }

    @Test
    public void shouldUseTheDirectoryHandler() {
        Pipeline pipeline = pipelineWithStage("mingle", 1, LABEL, "dev", 1);
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString(pipeline.getName()), new CaseInsensitiveString(pipeline.getFirstStage().getName()), new CaseInsensitiveString("windows-3"), "", "dest\\subfolder");
        fetchTask.setSrcdir("log");
        FetchHandler actual = fetchTaskBuilder.getHandler(fetchTask, pipeline.getName());
        File folderOnAgent = new File("mingle/dest/subfolder");
        assertThat(actual, is(new DirHandler("log", folderOnAgent)));
    }

    @Test
    public void shouldUseTheFileHandler() {
        Pipeline pipeline = pipelineWithStage("mingle", 1, LABEL, "dev", 1);
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString(pipeline.getName()), new CaseInsensitiveString(pipeline.getFirstStage().getName()), new CaseInsensitiveString("windows-3"), "cruise.zip", "dest\\subfolder");
        FetchHandler actual = fetchTaskBuilder.getHandler(fetchTask, pipeline.getName());
        File folderOnAgent = new File("mingle/dest/subfolder");
        assertThat(actual, is(new FileHandler(new File(folderOnAgent, "cruise.zip"), getSrc())));
    }

    @Test
    public void shouldThrowExceptionWithAppropriateMessageWhenAncestorPipelineChanged() {
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
    public void shouldThrowExceptionWithAppropriateMessageWhenFetchArtifactPipelinePathBroken() {
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
        Pipeline pipeline = PipelineMother.pipeline("cruise", new NullStage("Stage"));
        pipeline.setLabel(label);
        return pipeline;
    }

}
