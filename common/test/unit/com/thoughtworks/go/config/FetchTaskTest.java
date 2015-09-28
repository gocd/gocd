/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.io.File;
import java.util.Date;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.ReflectionUtil;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class FetchTaskTest {
    private static final String LABEL = "1.01";
    private PipelineConfig downstream;
    private PipelineConfig upstream;
    private PipelineConfig uppestStream;
    private PipelineConfig randomPipeline;
    private PipelineConfig uppestLookalike;
    private CruiseConfig config;
    private UpstreamPipelineResolver resolver;

    @Before
    public void setUp() {
        config = GoConfigMother.configWithPipelines("random_pipeline", "uppest_lookalike", "uppest_stream", "upstreams_peer", "upstream", "downstream", "dummy");

        randomPipeline = config.pipelineConfigByName(new CaseInsensitiveString("random_pipeline"));
        randomPipeline.add(StageConfigMother.stageConfig("random-stage1", new JobConfigs(new JobConfig("random-job1"))));

        uppestLookalike = config.pipelineConfigByName(new CaseInsensitiveString("uppest_lookalike"));
        uppestLookalike.add(StageConfigMother.stageConfig("uppest-stage1", new JobConfigs(new JobConfig("uppest-job1"))));

        uppestStream = config.pipelineConfigByName(new CaseInsensitiveString("uppest_stream"));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage1", new JobConfigs(new JobConfig("uppest-job1"))));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage2", new JobConfigs(new JobConfig("uppest-job2"))));
        uppestStream.add(StageConfigMother.stageConfig("uppest-stage3", new JobConfigs(new JobConfig("uppest-job3"))));

        upstream = config.pipelineConfigByName(new CaseInsensitiveString("upstream"));
        upstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage2")));
        upstream.add(StageConfigMother.stageConfig("up-stage1", new JobConfigs(new JobConfig("up-job1"))));
        upstream.add(StageConfigMother.stageConfig("up-stage2", new JobConfigs(new JobConfig("up-job2"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1")));
        downstream.get(0).getJobs().get(0).addTask(new FetchTask(new CaseInsensitiveString("foo"), new CaseInsensitiveString("bar"), new CaseInsensitiveString("baz"), "abcd", "efg"));

        resolver = mock(UpstreamPipelineResolver.class);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }


    @Test
    public void validate_shouldErrorWhenReferencingConfigRepositoryPipelineFromFilePipeline() {
        uppestStream.setOrigin(new RepoConfigOrigin());
        downstream.setOrigin(new FileConfigOrigin());

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.ORIGIN),startsWith("Pipeline \"downstream\" tries to fetch artifact from job \"uppest_stream :: uppest-stage2 :: uppest-job2\" which is defined in"));

    }
    @Test
    public void validate_shouldNotErrorWhenReferencingFilePipelineFromConfigRepositoryPipeline() {
        uppestStream.setOrigin(new FileConfigOrigin());
        downstream.setOrigin(new RepoConfigOrigin());

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty(), is(true));
    }
    @Test
    public void validate_shouldNotErrorWhenReferencingConfigRepositoryPipelineFromConfigRepositoryPipeline() {
        uppestStream.setOrigin(new RepoConfigOrigin());
        downstream.setOrigin(new RepoConfigOrigin());

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty(), is(true));
    }
    @Test
    public void validate_shouldNotErrorWhenReferencingFilePipelineFromFilePipeline() {
        uppestStream.setOrigin(new FileConfigOrigin());
        downstream.setOrigin(new FileConfigOrigin());

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldPassValidationWhenSrcAndDestDirectoryAreInsideAgentSandbox() {
        StageConfig stage = upstream.getFirstStageConfig();
        JobConfig job = stage.getJobs().get(0);
        FetchTask task = new FetchTask(upstream.name(), stage.name(), job.name(), "src", "dest");
        task.validate(ValidationContext.forChain(upstream, stage, job));

        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldFailValidationWhenSrcDirectoryIsOutsideAgentSandbox() {
        validateAndAssertIncorrectPath("/src", true, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("../src", true, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("..", true, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("first-level/../../../going-back", true, "dest", FetchTask.SRC);
    }

    @Test
    public void shouldFailValidationWhenSrcFileIsOutsideAgentSandbox() {
        validateAndAssertIncorrectPath("/src/junk.txt", false, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("../junk.txt", false, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("first-level/../../../going-back/file.txt", false, "dest", FetchTask.SRC);
    }

    @Test
    public void shouldFailValidationWhenDestDirectoryIsOutsideAgentSandbox() {
        validateAndAssertIncorrectPath("src", false, "/dest", FetchTask.DEST);
        validateAndAssertIncorrectPath("src", false, "../dest", FetchTask.DEST);
        validateAndAssertIncorrectPath("src", false, "..", FetchTask.DEST);
        validateAndAssertIncorrectPath("src", false, "first-level/../../../going-back", FetchTask.DEST);
    }

    @Test
    public void validate_withinTemplates_shouldPopulateErrorOnSrcFileOrSrcDirOrDestIfIsNotAValidFilePathPattern() {
        String dest = "..";
        String src = "..";

        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template-1");
        StageConfig stage = template.get(0);
        JobConfig job = stage.getJobs().get(0);
        FetchTask task = new FetchTask(template.name(), stage.name(), job.name(), src, dest);
        task.validate(ValidationContext.forChain(template, stage, job));

        assertThat(task.errors().isEmpty(), is(false));
        String messageForSrc = String.format("Task of job '%s' in stage '%s' of template '%s' has path '%s' which is outside the working directory.", job.name(), stage.name(), template.name(), src);
        assertThat(task.errors().on(FetchTask.SRC), is(messageForSrc));

        String messageForDest = String.format("Task of job '%s' in stage '%s' of template '%s' has path '%s' which is outside the working directory.", job.name(), stage.name(), template.name(), dest);
        assertThat(task.errors().on(FetchTask.DEST), is(messageForDest));
    }

    private void validateAndAssertIncorrectPath(String source, boolean isSourceDir, String destination, String propertyName) {
        StageConfig stage = upstream.getFirstStageConfig();
        JobConfig job = stage.getJobs().get(0);
        FetchTask task = new FetchTask(upstream.name(), stage.name(), job.name(), null, destination);
        if (isSourceDir) {
            task.setSrcdir(source);
        } else {
            task.setSrcfile(source);
        }
        task.validate(ValidationContext.forChain(upstream, stage, job));

        assertThat(task.errors().isEmpty(), is(false));
        String path = propertyName.equals(FetchTask.SRC) ? source : destination;
        String message = String.format("Task of job '%s' in stage '%s' of pipeline '%s' has path '%s' which is outside the working directory.", job.name(), stage.name(), upstream.name(), path);
        assertThat(task.errors().on(propertyName), is(message));
    }

    @Test
    public void validate_shouldPopulateErrorOnSrcFileOrSrcDirOrDestIfIsNotAValidFilePathPattern() {
        FetchTask task = new FetchTask(new CaseInsensitiveString(""), new CaseInsensitiveString(""), new CaseInsensitiveString(""), "", "");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(
                new CaseInsensitiveString("stage")).getJobs().get(0)));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.STAGE), is("Stage is a required field."));
        assertThat(task.errors().on(FetchTask.SRC), is("Should provide either srcdir or srcfile"));
    }

    @Test
    public void validate_shouldNotTryAndValidateWhenWithinTemplate() throws Exception {
        FetchTask task = new FetchTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new TemplatesConfig(), downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), Is.is(true));
    }

    @Test
    public void validate_shouldValidateBlankStageAndJobWhenWithinTemplate() throws Exception {
        FetchTask task = new FetchTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString(""), new CaseInsensitiveString(""), "src", "dest");
        task.validate(ValidationContext.forChain(config, new TemplatesConfig(), downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), Is.is(false));
        assertThat(task.errors().on(FetchTask.STAGE), is("Stage is a required field."));
        assertThat(task.errors().on(FetchTask.JOB), is("Job is a required field."));
    }

    @Test
    public void shouldPopulateErrorsIfFetchArtifactFromPipelineThatIsNotDependency() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME), is("Pipeline \"downstream\" tries to fetch artifact from pipeline "
                + "\"dummy\" which is not an upstream pipeline"));
    }

    @Test
    public void shouldPopulateErrorsIfFetchArtifactDoesNotHaveStageAndOrJobDefined() {
        FetchTask task = new FetchTask(new CaseInsensitiveString(""), new CaseInsensitiveString(""), new CaseInsensitiveString(""), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, new StageConfig(), new JobConfig()));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.STAGE), is("Stage is a required field."));
        assertThat(task.errors().on(FetchTask.JOB), is("Job is a required field."));
    }

    @Test
    public void shouldBeValidWhenFetchArtifactIsFromAnyAncestorStage_onTheUpstreamPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldBeValidWhenFetchArtifactIsFromAnyAncestorStage_s_predecessorStage__onTheUpstreamPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void should_NOT_BeValidWhenFetchArtifactIsFromAnyAncestorStage_s_successorStage_onTheUpstreamPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage3"), new CaseInsensitiveString("uppest-job3"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.STAGE), is("Pipeline \"downstream\" tries to fetch artifact from stage \"uppest_stream :: uppest-stage3\" which does not complete before \"downstream\" pipeline's dependencies."));
    }

    @Test
    public void should_NOT_BeValidWhen_pathFromAncestor_isInvalid_becauseRefferedPipelineIsNotAnAncestor() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("random_pipeline/upstream"), new CaseInsensitiveString("random-stage1"), new CaseInsensitiveString("random-job1"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME), is("Pipeline named 'random_pipeline' exists, but is not an ancestor of 'downstream' as declared in 'random_pipeline/upstream'."));
    }

    @Test
    public void should_NOT_BeValidWhen_NO_pathFromAncestorIsGiven_butAncestorPipelineIsBeingFetchedFrom() {
        FetchTask task = new FetchTask(null, new CaseInsensitiveString("uppest-stage3"), new CaseInsensitiveString("uppest-job3"), "src", "dest");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().get(0)));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.STAGE), is("Pipeline \"downstream\" tries to fetch artifact from stage \"downstream :: uppest-stage3\" which does not exist. It is used in stage \"stage\" inside job \"job\"."));
    }

    @Test
    public void should_BeValidWhen_hasAnAlternatePathToAncestor() {
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(true));

        task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void should_NOT_BeValidWhen_ImmediateParentDeclaredInPathFromAncestor_isNotAParentPipeline() {
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream/uppest_stream"), new CaseInsensitiveString("up-stage1"), new CaseInsensitiveString("up-job1"), "src", "dest");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().get(0)));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME), is("Pipeline named 'uppest_stream' exists, but is not an ancestor of 'downstream' as declared in 'upstream/uppest_stream'."));
    }

    @Test
    public void should_NOT_BeValidWhen_stageMayNotHaveRunViaTheGivenPath_evenThoughItMayHaveActuallyRunAccordingToAnAlternatePath() {//TODO: Please fix this if someone cares about this corner case working -jj
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(true));

        task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.STAGE), is("Pipeline \"downstream\" tries to fetch artifact from stage \"uppest_stream :: uppest-stage2\" which does not complete before \"downstream\" pipeline's dependencies."));
    }

    @Test
    public void shouldFailWhenFetchArtifactIsFromAnyStage_AFTER_theDependencyStageOnTheUpstreamPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("up-stage2"), new CaseInsensitiveString("up-job2"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.STAGE), is("Pipeline \"downstream\" tries to fetch artifact from stage \"upstream :: up-stage2\" which does not complete before \"downstream\" pipeline's dependencies."));
    }

    @Test
    public void shouldPopulateErrorIfFetchArtifactFromDependentPipelineButStageDoesNotExist() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage-does-not-exist"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(
                new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().get(0)));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.STAGE), is("Pipeline \"downstream\" tries to fetch artifact from stage "
                + "\"upstream :: stage-does-not-exist\" which does not exist. It is used in stage \"stage\" inside job \"job\"."));
    }

    @Test
    public void shouldPopulateErrorIfFetchArtifactFromDependentPipelineButJobNotExist() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job-does-not-exist"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.JOB), is("Pipeline \"downstream\" tries to fetch artifact from job "
                + "\"upstream :: stage :: job-does-not-exist\" which does not exist."));
    }

    @Test
    public void shouldBeValidIfFetchArtifactUsingADependantPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("up-stage1"), new CaseInsensitiveString("up-job1"), "src", "dest");
        task.validate(ValidationContext.forChain(config, downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldBeValidIfFetchArtifactUsingAStageBeforeCurrentInTheSamePipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("up-stage1"))));
        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldBeValidIfFetchArtifactDoesNotSpecifyPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("up-stage1"))));
        assertThat(task.errors().isEmpty(), is(true));
    }

    // The Fetch Task is now case insensitive to job names . This behavior has changed from before migration of validations
    // to inside Config Objects for clicky admin. It was earlier Case SENSITIVE. This was done to address #4970
    // -Jake
    @Test
    public void shouldPopulateErrorsIfFetchArtifactUsingJobNameWithDifferentCase() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("JOB"), "src", "dest");
        task.validate(ValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldPopulateErrorIfSrcFileAndSrcDirBothAreDefined() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src_file", "dest");
        task.setSrcdir("src_dir");
        task.validate(ValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.SRC), is("Only one of srcfile or srcdir is allowed at a time"));
    }
    
    @Test
    public void shouldPopulateErrorIfBothSrcFileAndSrcDirAreNotDefined() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src_file", "dest");
        task.setSrcfile(null);
        task.validate(ValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(FetchTask.SRC), is("Should provide either srcdir or srcfile"));
    }

    @Test
    public void shouldIndicateSourceIsAFileBasedOnValuePopulated() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setSrcfile("a.txt");
        assertThat(fetchTask.isSourceAFile(), is(true));

        FetchTask fetchTaskWithDir = new FetchTask();
        fetchTaskWithDir.setSrcdir("/a/b");
        assertThat(fetchTaskWithDir.isSourceAFile(), is(false));
    }

    @Test
    public void shouldReturnSrcFileWhenSrcFileIsNotEmpty() throws Exception {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setSrcfile("a.jar");
        assertThat(fetchTask.getSrc(), is("a.jar"));
    }

    @Test
    public void shouldReturnSrcDirWhenSrcDirIsNotEmpty() throws Exception {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setSrcdir("folder");
        assertThat(fetchTask.getSrc(), is("folder"));
    }

    @Test
    public void shouldNormalizeDest() throws Exception {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise-output/console.log",
                "dest\\subfolder");
        assertThat(fetchTask.getDest(), is("dest/subfolder"));
    }

    @Test
    public void shouldNormalizeSrcFile() throws Exception {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise-output\\console.log",
                "dest\\subfolder");
        assertThat(fetchTask.getSrc(), is("cruise-output/console.log"));
    }

    @Test
    public void shouldNormalizeSrcDir() throws Exception {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "", "dest\\subfolder");
        fetchTask.setSrcdir("testfolder\\subfolder");
        assertThat(fetchTask.getSrc(), is("testfolder/subfolder"));
    }

    @Test
    public void describeTestForSrcFile() throws Exception {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise.zip", "dest\\subfolder");
        assertThat(fetchTask.describe(),
                is("<fetchartifact pipeline=\"mingle\" stage=\"dev\" job=\"windows-3\" "
                        + "srcfile=\"cruise.zip\" dest=\"dest\\subfolder\" />"));
    }

    @Test
    public void describeTestForSrcDir() throws Exception {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "", "dest\\subfolder");
        fetchTask.setSrcdir("cruise-output");
        assertThat(fetchTask.describe(),
                is("<fetchartifact pipeline=\"mingle\" stage=\"dev\" job=\"windows-3\" "
                        + "srcdir=\"cruise-output\" dest=\"dest\\subfolder\" />"));
    }

    @Test
    public void describeTestForSrcDirAndSrcFile() throws Exception {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise.zip", "dest\\subfolder");
        fetchTask.setSrcdir("cruise-output");
        assertThat(fetchTask.describe(),
                is("<fetchartifact pipeline=\"mingle\" stage=\"dev\" job=\"windows-3\" srcfile=\"cruise.zip\" "
                        + "srcdir=\"cruise-output\" dest=\"dest\\subfolder\" />"));
    }

    @Test
    public void shouldUpdateItsAttributesFromAttributeMap() throws Exception {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setConfigAttributes(
                m(FetchTask.PIPELINE_NAME, "pipeline_foo", FetchTask.STAGE, "stage_bar", FetchTask.JOB, "job_baz", FetchTask.SRC, "src_file", FetchTask.DEST, "dest_dir", FetchTask.IS_SOURCE_A_FILE, "1"));
        assertThat(fetchTask.getTargetPipelineName(), is(new CaseInsensitiveString("pipeline_foo")));
        assertThat(fetchTask.getStage(), is(new CaseInsensitiveString("stage_bar")));
        assertThat(fetchTask.getJob().toString(), is("job_baz"));
        assertThat(fetchTask.getSrcfile(), is("src_file"));
        assertThat(fetchTask.getSrcdir(), is(nullValue()));
        assertThat(fetchTask.getDest(), is("dest_dir"));
        fetchTask.setConfigAttributes(m(FetchTask.PIPELINE_NAME, "", FetchTask.STAGE, "", FetchTask.JOB, "", FetchTask.SRC, "", FetchTask.IS_SOURCE_A_FILE, "1", FetchTask.DEST, ""));
        assertThat(fetchTask.getTargetPipelineName(), is(new CaseInsensitiveString("")));
        assertThat(fetchTask.getStage(), is(new CaseInsensitiveString("")));
        assertThat(fetchTask.getJob().toString(), is(""));
        assertThat(fetchTask.getSrcfile(), is(nullValue()));
        assertThat(fetchTask.getSrcdir(), is(nullValue()));
        assertThat(fetchTask.getDest(), is(""));
    }

    @Test
    public void shouldSetSrcFileToNullIfSrcDirIsDefined() throws Exception {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setConfigAttributes(
                m(FetchTask.PIPELINE_NAME, "pipeline_foo", FetchTask.STAGE, "stage_bar", FetchTask.JOB, "job_baz", FetchTask.IS_SOURCE_A_FILE, "0", FetchTask.SRC, "src_dir", FetchTask.DEST,
                        "dest_dir"));

        assertThat(fetchTask.getSrcfile(), is(nullValue()));
        assertThat(fetchTask.getSrcdir(), is("src_dir"));
    }

    @Test
    public void shouldSetSrcFileToNullWhenSrcDirIsUpdated() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("pname"), new CaseInsensitiveString("sname"), new CaseInsensitiveString("jname"), "sfile", "dest");
        fetchTask.setConfigAttributes(
                m(FetchTask.PIPELINE_NAME, "pipeline_foo", FetchTask.STAGE, "stage_bar", FetchTask.JOB, "job_baz", FetchTask.IS_SOURCE_A_FILE, "0", FetchTask.SRC, "src_dir",  FetchTask.DEST,
                        "dest_dir"));

        assertThat(fetchTask.getSrcfile(), is(nullValue()));
        assertThat(fetchTask.getSrcdir(), is("src_dir"));
    }

    @Test
    public void shouldNotUpdateItsAttributesFromAttributeMapWhenKeysNotPresent() throws Exception {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setConfigAttributes(
                m(FetchTask.PIPELINE_NAME, "pipeline_foo", FetchTask.STAGE, "stage_bar", FetchTask.JOB, "job_baz", FetchTask.SRC, "src_file", FetchTask.IS_SOURCE_A_FILE, "1", FetchTask.SRC, "src_file", FetchTask.DEST,
                        "dest_dir"));
        fetchTask.setConfigAttributes(m());
        assertThat(fetchTask.getTargetPipelineName(), is(new CaseInsensitiveString("pipeline_foo")));
        assertThat(fetchTask.getStage(), is(new CaseInsensitiveString("stage_bar")));
        assertThat(fetchTask.getJob().toString(), is("job_baz"));
        assertThat(fetchTask.getSrcfile(), is("src_file"));
        assertThat(fetchTask.getSrcdir(), is(nullValue()));
        assertThat(fetchTask.getDest(), is("dest_dir"));
    }

    @Test
    public void shouldUpdateDestToNullIfDestIsEmptyInAttributeMap_SoThatItDoesNotGetSerializedInXml() {
        FetchTask fetchTask =  new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", "dest");
        fetchTask.setConfigAttributes(m(FetchTask.DEST, ""));
        assertThat(fetchTask, is(new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", null)));
    }

    @Test
    public void shouldPopulateAllFieldsInReturnedPropertiesForDisplay() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("foo-pipeline"), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), "quux.c", "bang-file");
        assertThat(fetchTask.getPropertiesForDisplay(), hasItems(new TaskProperty("PIPELINE_NAME", "foo-pipeline", "pipeline_name"),
                new TaskProperty("STAGE_NAME", "bar-stage", "stage_name"), new TaskProperty("JOB_NAME", "baz-job", "job_name"),
                new TaskProperty("SRC_FILE", "quux.c", "src_file"), new TaskProperty("DEST_FILE", "bang-file", "dest_file")));
        assertThat(fetchTask.getPropertiesForDisplay().size(), is(5));

        fetchTask = new FetchTask(new CaseInsensitiveString("foo-pipeline"), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), null, "bang-file");
        fetchTask.setSrcdir("foo/src");
        assertThat(fetchTask.getPropertiesForDisplay(), hasItems(new TaskProperty("PIPELINE_NAME", "foo-pipeline", "pipeline_name"),
                new TaskProperty("STAGE_NAME", "bar-stage", "stage_name"), new TaskProperty("JOB_NAME", "baz-job", "job_name"),
                new TaskProperty("SRC_DIR", "foo/src", "src_dir"), new TaskProperty("DEST_FILE", "bang-file", "dest_file")));
        assertThat(fetchTask.getPropertiesForDisplay().size(), is(5));

        fetchTask = new FetchTask(new CaseInsensitiveString(null), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), null, null);
        assertThat(fetchTask.getPropertiesForDisplay(), hasItems(new TaskProperty("STAGE_NAME", "bar-stage", "stage_name"), new TaskProperty("JOB_NAME", "baz-job", "job_name")));
        assertThat(fetchTask.getPropertiesForDisplay().size(), is(2));
    }

    @Test
    public void shouldCreateChecksumPath() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("up-pipeline"), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), "quux.c", "bang-file");
        String checksumPath = fetchTask.checksumPath();
        assertThat(checksumPath, is("up-pipeline_bar-stage_baz-job_md5.checksum"));
    }

    @Test
    public void shouldCreateArtifactDest() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("up-pipeline"), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), "quux.c", "dest-dir");
        File artifactDest = fetchTask.artifactDest("foo-pipeline", "file-name-in-dest");
        assertThat(artifactDest, is(new File("pipelines/foo-pipeline/dest-dir/file-name-in-dest")));
    }

    @Test
    public void shouldNotPopulatePropertyForPipelineWhenPipelineIsNull() {
        FetchTask fetchTask = new FetchTask(null, new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), "quux.c", "bang-file");
        // is null when no pipeline name is specified in config xml (manual entry)
        ReflectionUtil.setField(fetchTask, "pipelineName", null);
        assertThat(fetchTask.getPropertiesForDisplay(), hasItems(new TaskProperty("STAGE_NAME", "bar-stage", "stage_name"), new TaskProperty("JOB_NAME", "baz-job", "job_name"),
                new TaskProperty("SRC_FILE", "quux.c", "src_file"), new TaskProperty("DEST_FILE", "bang-file", "dest_file")));
        assertThat(fetchTask.getPropertiesForDisplay().size(), is(4));
    }

    private Pipeline pipeline(String label) {
        Pipeline pipeline = createPipeline("cruise", new NullStage("Stage"));
        pipeline.setLabel(label);
        return pipeline;
    }

    private Pipeline createPipeline(String pipelineName, Stage stage) {
        Materials materials = MaterialsMother.defaultMaterials();
        return new Pipeline(pipelineName, BuildCause.createWithModifications(ModificationsMother.modifyOneFile(materials, ModificationsMother.nextRevision()), ""), stage);
    }

    private String getSrc() {
        return "";
    }

    private Pipeline pipelineWithStage(String pipelineName, int pipelineCounter, String label, String stagename,
                                       int stageCounter) {
        Stage stage = StageMother.custom(stagename);
        stage.setCounter(stageCounter);
        Pipeline pipeline = createPipeline(pipelineName, stage);
        pipeline.setCounter(pipelineCounter);
        pipeline.setLabel(label);
        return pipeline;
    }

    private Pipeline pipelineWithDepencencyMaterial(String currentPipeline, String upstreamPipelineName,
                                                    int upstreamPipelineCounter, String upstreamPipelineLabel,
                                                    String upstreamStageName,
                                                    int upstreamStageCounter) {
        Pipeline pipeline = createPipeline(currentPipeline, new NullStage("Stage"));
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

}
