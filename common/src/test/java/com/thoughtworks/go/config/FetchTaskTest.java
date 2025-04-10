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

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FetchTaskTest {
    private PipelineConfig downstream;
    private PipelineConfig upstream;
    private PipelineConfig uppestStream;
    private CruiseConfig config;

    @BeforeEach
    void setUp() {
        config = GoConfigMother.configWithPipelines("random_pipeline", "uppest_lookalike", "uppest_stream", "upstreams_peer", "upstream", "downstream", "dummy");

        PipelineConfig randomPipeline = config.pipelineConfigByName(new CaseInsensitiveString("random_pipeline"));
        randomPipeline.add(StageConfigMother.stageConfig("random-stage1", new JobConfigs(new JobConfig("random-job1"))));

        PipelineConfig uppestLookalike = config.pipelineConfigByName(new CaseInsensitiveString("uppest_lookalike"));
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
    }

    @Test
    void shouldImplementAbstractFetchArtifact() {
        assertThat(new FetchTask() instanceof AbstractFetchTask).isTrue();
    }

    @Test
    void validate_shouldErrorWhenReferencingConfigRepositoryPipelineFromFilePipeline() {
        uppestStream.setOrigin(new RepoConfigOrigin());
        downstream.setOrigin(new FileConfigOrigin());

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().first()));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.ARTIFACT_ORIGIN)).startsWith("\"downstream :: stage :: job\" tries to fetch artifact from job \"uppest_stream :: uppest-stage2 :: uppest-job2\" which is defined in");

    }

    @Test
    void validate_shouldNotErrorWhenReferencingFilePipelineFromConfigRepositoryPipeline() {
        uppestStream.setOrigin(new FileConfigOrigin());
        downstream.setOrigin(new RepoConfigOrigin());

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldNotErrorWhenReferencingConfigRepositoryPipelineFromConfigRepositoryPipeline() {
        uppestStream.setOrigin(new RepoConfigOrigin());
        downstream.setOrigin(new RepoConfigOrigin());

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldNotErrorWhenReferencingFilePipelineFromFilePipeline() {
        uppestStream.setOrigin(new FileConfigOrigin());
        downstream.setOrigin(new FileConfigOrigin());

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldPassValidationWhenSrcAndDestDirectoryAreInsideAgentSandbox() {
        StageConfig stage = upstream.getFirstStageConfig();
        JobConfig job = stage.getJobs().get(0);
        FetchTask task = new FetchTask(upstream.name(), stage.name(), job.name(), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(upstream, stage, job));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldFailValidationWhenSrcDirectoryIsOutsideAgentSandbox() {
        validateAndAssertIncorrectPath("/src", true, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("../src", true, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("..", true, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("first-level/../../../going-back", true, "dest", FetchTask.SRC);
    }

    @Test
    void shouldFailValidationWhenSrcFileIsOutsideAgentSandbox() {
        validateAndAssertIncorrectPath("/src/junk.txt", false, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("../junk.txt", false, "dest", FetchTask.SRC);
        validateAndAssertIncorrectPath("first-level/../../../going-back/file.txt", false, "dest", FetchTask.SRC);
    }

    @Test
    void shouldFailValidationWhenDestDirectoryIsOutsideAgentSandbox() {
        validateAndAssertIncorrectPath("src", false, "/dest", FetchTask.DEST);
        validateAndAssertIncorrectPath("src", false, "../dest", FetchTask.DEST);
        validateAndAssertIncorrectPath("src", false, "..", FetchTask.DEST);
        validateAndAssertIncorrectPath("src", false, "first-level/../../../going-back", FetchTask.DEST);
    }

    @Test
    void validate_withinTemplates_shouldPopulateErrorOnSrcFileOrSrcDirOrDestIfIsNotAValidFilePathPattern() {
        String dest = "..";
        String src = "..";

        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template-1");
        StageConfig stage = template.get(0);
        JobConfig job = stage.getJobs().get(0);
        FetchTask task = new FetchTask(template.name(), stage.name(), job.name(), src, dest);
        ValidationContext context = ConfigSaveValidationContext.forChain(config, template, stage, job);
        task.validate(context);

        assertThat(task.errors().isEmpty()).isFalse();
        String messageForSrc = String.format("Task of job '%s' in stage '%s' of template '%s' has src path '%s' which is outside the working directory.", job.name(), stage.name(), template.name(), src);
        assertThat(task.errors().on(FetchTask.SRC)).isEqualTo(messageForSrc);

        String messageForDest = String.format("Task of job '%s' in stage '%s' of template '%s' has dest path '%s' which is outside the working directory.", job.name(), stage.name(), template.name(), dest);
        assertThat(task.errors().on(FetchTask.DEST)).isEqualTo(messageForDest);
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
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), upstream, stage, job));

        assertThat(task.errors().isEmpty()).isFalse();
        String path = propertyName.equals(FetchTask.SRC) ? source : destination;
        String message = String.format("Task of job '%s' in stage '%s' of pipeline '%s' has %s path '%s' which is outside the working directory.", job.name(), stage.name(), upstream.name(), propertyName, path);
        assertThat(task.errors().on(propertyName)).isEqualTo(message);
    }

    @Test
    void validate_shouldPopulateErrorOnSrcFileOrSrcDirOrDestIfIsNotAValidFilePathPattern() {
        FetchTask task = new FetchTask(new CaseInsensitiveString(""), new CaseInsensitiveString(""), new CaseInsensitiveString(""), "", "");
        task.validateAttributes(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(
                new CaseInsensitiveString("stage")).getJobs().get(0)));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.SRC)).isEqualTo("Should provide either srcdir or srcfile");
    }

    @Test
    void validate_shouldNotTryAndValidateWhenWithinTemplate() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new TemplatesConfig(), downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldValidateBlankStageAndJobWhenWithinTemplate() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString(""), new CaseInsensitiveString(""), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new TemplatesConfig(), downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("Stage is a required field.");
        assertThat(task.errors().on(FetchTask.JOB)).isEqualTo("Job is a required field.");
    }

    @Test
    void shouldPopulateErrorsIfFetchArtifactFromPipelineThatIsNotDependency() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME)).isEqualTo("Pipeline \"downstream\" tries to fetch artifact from pipeline "
                + "\"dummy\" which is not an upstream pipeline");
        assertThat(downstream.errors().on("base")).isEqualTo("Pipeline \"downstream\" tries to fetch artifact from pipeline \"dummy\" which is not an upstream pipeline");
    }

    @Test
    void shouldPopulateErrorsIfFetchArtifactDoesNotHaveStageAndOrJobDefined() {
        FetchTask task = new FetchTask(new CaseInsensitiveString(""), new CaseInsensitiveString(""), new CaseInsensitiveString(""), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, new StageConfig(), new JobConfig()));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("Stage is a required field.");
        assertThat(task.errors().on(FetchTask.JOB)).isEqualTo("Job is a required field.");
    }

    @Test
    void shouldBeValidWhenFetchArtifactIsFromAnyAncestorStage_onTheUpstreamPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldBeValidWhenFetchArtifactIsFromAnyAncestorStage_s_predecessorStage__onTheUpstreamPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void should_NOT_BeValidWhenFetchArtifactIsFromAnyAncestorStage_s_successorStage_onTheUpstreamPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage3"), new CaseInsensitiveString("uppest-job3"), "src", "dest");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().first()));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage \"uppest_stream :: uppest-stage3\" which does not complete before \"downstream\" pipeline's dependencies.");
    }

    @Test
    void should_NOT_BeValidWhen_pathFromAncestor_isInvalid_becauseRefferedPipelineIsNotAnAncestor() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("random_pipeline/upstream"), new CaseInsensitiveString("random-stage1"), new CaseInsensitiveString("random-job1"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME)).isEqualTo("Pipeline named 'random_pipeline' exists, but is not an ancestor of 'downstream' as declared in 'random_pipeline/upstream'.");
        assertThat(downstream.errors().on("base")).isEqualTo("Pipeline named 'random_pipeline' exists, but is not an ancestor of 'downstream' as declared in 'random_pipeline/upstream'.");
    }

    @Test
    void should_NOT_BeValidWhen_NO_pathFromAncestorIsGiven_butAncestorPipelineIsBeingFetchedFrom() {
        FetchTask task = new FetchTask(null, new CaseInsensitiveString("uppest-stage3"), new CaseInsensitiveString("uppest-job3"), "src", "dest");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().get(0)));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage \"downstream :: uppest-stage3\" which does not exist.");
    }

    @Test
    void should_BeValidWhen_hasAnAlternatePathToAncestor() {
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();

        task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void should_NOT_BeValidWhen_ImmediateParentDeclaredInPathFromAncestor_isNotAParentPipeline() {
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream/uppest_stream"), new CaseInsensitiveString("up-stage1"), new CaseInsensitiveString("up-job1"), "src", "dest");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().get(0)));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME)).isEqualTo("Pipeline named 'uppest_stream' exists, but is not an ancestor of 'downstream' as declared in 'upstream/uppest_stream'.");
        assertThat(downstream.errors().on("base")).isEqualTo("Pipeline named 'uppest_stream' exists, but is not an ancestor of 'downstream' as declared in 'upstream/uppest_stream'.");
    }

    @Test
    void should_NOT_BeValidWhen_ImmediateParentDeclaredInPathFromAncestor_isNotAParentPipeline_PipelineConfigValidationContext() {
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream/uppest_stream"), new CaseInsensitiveString("up-stage1"), new CaseInsensitiveString("up-job1"), "src", "dest");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));

        task.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", config, downstream, stage, stage.getJobs().get(0)));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME)).isEqualTo("Pipeline named 'uppest_stream' exists, but is not an ancestor of 'downstream' as declared in 'upstream/uppest_stream'.");
        assertThat(downstream.errors().on("base")).isEqualTo("Pipeline named 'uppest_stream' exists, but is not an ancestor of 'downstream' as declared in 'upstream/uppest_stream'.");
    }

    @Test
    void should_NOT_BeValidWhen_stageMayNotHaveRunViaTheGivenPath_evenThoughItMayHaveActuallyRunAccordingToAnAlternatePath() {//TODO: Please fix this if someone cares about this corner case working -jj
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchTask task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().first()));
        assertThat(task.errors().isEmpty()).isTrue();

        task = new FetchTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().first()));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage \"uppest_stream :: uppest-stage2\" which does not complete before \"downstream\" pipeline's dependencies.");
    }

    @Test
    void shouldFailWhenFetchArtifactIsFromAnyStage_AFTER_theDependencyStageOnTheUpstreamPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("up-stage2"), new CaseInsensitiveString("up-job2"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().first()));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage \"upstream :: up-stage2\" which does not complete before \"downstream\" pipeline's dependencies.");
    }

    @Test
    void shouldPopulateErrorIfFetchArtifactFromDependentPipelineButStageDoesNotExist() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage-does-not-exist"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(
                new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().get(0)));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage "
                + "\"upstream :: stage-does-not-exist\" which does not exist.");
    }

    @Test
    void shouldPopulateErrorIfFetchArtifactFromDependentPipelineButJobNotExist() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job-does-not-exist"), "src", "dest");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().first()));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.JOB)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from job "
                + "\"upstream :: stage :: job-does-not-exist\" which does not exist.");
    }

    @Test
    void shouldBeValidIfFetchArtifactUsingADependantPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("up-stage1"), new CaseInsensitiveString("up-job1"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldBeValidIfFetchArtifactUsingAStageBeforeCurrentInTheSamePipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("up-stage1"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldBeValidIfFetchArtifactDoesNotSpecifyPipeline() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("up-stage1"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    // The Fetch Task is now case insensitive to job names . This behavior has changed from before migration of validations
    // to inside Config Objects for clicky admin. It was earlier Case SENSITIVE. This was done to address #4970
    // -Jake
    @Test
    void shouldPopulateErrorsIfFetchArtifactUsingJobNameWithDifferentCase() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("JOB"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldPopulateErrorIfSrcFileAndSrcDirBothAreDefined() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src_file", "dest");
        task.setSrcdir("src_dir");
        task.validate(ConfigSaveValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.SRC)).isEqualTo("Only one of srcfile or srcdir is allowed at a time");
    }

    @Test
    void shouldPopulateErrorIfBothSrcFileAndSrcDirAreNotDefined() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src_file", "dest");
        task.setSrcfile(null);
        task.validate(ConfigSaveValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.SRC)).isEqualTo("Should provide either srcdir or srcfile");
    }

    @Test
    void shouldPopulateErrorOnSrcFileOrSrcDirOrDestIfIsNotAValidFilePathPattern() {
        FetchTask task = new FetchTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "..", "..");
        StageConfig stage = upstream.getStage(new CaseInsensitiveString("stage"));
        ValidationContext context = ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), upstream, stage, stage.getJobs().first());
        task.validate(context);
        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.SRC)).isEqualTo("Task of job 'job' in stage 'stage' of pipeline 'upstream' has src path '..' which is outside the working directory.");
        assertThat(task.errors().on(FetchTask.DEST)).isEqualTo("Task of job 'job' in stage 'stage' of pipeline 'upstream' has dest path '..' which is outside the working directory.");
        task.setSrcfile(null);
        task.setSrcdir("..");
        task.validate(context);
        assertThat(task.errors().on(FetchTask.SRC)).isEqualTo("Task of job 'job' in stage 'stage' of pipeline 'upstream' has src path '..' which is outside the working directory.");
    }

    @Test
    void shouldIndicateSourceIsAFileBasedOnValuePopulated() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setSrcfile("a.txt");
        assertThat(fetchTask.isSourceAFile()).isTrue();

        FetchTask fetchTaskWithDir = new FetchTask();
        fetchTaskWithDir.setSrcdir("/a/b");
        assertThat(fetchTaskWithDir.isSourceAFile()).isFalse();
    }

    @Test
    void shouldReturnSrcFileWhenSrcFileIsNotEmpty() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setSrcfile("a.jar");
        assertThat(fetchTask.getSrc()).isEqualTo("a.jar");
    }

    @Test
    void shouldReturnSrcDirWhenSrcDirIsNotEmpty() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setSrcdir("folder");
        assertThat(fetchTask.getSrc()).isEqualTo("folder");
    }

    @Test
    void shouldNormalizeDest() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise-output/console.log",
                "dest\\subfolder");
        assertThat(fetchTask.getDest()).isEqualTo("dest/subfolder");
    }

    @Test
    void shouldNormalizeSrcFile() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise-output\\console.log",
                "dest\\subfolder");
        assertThat(fetchTask.getSrc()).isEqualTo("cruise-output/console.log");
    }

    @Test
    void shouldNormalizeSrcDir() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "", "dest\\subfolder");
        fetchTask.setSrcdir("testfolder\\subfolder");
        assertThat(fetchTask.getSrc()).isEqualTo("testfolder/subfolder");
    }

    @Test
    void describeTestForSrcFile() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise.zip", "dest\\subfolder");
        assertThat(fetchTask.describe()).isEqualTo("fetch artifact [cruise.zip] => [dest/subfolder] from [mingle/dev/windows-3]");
    }

    @Test
    void describeTestForSrcDir() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "", "dest\\subfolder");
        fetchTask.setSrcdir("cruise-output");
        assertThat(fetchTask.describe()).isEqualTo("fetch artifact [cruise-output] => [dest/subfolder] from [mingle/dev/windows-3]");
    }

    @Test
    void describeTestForSrcDirAndSrcFile() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("windows-3"), "cruise.zip", "dest\\subfolder");
        fetchTask.setSrcdir("cruise-output");
        assertThat(fetchTask.describe()).isEqualTo("fetch artifact [cruise.zip] => [dest/subfolder] from [mingle/dev/windows-3]");
    }

    @Test
    void shouldUpdateItsAttributesFromAttributeMap() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setConfigAttributes(
                Map.of(FetchTask.PIPELINE_NAME, "pipeline_foo", FetchTask.STAGE, "stage_bar", FetchTask.JOB, "job_baz", FetchTask.SRC, "src_file", FetchTask.DEST, "dest_dir", FetchTask.IS_SOURCE_A_FILE, "1"));
        assertThat(fetchTask.getTargetPipelineName()).isEqualTo(new CaseInsensitiveString("pipeline_foo"));
        assertThat(fetchTask.getStage()).isEqualTo(new CaseInsensitiveString("stage_bar"));
        assertThat(fetchTask.getJob().toString()).isEqualTo("job_baz");
        assertThat(fetchTask.getSrcfile()).isEqualTo("src_file");
        assertThat(fetchTask.getSrcdir()).isNull();
        assertThat(fetchTask.getDest()).isEqualTo("dest_dir");
        fetchTask.setConfigAttributes(Map.of(FetchTask.PIPELINE_NAME, "", FetchTask.STAGE, "", FetchTask.JOB, "", FetchTask.SRC, "", FetchTask.IS_SOURCE_A_FILE, "1", FetchTask.DEST, ""));
        assertThat(fetchTask.getTargetPipelineName()).isEqualTo(new CaseInsensitiveString(""));
        assertThat(fetchTask.getStage()).isEqualTo(new CaseInsensitiveString(""));
        assertThat(fetchTask.getJob().toString()).isEqualTo("");
        assertThat(fetchTask.getSrcfile()).isNull();
        assertThat(fetchTask.getSrcdir()).isNull();
        assertThat(fetchTask.getDest()).isEqualTo("");
    }

    @Test
    void shouldSetSrcFileToNullIfSrcDirIsDefined() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setConfigAttributes(
                Map.of(FetchTask.PIPELINE_NAME, "pipeline_foo", FetchTask.STAGE, "stage_bar", FetchTask.JOB, "job_baz", FetchTask.IS_SOURCE_A_FILE, "0", FetchTask.SRC, "src_dir", FetchTask.DEST,
                        "dest_dir"));

        assertThat(fetchTask.getSrcfile()).isNull();
        assertThat(fetchTask.getSrcdir()).isEqualTo("src_dir");
    }

    @Test
    void shouldSetSrcFileToNullWhenSrcDirIsUpdated() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("pname"), new CaseInsensitiveString("sname"), new CaseInsensitiveString("jname"), "sfile", "dest");
        fetchTask.setConfigAttributes(
                Map.of(FetchTask.PIPELINE_NAME, "pipeline_foo", FetchTask.STAGE, "stage_bar", FetchTask.JOB, "job_baz", FetchTask.IS_SOURCE_A_FILE, "0", FetchTask.SRC, "src_dir", FetchTask.DEST,
                        "dest_dir"));

        assertThat(fetchTask.getSrcfile()).isNull();
        assertThat(fetchTask.getSrcdir()).isEqualTo("src_dir");
    }

    @Test
    void shouldNotUpdateItsAttributesFromAttributeMapWhenKeysNotPresent() {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setConfigAttributes(
                Map.of(FetchTask.PIPELINE_NAME, "pipeline_foo", FetchTask.STAGE, "stage_bar", FetchTask.JOB, "job_baz", FetchTask.SRC, "src_file", FetchTask.IS_SOURCE_A_FILE, "1", FetchTask.DEST, "dest_dir"));
        fetchTask.setConfigAttributes(Map.of());
        assertThat(fetchTask.getTargetPipelineName()).isEqualTo(new CaseInsensitiveString("pipeline_foo"));
        assertThat(fetchTask.getStage()).isEqualTo(new CaseInsensitiveString("stage_bar"));
        assertThat(fetchTask.getJob().toString()).isEqualTo("job_baz");
        assertThat(fetchTask.getSrcfile()).isEqualTo("src_file");
        assertThat(fetchTask.getSrcdir()).isNull();
        assertThat(fetchTask.getDest()).isEqualTo("dest_dir");
    }

    @Test
    void shouldUpdateDestToNullIfDestIsEmptyInAttributeMap_SoThatItDoesNotGetSerializedInXml() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", "dest");
        fetchTask.setConfigAttributes(Map.of(FetchTask.DEST, ""));
        assertThat(fetchTask).isEqualTo(new FetchTask(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"), new CaseInsensitiveString("one"), "", null));
    }

    @Test
    void shouldPopulateAllFieldsInReturnedPropertiesForDisplay() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("foo-pipeline"), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), "quux.c", "bang-file");
        assertThat(fetchTask.getPropertiesForDisplay()).contains(new TaskProperty("Pipeline Name", "foo-pipeline", "pipeline_name"), new TaskProperty("Stage Name", "bar-stage", "stage_name"), new TaskProperty("Job Name", "baz-job", "job_name"), new TaskProperty("Source File", "quux.c", "source_file"), new TaskProperty("Destination", "bang-file", "destination"));
        assertThat(fetchTask.getPropertiesForDisplay().size()).isEqualTo(5);

        fetchTask = new FetchTask(new CaseInsensitiveString("foo-pipeline"), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), null, "bang-file");
        fetchTask.setSrcdir("foo/src");
        assertThat(fetchTask.getPropertiesForDisplay()).contains(new TaskProperty("Pipeline Name", "foo-pipeline", "pipeline_name"), new TaskProperty("Stage Name", "bar-stage", "stage_name"), new TaskProperty("Job Name", "baz-job", "job_name"), new TaskProperty("Source Directory", "foo/src", "source_directory"), new TaskProperty("Destination", "bang-file", "destination"));
        assertThat(fetchTask.getPropertiesForDisplay().size()).isEqualTo(5);

        fetchTask = new FetchTask(new CaseInsensitiveString(null), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), null, null);
        assertThat(fetchTask.getPropertiesForDisplay()).contains(new TaskProperty("Stage Name", "bar-stage", "stage_name"), new TaskProperty("Job Name", "baz-job", "job_name"));
        assertThat(fetchTask.getPropertiesForDisplay().size()).isEqualTo(2);
    }

    @Test
    void shouldCreateChecksumPath() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("up-pipeline"), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), "quux.c", "bang-file");
        String checksumPath = fetchTask.checksumPath();
        assertThat(checksumPath).isEqualTo("up-pipeline_bar-stage_baz-job_md5.checksum");
    }

    @Test
    void shouldCreateArtifactDest() {
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("up-pipeline"), new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), "quux.c", "dest-dir");
        File artifactDest = fetchTask.artifactDest("foo-pipeline", "file-name-in-dest");
        assertThat(artifactDest).isEqualTo(new File("pipelines/foo-pipeline/dest-dir/file-name-in-dest"));
    }

    @Test
    void shouldNotPopulatePropertyForPipelineWhenPipelineIsNull() {
        FetchTask fetchTask = new FetchTask(null, new CaseInsensitiveString("bar-stage"), new CaseInsensitiveString("baz-job"), "quux.c", "bang-file");
        // is null when no pipeline name is specified in config xml (manual entry)
        ReflectionUtil.setField(fetchTask, "pipelineName", null);
        assertThat(fetchTask.getPropertiesForDisplay()).contains(new TaskProperty("Stage Name", "bar-stage", "stage_name"), new TaskProperty("Job Name", "baz-job", "job_name"), new TaskProperty("Source File", "quux.c", "source_file"), new TaskProperty("Destination", "bang-file", "destination"));
        assertThat(fetchTask.getPropertiesForDisplay().size()).isEqualTo(4);
    }

    @Test
    void shouldNotFailValidationIfUpstreamExists_PipelineConfigSave() {
        PipelineConfig upstream = new PipelineConfig(new CaseInsensitiveString("upstream-pipeline"),
                new MaterialConfigs(), new StageConfig(new CaseInsensitiveString("upstream-stage"),
                new JobConfigs(new JobConfig(new CaseInsensitiveString("upstream-job")))));
        JobConfig job = new JobConfig(new CaseInsensitiveString("downstream-job"));
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("upstream-pipeline"),
                new CaseInsensitiveString("upstream-stage"),
                new CaseInsensitiveString("upstream-job"), "quux.c", "bang-file");
        job.addTask(fetchTask);
        PipelineConfig downstream = new PipelineConfig(new CaseInsensitiveString("downstream-pipeline"),
                new MaterialConfigs(new DependencyMaterialConfig(upstream.name(), upstream.getFirstStageConfig().name())),
                new StageConfig(new CaseInsensitiveString("downstream-stage"), new JobConfigs(job)));

        fetchTask.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(new BasicPipelineConfigs(upstream, downstream)), downstream, downstream.getFirstStageConfig(), downstream.getFirstStageConfig().getJobs().first()));
        assertThat(fetchTask.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldFailValidationIfFetchArtifactPipelineIsNotAMaterial_PipelineConfigSave() {
        PipelineConfig upstream = new PipelineConfig(new CaseInsensitiveString("upstream-pipeline"),
                new MaterialConfigs(), new StageConfig(new CaseInsensitiveString("upstream-stage"),
                new JobConfigs(new JobConfig(new CaseInsensitiveString("upstream-job")))));
        JobConfig job = new JobConfig(new CaseInsensitiveString("downstream-job"));
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("upstream-pipeline"),
                new CaseInsensitiveString("upstream-stage"),
                new CaseInsensitiveString("upstream-job"), "quux.c", "bang-file");
        job.addTask(fetchTask);
        PipelineConfig downstream = new PipelineConfig(new CaseInsensitiveString("downstream-pipeline"),
                new MaterialConfigs(),
                new StageConfig(new CaseInsensitiveString("downstream-stage"), new JobConfigs(job)));

        fetchTask.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(new BasicPipelineConfigs(upstream, downstream)), downstream, downstream.getFirstStageConfig(), downstream.getFirstStageConfig().getJobs().first()));
        assertThat(fetchTask.errors().isEmpty()).isFalse();
        assertThat(fetchTask.errors().on(FetchTask.PIPELINE_NAME)).isEqualTo("Pipeline \"downstream-pipeline\" tries to fetch artifact from pipeline \"upstream-pipeline\" which is not an upstream pipeline");
        assertThat(downstream.errors().on("base")).isEqualTo("Pipeline \"downstream-pipeline\" tries to fetch artifact from pipeline \"upstream-pipeline\" which is not an upstream pipeline");
    }

    @Test
    void shouldFailValidationIfFetchArtifactPipelineAndStageExistsButJobDoesNot_PipelineConfigSave() {
        PipelineConfig upstream = new PipelineConfig(new CaseInsensitiveString("upstream-pipeline"),
                new MaterialConfigs(), new StageConfig(new CaseInsensitiveString("upstream-stage"),
                new JobConfigs(new JobConfig(new CaseInsensitiveString("upstream-job")))));
        JobConfig job = new JobConfig(new CaseInsensitiveString("downstream-job"));
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("upstream-pipeline"),
                new CaseInsensitiveString("upstream-stage"),
                new CaseInsensitiveString("some-random-job"), "quux.c", "bang-file");
        job.addTask(fetchTask);
        PipelineConfig downstream = new PipelineConfig(new CaseInsensitiveString("downstream-pipeline"),
                new MaterialConfigs(new DependencyMaterialConfig(upstream.name(), upstream.getFirstStageConfig().name())),
                new StageConfig(new CaseInsensitiveString("downstream-stage"), new JobConfigs(job)));

        fetchTask.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(new BasicPipelineConfigs(upstream, downstream)), downstream, downstream.getFirstStageConfig(), downstream.getFirstStageConfig().getJobs().first()));
        assertThat(fetchTask.errors().isEmpty()).isFalse();
        assertThat(fetchTask.errors().on(FetchTask.JOB)).isEqualTo("\"downstream-pipeline :: downstream-stage :: downstream-job\" tries to fetch artifact from job \"upstream-pipeline :: upstream-stage :: some-random-job\" which does not exist.");
    }

    @Test
    void shouldPassValidationWhenFetchingFromAnInstanceOfRunOnAllJob() {
        StageConfig stage = upstream.getFirstStageConfig();
        JobConfig job = stage.getJobs().get(0);
        job.setRunOnAllAgents(true);
        FetchTask task = new FetchTask(upstream.name(), stage.name(), new CaseInsensitiveString(job.name() + "-runOnAll-1"), "src", "dest");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), upstream, stage, job));

        assertThat(task.errors().on(FetchTask.JOB)).isNull();
    }

}
