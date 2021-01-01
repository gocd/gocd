/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FetchPluggableArtifactTaskTest {
    private PipelineConfig downstream;
    private PipelineConfig upstream;
    private PipelineConfig uppestStream;
    private CruiseConfig config;
    private ArtifactPluginInfo artifactPluginInfo;

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
        downstream.get(0).getJobs().get(0).addTask(new FetchPluggableArtifactTask(new CaseInsensitiveString("foo"), new CaseInsensitiveString("bar"), new CaseInsensitiveString("baz"), "s3"));

        artifactPluginInfo = mock(ArtifactPluginInfo.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(artifactPluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginDescriptor.id()).thenReturn("cd.go.s3");
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }

    @AfterEach
    void clear() {
        ArtifactMetadataStore.instance().setPluginInfo(null);
    }

    @Test
    void shouldImplementAbstractFetchArtifact() {
        assertThat(new FetchPluggableArtifactTask() instanceof AbstractFetchTask).isTrue();
    }

    @Test
    void validate_shouldErrorWhenReferencingConfigRepositoryPipelineFromFilePipeline() {
        uppestStream.setOrigin(new RepoConfigOrigin());
        downstream.setOrigin(new FileConfigOrigin());

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "s3");

        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().first()));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.ARTIFACT_ORIGIN)).startsWith("\"downstream :: stage :: job\" tries to fetch artifact from job \"uppest_stream :: uppest-stage2 :: uppest-job2\" which is defined in");
    }

    @Test
    void validate_shouldNotErrorWhenReferencingFilePipelineFromConfigRepositoryPipeline() {
        uppestStream.getStage("uppest-stage2").jobConfigByConfigName("uppest-job2").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        uppestStream.setOrigin(new FileConfigOrigin());
        downstream.setOrigin(new RepoConfigOrigin());

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldNotErrorWhenReferencingConfigRepositoryPipelineFromConfigRepositoryPipeline() {
        uppestStream.getStage("uppest-stage2").jobConfigByConfigName("uppest-job2").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        uppestStream.setOrigin(new RepoConfigOrigin());
        downstream.setOrigin(new RepoConfigOrigin());
        config.setArtifactStores(new ArtifactStores(new ArtifactStore("s3", "foo.plugin")));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldNotErrorWhenReferencingFilePipelineFromFilePipeline() {
        uppestStream.getStage("uppest-stage2").jobConfigByConfigName("uppest-job2").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));

        uppestStream.setOrigin(new FileConfigOrigin());
        downstream.setOrigin(new FileConfigOrigin());

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldNotTryAndValidateWhenWithinTemplate() throws Exception {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new TemplatesConfig(), downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldValidatePresenceOfartifactId() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().on("artifactId").isEmpty()).isFalse();
        assertThat(task.errors().on("artifactId")).isEqualTo("Artifact Id cannot be blank.");
    }

    @Test
    void validate_shouldValidateNullartifactId() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), null);

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().on("artifactId").isEmpty()).isFalse();
        assertThat(task.errors().on("artifactId")).isEqualTo("Artifact Id cannot be blank.");
    }

    @Test
    void validate_shouldPopulateErrorIfArtifactIsNotPresentInConfig() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");
        config.setArtifactStores(new ArtifactStores());

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().on("artifactId").isEmpty()).isFalse();
        assertThat(task.errors().on("artifactId")).isEqualTo("Pluggable artifact with id `s3` does not exist in [dummy/stage/job].");
    }

    @Test
    void validate_shouldValidateFetchPluggableArtifactConfigurationUniqueness() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3", create("Foo", false, "Bar"), create("Foo", false, "Bar"));

        task.validate(ConfigSaveValidationContext.forChain(config, new TemplatesConfig(), downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.getConfiguration().hasErrors()).isTrue();
        assertThat(task.getConfiguration().get(0).errors().on("configurationKey")).isEqualTo("Duplicate key 'Foo' found for Fetch pluggable artifact");
        assertThat(task.getConfiguration().get(1).errors().on("configurationKey")).isEqualTo("Duplicate key 'Foo' found for Fetch pluggable artifact");
    }

    @Test
    void shouldPopulateErrorsIfFetchArtifactFromPipelineThatIsNotDependency() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME)).isEqualTo("Pipeline \"downstream\" tries to fetch artifact from pipeline "
                + "\"dummy\" which is not an upstream pipeline");
        assertThat(downstream.errors().on("base")).isEqualTo("Pipeline \"downstream\" tries to fetch artifact from pipeline \"dummy\" which is not an upstream pipeline");
    }

    @Test
    void validate_shouldValidateBlankStageAndJobWhenWithinTemplate() throws Exception {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("dummy"), new CaseInsensitiveString(""), new CaseInsensitiveString(""), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new TemplatesConfig(), downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("Stage is a required field.");
        assertThat(task.errors().on(FetchTask.JOB)).isEqualTo("Job is a required field.");
    }

    @Test
    void shouldPopulateErrorsIfFetchArtifactDoesNotHaveStageAndOrJobDefined() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString(""), new CaseInsensitiveString(""), new CaseInsensitiveString(""), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, new StageConfig(), new JobConfig()));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("Stage is a required field.");
        assertThat(task.errors().on(FetchTask.JOB)).isEqualTo("Job is a required field.");
    }

    @Test
    void shouldBeValidWhenFetchArtifactIsFromAnyAncestorStage_onTheUpstreamPipeline() {
        uppestStream.getStage("uppest-stage2").jobConfigByConfigName("uppest-job2").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldBeValidWhenFetchArtifactIsFromAnyAncestorStage_s_predecessorStage__onTheUpstreamPipeline() {
        uppestStream.getStage("uppest-stage1").jobConfigByConfigName("uppest-job1").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void should_NOT_BeValidWhenFetchArtifactIsFromAnyAncestorStage_s_successorStage_onTheUpstreamPipeline() {
        uppestStream.getStage("uppest-stage3").jobConfigByConfigName("uppest-job3").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage3"), new CaseInsensitiveString("uppest-job3"), "s3");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().first()));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage \"uppest_stream :: uppest-stage3\" which does not complete before \"downstream\" pipeline's dependencies.");
    }

    @Test
    void should_NOT_BeValidWhen_pathFromAncestor_isInvalid_becauseRefferedPipelineIsNotAnAncestor() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("random_pipeline/upstream"), new CaseInsensitiveString("random-stage1"), new CaseInsensitiveString("random-job1"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME)).isEqualTo("Pipeline named 'random_pipeline' exists, but is not an ancestor of 'downstream' as declared in 'random_pipeline/upstream'.");
        assertThat(downstream.errors().on("base")).isEqualTo("Pipeline named 'random_pipeline' exists, but is not an ancestor of 'downstream' as declared in 'random_pipeline/upstream'.");
    }

    @Test
    void should_NOT_BeValidWhen_NO_pathFromAncestorIsGiven_butAncestorPipelineIsBeingFetchedFrom() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(null, new CaseInsensitiveString("uppest-stage3"), new CaseInsensitiveString("uppest-job3"), "s3");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().get(0)));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage \"downstream :: uppest-stage3\" which does not exist.");
    }

    @Test
    void should_BeValidWhen_hasAnAlternatePathToAncestor() {
        uppestStream.getStage("uppest-stage1").jobConfigByConfigName("uppest-job1").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));

        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstream"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "s3");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();

        task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "s3");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage"))));
        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void should_NOT_BeValidWhen_ImmediateParentDeclaredInPathFromAncestor_isNotAParentPipeline() {
        uppestStream.getStage("uppest-stage1").jobConfigByConfigName("uppest-job1").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("upstream/uppest_stream"), new CaseInsensitiveString("up-stage1"), new CaseInsensitiveString("up-job1"), "s3");
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

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("upstream/uppest_stream"), new CaseInsensitiveString("up-stage1"), new CaseInsensitiveString("up-job1"), "s3");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));

        task.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", config, downstream, stage, stage.getJobs().get(0)));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.PIPELINE_NAME)).isEqualTo("Pipeline named 'uppest_stream' exists, but is not an ancestor of 'downstream' as declared in 'upstream/uppest_stream'.");
        assertThat(downstream.errors().on("base")).isEqualTo("Pipeline named 'uppest_stream' exists, but is not an ancestor of 'downstream' as declared in 'upstream/uppest_stream'.");
    }

    @Test
    void should_NOT_BeValidWhen_stageMayNotHaveRunViaTheGivenPath_evenThoughItMayHaveActuallyRunAccordingToAnAlternatePath() {
        uppestStream.getStage("uppest-stage1").jobConfigByConfigName("uppest-job1").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        PipelineConfig upstreamsPeer = config.pipelineConfigByName(new CaseInsensitiveString("upstreams_peer"));
        upstreamsPeer.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("uppest_stream", "uppest-stage1")));
        upstreamsPeer.add(StageConfigMother.stageConfig("peer-stage", new JobConfigs(new JobConfig("peer-job"))));

        downstream = config.pipelineConfigByName(new CaseInsensitiveString("downstream"));
        downstream.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("upstream", "up-stage1"), MaterialConfigsMother.dependencyMaterialConfig("upstreams_peer", "peer-stage")));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage1"), new CaseInsensitiveString("uppest-job1"), "s3");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().first()));

        assertThat(task.errors().isEmpty()).isTrue();

        task = new FetchPluggableArtifactTask(new CaseInsensitiveString("uppest_stream/upstreams_peer"), new CaseInsensitiveString("uppest-stage2"), new CaseInsensitiveString("uppest-job2"), "s3");
        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().first()));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage \"uppest_stream :: uppest-stage2\" which does not complete before \"downstream\" pipeline's dependencies.");
    }

    @Test
    void shouldFailWhenFetchArtifactIsFromAnyStage_AFTER_theDependencyStageOnTheUpstreamPipeline() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("up-stage2"), new CaseInsensitiveString("up-job2"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().first()));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage \"upstream :: up-stage2\" which does not complete before \"downstream\" pipeline's dependencies.");
    }

    @Test
    void shouldPopulateErrorIfFetchArtifactFromDependentPipelineButStageDoesNotExist() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage-does-not-exist"), new CaseInsensitiveString("job"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, downstream.getStage(new CaseInsensitiveString("stage")), downstream.getStage(
                new CaseInsensitiveString("stage")), downstream.getStage(new CaseInsensitiveString("stage")).getJobs().get(0)));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.STAGE)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from stage "
                + "\"upstream :: stage-does-not-exist\" which does not exist.");
    }

    @Test
    void shouldPopulateErrorIfFetchArtifactFromDependentPipelineButJobNotExist() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job-does-not-exist"), "s3");
        StageConfig stage = downstream.getStage(new CaseInsensitiveString("stage"));

        task.validate(ConfigSaveValidationContext.forChain(config, new BasicPipelineConfigs(), downstream, stage, stage.getJobs().first()));

        assertThat(task.errors().isEmpty()).isFalse();
        assertThat(task.errors().on(FetchTask.JOB)).isEqualTo("\"downstream :: stage :: job\" tries to fetch artifact from job "
                + "\"upstream :: stage :: job-does-not-exist\" which does not exist.");
    }

    @Test
    void shouldBeValidIfFetchArtifactUsingADependantPipeline() {
        upstream.getStage("up-stage1").jobConfigByConfigName("up-job1").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("up-stage1"), new CaseInsensitiveString("up-job1"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, downstream, downstream.getStage(new CaseInsensitiveString("stage"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldBeValidIfFetchArtifactUsingAStageBeforeCurrentInTheSamePipeline() {
        upstream.getStage("stage").jobConfigByConfigName("job").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("up-stage1"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldBeValidIfFetchArtifactDoesNotSpecifyPipeline() {
        upstream.getStage("stage").jobConfigByConfigName("job").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("s3", "cd.go.s3"));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, upstream, upstream.getStage(new CaseInsensitiveString("up-stage1"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldSkipValidationOfPluggableArtifact_IsWithinTemplate() {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        task.validate(ConfigSaveValidationContext.forChain(config, new TemplatesConfig(), upstream.getStage(new CaseInsensitiveString("up-stage1"))));

        assertThat(task.errors().isEmpty()).isTrue();
    }

    @Test
    void encryptSecureProperties_shouldLeaveUserEnteredValuesAsIs() throws CryptoException {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        task.addConfigurations(configurationProperties);

        assertThat(task.getConfiguration()).isEqualTo(configurationProperties);
    }


    @Test
    void encryptSecureProperties_shouldLeaveUserEnteredValuesAsIsIfPipelineIsNotInConfig() throws CryptoException {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        task.addConfigurations(configurationProperties);
        task.encryptSecureProperties(cruiseConfig, PipelineConfigMother.pipelineConfig("p1"), task);

        assertThat(task.getConfiguration()).isEqualTo(configurationProperties);
    }

    @Test
    void encryptSecureProperties_shouldLeaveUserEnteredValuesAsIsIfStageDoesNotExist() throws CryptoException {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "s1", "j1");
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addPipelineWithoutValidation("foo", pipelineConfig);

        task.addConfigurations(configurationProperties);
        task.encryptSecureProperties(cruiseConfig, pipelineConfig, task);

        assertThat(task.getConfiguration()).isEqualTo(configurationProperties);
    }

    @Test
    void encryptSecureProperties_shouldLeaveUserEnteredValuesAsIsIfJobDoesNotExist() throws CryptoException {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "j1");
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addPipelineWithoutValidation("foo", pipelineConfig);

        task.addConfigurations(configurationProperties);
        task.encryptSecureProperties(cruiseConfig, pipelineConfig, task);

        assertThat(task.getConfiguration()).isEqualTo(configurationProperties);
    }

    @Test
    void encryptSecureProperties_shouldLeaveUserEnteredValuesAsIsIfArtifactDoesNotExist() throws CryptoException {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "job");
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addPipelineWithoutValidation("foo", pipelineConfig);

        task.addConfigurations(configurationProperties);
        task.encryptSecureProperties(cruiseConfig, pipelineConfig, task);

        assertThat(task.getConfiguration()).isEqualTo(configurationProperties);
    }

    @Test
    void encryptSecureProperties_shouldLeaveUserEnteredValuesAsIsIfArtifactStoreIsMissing() throws CryptoException {
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "job");
        pipelineConfig.getStage("stage").jobConfigByConfigName("job").artifactTypeConfigs().add(new PluggableArtifactConfig("s3", "aws"));

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addPipelineWithoutValidation("foo", pipelineConfig);

        task.addConfigurations(configurationProperties);
        task.encryptSecureProperties(cruiseConfig, pipelineConfig, task);

        assertThat(task.getConfiguration()).isEqualTo(configurationProperties);
    }

    @Test
    void encryptSecureProperties_shouldLeaveUserEnteredValuesAsIsIfPluginIsMissing() throws CryptoException {
        ArtifactMetadataStore.instance().remove("cd.go.s3");
        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "job");
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("s3", "aws");
        pipelineConfig.getStage("stage").jobConfigByConfigName("job").artifactTypeConfigs().add(pluggableArtifactConfig);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addPipelineWithoutValidation("foo", pipelineConfig);

        task.addConfigurations(configurationProperties);
        task.encryptSecureProperties(cruiseConfig, pipelineConfig, task);

        assertThat(task.getConfiguration()).isEqualTo(configurationProperties);
    }

    @Test
    void encryptSecureProperties_shouldEncryptSecureProperties() throws CryptoException {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        pluginConfigurations.add(new PluginConfiguration("secure_property1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("secure_property2", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("plain", new Metadata(true, false)));
        when(artifactPluginInfo.getFetchArtifactSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "job");
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("s3", "aws");
        pipelineConfig.getStage("stage").jobConfigByConfigName("job").artifactTypeConfigs().add(pluggableArtifactConfig);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addPipelineWithoutValidation("foo", pipelineConfig);
        cruiseConfig.getArtifactStores().add(new ArtifactStore("aws", artifactPluginInfo.getDescriptor().id()));


        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("plain"), new ConfigurationValue("plain")));
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property1"), new ConfigurationValue("password")));
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property2"), new EncryptedConfigurationValue(new GoCipher().encrypt("secret"))));

        ArrayList<ConfigurationProperty> expectedConfigurationProperties = new ArrayList<>();
        expectedConfigurationProperties.add(new ConfigurationProperty(new ConfigurationKey("plain"), new ConfigurationValue("plain")));
        expectedConfigurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property1"), new EncryptedConfigurationValue(new GoCipher().encrypt("password"))));
        expectedConfigurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property2"), new EncryptedConfigurationValue(new GoCipher().encrypt("secret"))));


        task.addConfigurations(configurationProperties);
        task.encryptSecureProperties(cruiseConfig, pipelineConfig, task);

        assertThat(task.getConfiguration().size()).isEqualTo(3);
        assertThat(task.getConfiguration()).isEqualTo(expectedConfigurationProperties);
    }

    @Test
    void encryptSecureProperties_shouldEncryptSecurePropertiesIfTheConfigIdentifersAreParams() throws CryptoException {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        pluginConfigurations.add(new PluginConfiguration("secure_property1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("secure_property2", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("plain", new Metadata(true, false)));
        when(artifactPluginInfo.getFetchArtifactSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "job");
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("s3", "aws");
        pipelineConfig.getStage("stage").jobConfigByConfigName("job").artifactTypeConfigs().add(pluggableArtifactConfig);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addPipelineWithoutValidation("foo", pipelineConfig);
        cruiseConfig.getArtifactStores().add(new ArtifactStore("aws", artifactPluginInfo.getDescriptor().id()));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("#{pipeline}"), new CaseInsensitiveString("#{stage}"), new CaseInsensitiveString("#{job}"), "#{artifactId}");
        FetchPluggableArtifactTask preprocessedTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "s3");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("plain"), new ConfigurationValue("plain")));
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property1"), new ConfigurationValue("password")));
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property2"), new EncryptedConfigurationValue(new GoCipher().encrypt("secret"))));

        ArrayList<ConfigurationProperty> expectedConfigurationProperties = new ArrayList<>();
        expectedConfigurationProperties.add(new ConfigurationProperty(new ConfigurationKey("plain"), new ConfigurationValue("plain")));
        expectedConfigurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property1"), new EncryptedConfigurationValue(new GoCipher().encrypt("password"))));
        expectedConfigurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property2"), new EncryptedConfigurationValue(new GoCipher().encrypt("secret"))));


        task.addConfigurations(configurationProperties);
        PipelineConfig pipelineWhichHasTheFetchTask = PipelineConfigMother.createPipelineConfigWithStage("p2", "anotherStage");
        pipelineWhichHasTheFetchTask.first().getJobs().first().addTask(task);
        pipelineWhichHasTheFetchTask.setParams(new ParamsConfig(new ParamConfig("pipeline", "pipeline"), new ParamConfig("stage", "stage"), new ParamConfig("job", "job"), new ParamConfig("artifactId", "s3")));
        task.encryptSecureProperties(cruiseConfig, pipelineWhichHasTheFetchTask, preprocessedTask);

        assertThat(task.getConfiguration().size()).isEqualTo(3);
        assertThat(task.getConfiguration()).isEqualTo(expectedConfigurationProperties);
    }

    @Test
    void shouldSetConfiguration_whenPluginIsProvided() {
        final HashMap<Object, Object> configAttrs = new HashMap<>();
        configAttrs.put(FetchPluggableArtifactTask.ARTIFACT_ID, "installers");
        configAttrs.put(FetchPluggableArtifactTask.CONFIGURATION, Collections.singletonMap("NAME", "gocd.zip"));
        configAttrs.put("pluginId", "cd.go.artifact.s3");

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("#{pipeline}"), new CaseInsensitiveString("#{stage}"), new CaseInsensitiveString("#{job}"), "#{artifactId}");
        task.setFetchTaskAttributes(configAttrs);

        Assertions.assertThat(task.getArtifactId()).isEqualTo("installers");
        Assertions.assertThat(task.getConfiguration())
                .hasSize(1)
                .contains(new ConfigurationProperty(new ConfigurationKey("NAME"), new ConfigurationValue("gocd.zip")));
    }

    @Test
    void shouldSetConfiguration_whenPluginIsNotProvided() throws CryptoException {
        final HashMap<Object, Object> configAttrs = new HashMap<>();
        configAttrs.put(FetchPluggableArtifactTask.ARTIFACT_ID, "installers");
        configAttrs.put(FetchPluggableArtifactTask.CONFIGURATION, Collections.singletonMap("NAME", new HashMap<String, String>() {{
            put("value", new GoCipher().encrypt("gocd.zip"));
            put("isSecure", "true");
        }}));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("#{pipeline}"), new CaseInsensitiveString("#{stage}"), new CaseInsensitiveString("#{job}"), "#{artifactId}");
        task.setFetchTaskAttributes(configAttrs);

        Assertions.assertThat(task.getArtifactId()).isEqualTo("installers");
        Assertions.assertThat(task.getConfiguration())
                .hasSize(1)
                .contains(new ConfigurationProperty(new ConfigurationKey("NAME"), new EncryptedConfigurationValue(new GoCipher().encrypt("gocd.zip"))));
    }

    @Test
    void shouldNotSetConfigurationWhenArtifactIdIsNotProvided() {
        final HashMap<Object, Object> configAttrs = new HashMap<>();
        configAttrs.put(FetchPluggableArtifactTask.ARTIFACT_ID, "");
        configAttrs.put(FetchPluggableArtifactTask.CONFIGURATION, Collections.singletonMap("NAME", "gocd.zip"));

        FetchPluggableArtifactTask task = new FetchPluggableArtifactTask(new CaseInsensitiveString("#{pipeline}"), new CaseInsensitiveString("#{stage}"), new CaseInsensitiveString("#{job}"), "#{artifactId}");
        task.setFetchTaskAttributes(configAttrs);

        Assertions.assertThat(task.getArtifactId()).isEmpty();
        Assertions.assertThat(task.getConfiguration()).isEmpty();
    }
}
