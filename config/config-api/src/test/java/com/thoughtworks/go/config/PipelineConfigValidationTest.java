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
package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.p4;
import static com.thoughtworks.go.util.TestUtils.contains;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PipelineConfigValidationTest {
    private CruiseConfig config;
    private PipelineConfig pipeline;
    private GoConfigMother goConfigMother;

    @Before
    public void setup() {
        config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2", "pipeline3", "go");
        pipeline = config.pipelineConfigByName(new CaseInsensitiveString("go"));
        goConfigMother = new GoConfigMother();
    }

    @Test
    public void shouldEnsureStageNameUniqueness() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.initializeServer();
        PipelineConfig pipelineConfig = goConfigMother.addPipeline(cruiseConfig, "pipeline1", "stage", "build");
        JobConfig jobConfig = new JobConfig("my-build");
        jobConfig.addTask(new ExecTask("ls", "-la", "tmp"));
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig));
        pipelineConfig.addStageWithoutValidityAssertion(stageConfig);
        pipelineConfig.validate(null);

        assertThat(stageConfig.errors().getAllOn("name"),
                is(singletonList("You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique.")));

        assertThat(pipelineConfig.get(0).errors().getAllOn("name"),
                is(singletonList("You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique.")));

        assertThat(cruiseConfig.validateAfterPreprocess().get(0).getAllOn("name"),
                is(singletonList("You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique.")));
    }

    @Test
    public void rejectsLabelTemplateWithMissingMaterial() {
        assertLabelTemplate("foo-${[:5]}-bar", errors -> {
            assertEquals(singletonList("You have defined a label template in pipeline 'go' that refers to a material called '', but no material with this name is defined."), errors);
        });
    }

    @Test
    public void rejectsLabelTemplateWithBadTruncation() {
        assertLabelTemplate("foo-${material[:5}-bar", errors -> {
            assertEquals(1, errors.size());
            assertThat(errors.get(0), Matchers.startsWith("Invalid label"));
        });
    }

    @Test
    public void rejectsLabelTemplateWithBlankToken() {
        assertLabelTemplate("foo-${}-bar", errors ->
                assertEquals(singletonList("Label template variable cannot be blank."), errors));
    }

    @Test
    public void rejectsLabelTemplateWithMissingEnvironmentVariable() {
        assertLabelTemplate("foo-${env:}-bar", errors ->
                assertEquals(singletonList("Missing environment variable name."), errors));

        assertLabelTemplate("foo-${ENV:}-bar", errors ->
                assertEquals(singletonList("Missing environment variable name."), errors));
    }

    @Test
    public void acceptsLabelTemplateWithEnvironmentVariables() {
        assertLabelTemplate("release-${ENV:TEST}-${COUNT}", Assert::assertNull);
        assertLabelTemplate("release-${env:TeSt}-${COUNT}", Assert::assertNull);
    }

    @Test
    public void isValid_shouldEnsureLabelTemplateRefersToValidMaterials() {
        assertLabelTemplate("pipeline-${COUNT}-${myGit}", errors ->
                assertEquals(singletonList("You have defined a label template in pipeline 'go' that refers to a material called 'myGit', but no material with this name is defined."), errors));
    }

    @Test
    public void isValid_shouldEnsureLabelTemplateRefersToAMaterialOrCOUNT() {
        assertLabelTemplate("label-template-without-material-or-count", errors -> {
            assertEquals(1, errors.size());
            assertThat(errors.get(0), Matchers.startsWith("Invalid label"));
        });
    }

    @Test
    public void isValid_shouldEnsureLabelTemplateHasValidVariablePattern() {
        assertLabelTemplate("pipeline-${COUNT", errors -> {
            assertEquals(1, errors.size());
            assertThat(errors.get(0), Matchers.startsWith("Invalid label"));
        });
    }

    @Test
    public void isValid_labelTemplateShouldAcceptLabelTemplateWithHashCharacter() {
        assertLabelTemplate("foo${COUNT}-tanker#", Assert::assertNull);
    }

    @Test
    public void isValid_shouldMatchMaterialNamesInACaseInsensitiveManner() {
        ScmMaterialConfig gitMaterialConfig = MaterialConfigsMother.gitMaterialConfig("git://url");
        gitMaterialConfig.setName(new CaseInsensitiveString("mygit"));
        pipeline.addMaterialConfig(gitMaterialConfig);

        assertLabelTemplate("pipeline-${count}-${myGit}", errors -> {
            assertTrue(pipeline.errors().isEmpty());
            assertNull(errors);
        });
    }

    @Test
    public void isValid_shouldEnsureReturnsTrueWhenLabelTemplateRefersToValidMaterials() {
        GitMaterialConfig gitConfig = MaterialConfigsMother.gitMaterialConfig("git://url");
        gitConfig.setName(new CaseInsensitiveString("myGit"));
        pipeline.addMaterialConfig(gitConfig);

        assertLabelTemplate("pipeline-${COUNT}-${myGit}", errors -> {
            assertTrue(pipeline.errors().isEmpty());
            assertNull(errors);
        });
    }

    @Test
    public void isValid_shouldAllowColonForLabelTemplate() {
        pipeline.addMaterialConfig(new PackageMaterialConfig(new CaseInsensitiveString("repo:name"), "package-id", PackageDefinitionMother.create("package-id")));
        assertLabelTemplate("pipeline-${COUNT}-${repo:name}", Assert::assertNull);
    }

    @Test
    public void validate_shouldEnsureThatTemplateFollowsTheNameType() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("name"), new MaterialConfigs());
        pipelineConfig.setTemplateName(new CaseInsensitiveString(".Name"));
        config.addPipeline("group", pipelineConfig);
        pipelineConfig.validateTemplate(new PipelineTemplateConfig());
        assertThat(pipelineConfig.errors().isEmpty(), is(false));
        assertThat(pipelineConfig.errors().on(PipelineConfig.TEMPLATE_NAME), is("Invalid template name '.Name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void validate_shouldEnsureThatPipelineFollowsTheNameType() {
        PipelineConfig config = new PipelineConfig(new CaseInsensitiveString(".name"), new MaterialConfigs());
        config.validate(null);
        assertThat(config.errors().isEmpty(), is(false));
        assertThat(config.errors().on(PipelineConfig.NAME), is("Invalid pipeline name '.name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldBeValidIfTheReferencedPipelineExists() {
        pipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage")));
        pipeline.validate(null);
        assertThat(pipeline.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowMultipleDependenciesForDifferentPipelines() {
        pipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage")));
        pipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline3"), new CaseInsensitiveString("stage")));
        pipeline.validate(null);
        assertThat(pipeline.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowDependenciesFromMultiplePipelinesToTheSamePipeline() {
        pipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage")));

        PipelineConfig pipeline3 = config.pipelineConfigByName(new CaseInsensitiveString("pipeline3"));
        pipeline3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage")));

        pipeline.validate(null);
        assertThat(pipeline.errors().isEmpty(), is(true));
        pipeline3.validate(null);
        assertThat(pipeline3.errors().isEmpty(), is(true));

    }

    @Test
    public void shouldNotAllowAnEmptyView() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        P4MaterialConfig materialConfig = p4("localhost:1666", "");

        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.addMaterialConfig(materialConfig);
        materialConfig.validate(null);
        assertThat(materialConfig.errors().on("view"), is("P4 view cannot be empty."));
    }

    @Test
    public void shouldValidateAndUpdatePipelineConfig() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("validPipeline");
        pipeline.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.gitMaterialConfig(), MaterialConfigsMother.svnMaterialConfig()));
        StageConfig stage1 = getStageConfig("stage1", "s1j1");
        StageConfig stage2 = getStageConfig("stage2", "s2j1");
        pipeline.getStages().add(stage1);
        pipeline.getStages().add(stage2);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs("group", new Authorization(), pipeline));
        boolean isValid = pipeline.validateTree(PipelineConfigSaveValidationContext.forChain(true, cruiseConfig.getGroups().first().getGroup(), cruiseConfig, pipeline));
        assertThat(isValid, is(true));
        assertThat(pipeline.materialConfigs().errors().isEmpty(), is(true));
        assertThat(pipeline.materialConfigs().get(0).errors().isEmpty(), is(true));
        assertThat(pipeline.materialConfigs().get(1).errors().isEmpty(), is(true));
        assertThat(pipeline.errors().getAll().isEmpty(), is(true));
    }

    @Test
    public void shouldHandleNullStageNamesWhileValidating() {
        StageConfig s1 = new StageConfig();
        StageConfig s2 = new StageConfig(new CaseInsensitiveString("s2"), new JobConfigs());
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString("p1"), new MaterialConfigs(), s1, s2);
        pipeline.validate(null);
        assertThat(s1.errors().on(StageConfig.NAME).contains("Invalid stage name 'null'"), is(true));
    }

    @Test
    public void shouldValidateTree() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("pipeline");
        pipeline.addEnvironmentVariable("", "");
        pipeline.addParam(new ParamConfig("", ""));
        pipeline.setMaterialConfigs(new MaterialConfigs(MaterialConfigsMother.gitMaterialConfig(), MaterialConfigsMother.svnMaterialConfig()));
        StageConfig stage1 = getStageConfig("stage1", "s1j1");
        StageConfig stage2 = getStageConfig("stage2", "s2j1");
        pipeline.getStages().add(stage1);
        pipeline.getStages().add(stage2);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs("group", new Authorization(), pipeline));
        boolean isValid = pipeline.validateTree(PipelineConfigSaveValidationContext.forChain(true, cruiseConfig.getGroups().first().getGroup(), cruiseConfig, pipeline));
        assertThat(isValid, is(false));
        assertThat(pipeline.getVariables().get(0).errors().firstError(), is("Environment Variable cannot have an empty name for pipeline 'pipeline'."));
        assertThat(pipeline.getParams().get(0).errors().firstError(), is("Parameter cannot have an empty name for pipeline 'pipeline'."));
        assertThat(pipeline.materialConfigs().errors().isEmpty(), is(true));
        assertThat(pipeline.materialConfigs().get(0).errors().isEmpty(), is(true));
        assertThat(pipeline.materialConfigs().get(1).errors().isEmpty(), is(true));
        assertThat(pipeline.errors().getAll().isEmpty(), is(true));
    }

    @Test
    public void shouldFailValidateWhenUpstreamPipelineForDependencyMaterialDoesNotExist() {
        String upstreamPipeline = "non-existant";
        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(
                new DependencyMaterialConfig(new CaseInsensitiveString(upstreamPipeline), new CaseInsensitiveString("non-existant")));

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig));
        boolean isValid = pipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, cruiseConfig.getGroups().first().getGroup(), cruiseConfig, pipelineConfig));
        assertThat(isValid, is(false));

        ConfigErrors materialErrors = pipelineConfig.materialConfigs().first().errors();
        assertThat(materialErrors.isEmpty(), is(false));
        assertThat(materialErrors.firstError(), is("Pipeline with name 'non-existant' does not exist, it is defined as a dependency for pipeline 'pipeline' (cruise-config.xml)"));
    }

    @Test
    public void shouldFailValidateWhenUpstreamStageForDependencyMaterialDoesNotExist() {
        String upstreamPipeline = "upstream";
        String upstreamStage = "non-existant";
        PipelineConfig upstream = GoConfigMother.createPipelineConfigWithMaterialConfig(upstreamPipeline, git("url"));
        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig("downstream",
                new DependencyMaterialConfig(new CaseInsensitiveString(upstreamPipeline), new CaseInsensitiveString(upstreamStage)));
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig, upstream));
        boolean isValid = pipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, cruiseConfig.getGroups().first().getGroup(), cruiseConfig, pipelineConfig));
        assertThat(isValid, is(false));
        ConfigErrors materialErrors = pipelineConfig.materialConfigs().first().errors();
        assertThat(materialErrors.isEmpty(), is(false));
        assertThat(materialErrors.firstError(), is("Stage with name 'non-existant' does not exist on pipeline 'upstream', it is being referred to from pipeline 'downstream' (cruise-config.xml)"));
    }

    @Test
    public void shouldReturnTrueIfAllDescendentsAreValid() {
        StageConfig stageConfig = mock(StageConfig.class);
        MaterialConfigs materialConfigs = mock(MaterialConfigs.class);
        ParamsConfig paramsConfig = mock(ParamsConfig.class);
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        TrackingTool trackingTool = mock(TrackingTool.class);
        TimerConfig timerConfig = mock(TimerConfig.class);
        when(stageConfig.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(materialConfigs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(paramsConfig.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(variables.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(trackingTool.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(timerConfig.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p1"), materialConfigs, stageConfig);
        pipelineConfig.setParams(paramsConfig);
        pipelineConfig.setVariables(variables);
        pipelineConfig.setTrackingTool(trackingTool);
        pipelineConfig.setTimer(timerConfig);

        boolean isValid = pipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(new BasicPipelineConfigs("group", new Authorization())), pipelineConfig));
        assertTrue(isValid);
        verify(stageConfig).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(materialConfigs).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(paramsConfig).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(variables).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(trackingTool).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(timerConfig).validateTree(any(PipelineConfigSaveValidationContext.class));
    }

    @Test
    public void shouldReturnFalseIfAnyDescendentIsInValid() {
        StageConfig stageConfig = mock(StageConfig.class);
        MaterialConfigs materialConfigs = mock(MaterialConfigs.class);
        when(materialConfigs.iterator()).thenReturn(new MaterialConfigs().iterator());
        ParamsConfig paramsConfig = mock(ParamsConfig.class);
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        TrackingTool trackingTool = mock(TrackingTool.class);
        TimerConfig timerConfig = mock(TimerConfig.class);
        when(stageConfig.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(materialConfigs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(paramsConfig.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(variables.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(trackingTool.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(timerConfig.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p1"), materialConfigs, stageConfig);
        pipelineConfig.setParams(paramsConfig);
        pipelineConfig.setVariables(variables);
        pipelineConfig.setTrackingTool(trackingTool);
        pipelineConfig.setTimer(timerConfig);

        boolean isValid = pipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(new BasicPipelineConfigs("group", new Authorization())), pipelineConfig));
        assertFalse(isValid);
        verify(stageConfig).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(materialConfigs).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(paramsConfig).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(variables).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(trackingTool).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(timerConfig).validateTree(any(PipelineConfigSaveValidationContext.class));
    }

    @Test
    public void shouldValidateAPipelineHasAtleastOneStage() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(new BasicPipelineConfigs("group", new Authorization())), pipelineConfig));
        assertThat(pipelineConfig.errors().on("pipeline"), is("Pipeline 'p' does not have any stages configured. A pipeline must have at least one stage."));
    }

    @Test
    public void shouldDetectCyclicDependencies() {
        String pipelineName = "p1";
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines(pipelineName, "p2", "p3");
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString("stage")));
        PipelineConfig p3 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p3"));
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"), new CaseInsensitiveString("stage")));

        PipelineConfig p1 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString(pipelineName));
        p1 = new Cloner().deepClone(p1); // Do not remove cloning else it changes the underlying cache object defeating the purpose of the test.
        p1.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p3"), new CaseInsensitiveString("stage")));
        p1.validateTree(PipelineConfigSaveValidationContext.forChain(true, cruiseConfig.getGroups().first().getGroup(), cruiseConfig, p1));
        assertThat(p1.materialConfigs().errors().isEmpty(), is(false));
        assertThat(p1.materialConfigs().errors().on("base"), is("Circular dependency: p1 <- p2 <- p3 <- p1"));
    }

    @Test
    public void shouldValidateThatPipelineAssociatedToATemplateDoesNotHaveStagesDefinedLocally() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("wunderbar"), new MaterialConfigs());
        config.addPipeline("g", pipelineConfig);
        pipelineConfig.setTemplateName(new CaseInsensitiveString("template-name"));
        pipelineConfig.addStageWithoutValidityAssertion(new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs()));
        pipelineConfig.validateTemplate(null);
        assertThat(pipelineConfig.errors().on("stages"), is("Cannot add stages to pipeline 'wunderbar' which already references template 'template-name'"));
        assertThat(pipelineConfig.errors().on("template"), is("Cannot set template 'template-name' on pipeline 'wunderbar' because it already has stages defined"));
    }

    @Test
    public void shouldAddValidationErrorWhenAssociatedTemplateDoesNotExist() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("wunderbar"), new MaterialConfigs());
        config.addPipeline("group", pipelineConfig);
        pipelineConfig.setTemplateName(new CaseInsensitiveString("does-not-exist"));
        pipelineConfig.validateTemplate(null);
        assertThat(pipelineConfig.errors().on("pipeline"), is("Pipeline 'wunderbar' refers to non-existent template 'does-not-exist'."));
    }

    @Test
    public void shouldNotAddValidationErrorWhenAssociatedTemplateExists() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("wunderbar"), new MaterialConfigs());
        config.addPipeline("group", pipelineConfig);
        config.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("t1")));
        pipelineConfig.setTemplateName(new CaseInsensitiveString("t1"));
        pipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", config, pipelineConfig));
        assertThat(pipelineConfig.errors().getAllOn("template"), is(nullValue()));
    }

    @Test
    public void shouldFailValidationIfAStageIsDeletedWhileItsStillReferredToByADownstreamPipeline() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2");
        PipelineConfig p1 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p1"));
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        p2.addMaterialConfig(new DependencyMaterialConfig(p1.name(), p1.first().name()));

        String group = cruiseConfig.getGroups().first().getGroup();
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("s1"), new JobConfigs(new JobConfig(new CaseInsensitiveString("j1"))));
        PipelineConfig pipelineConfig = new PipelineConfig(p1.name(), new MaterialConfigs(), stageConfig);
        cruiseConfig.update(group, pipelineConfig.name().toString(), pipelineConfig);
        PipelineConfigSaveValidationContext validationContext = PipelineConfigSaveValidationContext.forChain(false, group, cruiseConfig, pipelineConfig);

        pipelineConfig.validateTree(validationContext);
        assertThat(pipelineConfig.errors().on("base"), is("Stage with name 'stage' does not exist on pipeline 'p1', it is being referred to from pipeline 'p2' (cruise-config.xml)"));
    }

    @Test
    public void shouldFailValidationIfAJobIsDeletedWhileItsStillReferredToByADescendentPipelineThroughFetchArtifact() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2", "p3");
        PipelineConfig p1 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p1"));
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        PipelineConfig p3 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p3"));
        p2.addMaterialConfig(new DependencyMaterialConfig(p1.name(), p1.first().name()));
        JobConfig p2S2J2 = new JobConfig(new CaseInsensitiveString("j2"));
        p2S2J2.addTask(fetchTaskFromSamePipeline(p2));
        p2.add(new StageConfig(new CaseInsensitiveString("stage2"), new JobConfigs(p2S2J2)));
        p3.addMaterialConfig(new DependencyMaterialConfig(p2.name(), p2.first().name()));
        p3.first().getJobs().first().addTask(new FetchTask(new CaseInsensitiveString("p1/p2"), p1.first().name(), p1.first().getJobs().first().name(), "src", "dest"));

        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig(new CaseInsensitiveString("new-job"))));
        PipelineConfig pipelineConfig = new PipelineConfig(p1.name(), new MaterialConfigs(), stageConfig);
        String group = cruiseConfig.getGroups().first().getGroup();
        cruiseConfig.update(group, pipelineConfig.name().toString(), pipelineConfig);
        PipelineConfigSaveValidationContext validationContext = PipelineConfigSaveValidationContext.forChain(false, group, cruiseConfig, pipelineConfig);

        pipelineConfig.validateTree(validationContext);
        assertThat(pipelineConfig.errors().on("base"), is("\"p3 :: stage :: job\" tries to fetch artifact from job \"p1 :: stage :: job\" which does not exist."));
    }

    private FetchTask fetchTaskFromSamePipeline(PipelineConfig pipelineConfig) {
        FetchTask fetchTask = new FetchTask();
        fetchTask.setStage(pipelineConfig.first().name());
        fetchTask.setJob(pipelineConfig.first().getJobs().first().name());
        return fetchTask;
    }

    @Test
    public void shouldAddValidationErrorsFromStagesOntoPipelineIfPipelineIsAssociatedToATemplate() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2", "p3");
        PipelineConfig p1 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p1"));
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        PipelineConfig p3 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p3"));
        p2.addMaterialConfig(new DependencyMaterialConfig(p1.name(), p1.first().name()));
        p3.addMaterialConfig(new DependencyMaterialConfig(p2.name(), p2.first().name()));
        p3.first().getJobs().first().addTask(new FetchTask(new CaseInsensitiveString("p1/p2"), p1.first().name(), p1.first().getJobs().first().name(), "src", "dest"));

        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig(new CaseInsensitiveString("new-job"))));
        PipelineConfig pipelineConfig = new PipelineConfig(p1.name(), new MaterialConfigs(), stageConfig);
        String group = cruiseConfig.getGroups().first().getGroup();
        cruiseConfig.update(group, pipelineConfig.name().toString(), pipelineConfig);
        PipelineConfigSaveValidationContext validationContext = PipelineConfigSaveValidationContext.forChain(false, group, cruiseConfig, pipelineConfig);

        pipelineConfig.validateTree(validationContext);
        assertThat(pipelineConfig.errors().on("base"), is("\"p3 :: stage :: job\" tries to fetch artifact from job \"p1 :: stage :: job\" which does not exist."));
    }

    @Test
    public void shouldCheckForPipelineNameUniqueness() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");
        String group = "group";
        cruiseConfig.getGroups().add(new BasicPipelineConfigs(group, new Authorization()));
        PipelineConfig p1Duplicate = GoConfigMother.createPipelineConfigWithMaterialConfig("p1", git("url"));
        cruiseConfig.addPipeline(group, p1Duplicate);
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, group, cruiseConfig, p1Duplicate);
        p1Duplicate.validateTree(context);
        assertThat(p1Duplicate.errors().on(PipelineConfig.NAME), is(String.format("You have defined multiple pipelines named '%s'. Pipeline names must be unique. Source(s): [cruise-config.xml]", p1Duplicate.name())));
    }

    @Test
    public void shouldValidateGroupNameWhenPipelineIsBeingCreatedUnderANonExistantGroup() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1");

        PipelineConfig p1 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p1"));
        String groupName = "%$-with-invalid-characters";
        cruiseConfig.addPipeline(groupName, p1);
        p1.validateTree(PipelineConfigSaveValidationContext.forChain(true, groupName, cruiseConfig, p1));

        assertFalse(p1.errors().isEmpty());
        assertThat(p1.errors().on(PipelineConfigs.GROUP), contains("Invalid group name '%$-with-invalid-characters'"));
    }


    private StageConfig getStageConfig(String stageName, String jobName) {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString(jobName));
        jobConfig.addTask(new AntTask());
        jobConfig.addTask(new ExecTask("command", "", "workingDir"));
        jobConfig.artifactTypeConfigs().add(new BuildArtifactConfig("src", "dest"));
        jobConfig.addVariable("env1", "val1");
        jobConfig.addResourceConfig("powerful");
        JobConfigs jobConfigs = new JobConfigs(jobConfig);
        return new StageConfig(new CaseInsensitiveString(stageName), jobConfigs);
    }

    private void assertLabelTemplate(String labelTemplate, LabelErrorsFn assertions) {
        pipeline.setLabelTemplate(labelTemplate);
        pipeline.validate(null);
        List<String> errors = pipeline.errors().getAllOn("labelTemplate");
        assertions.asserting(errors);
    }

    @FunctionalInterface
    private interface LabelErrorsFn {

        void asserting(List<String> errors);
    }
}
