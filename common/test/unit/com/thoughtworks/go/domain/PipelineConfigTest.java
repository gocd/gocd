/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.Node;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class PipelineConfigTest {
    private static final String BUILDING_PLAN_NAME = "building";

    EnvironmentVariablesConfig mockEnvironmentVariablesConfig = mock(EnvironmentVariablesConfig.class);
    ParamsConfig mockParamsConfig = mock(ParamsConfig.class);

    public enum Foo {
        Bar, Baz;
    }

    @Test
    public void shouldFindByName() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.findBy(new CaseInsensitiveString("completed stage")).name(), is(new CaseInsensitiveString("completed stage")));
    }

    @Test
    public void shouldReturnDuplicateWithoutName() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("somePipeline");
        PipelineConfig clonedPipelineConfig = pipelineConfig.duplicate();
        assertThat(clonedPipelineConfig.name(), is(new CaseInsensitiveString("")));
        assertThat(clonedPipelineConfig.materialConfigs(), is(pipelineConfig.materialConfigs()));
        assertThat(clonedPipelineConfig.getFirstStageConfig(), is(pipelineConfig.getFirstStageConfig()));
    }

    @Test
    public void shouldReturnDuplicateWithPipelineNameEmptyIfFetchArtifactTaskIsFetchingFromSamePipeline() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("somePipeline", "stage", "job");
        StageConfig stageConfig = pipelineConfig.get(0);
        JobConfig jobConfig = stageConfig.getJobs().get(0);
        Tasks originalTasks = jobConfig.getTasks();
        originalTasks.add(new FetchTask(pipelineConfig.name(), stageConfig.name(), jobConfig.name(), "src", "dest"));
        originalTasks.add(new FetchTask(new CaseInsensitiveString("some_other_pipeline"), stageConfig.name(), jobConfig.name(), "src", "dest"));
        PipelineConfig clone = pipelineConfig.duplicate();
        Tasks clonedTasks = clone.get(0).getJobs().get(0).getTasks();
        assertThat(((FetchTask) clonedTasks.get(0)).getTargetPipelineName(), is(new CaseInsensitiveString("")));
        assertThat(((FetchTask) clonedTasks.get(1)).getTargetPipelineName(), is(new CaseInsensitiveString("some_other_pipeline")));
        assertThat(((FetchTask) originalTasks.get(0)).getTargetPipelineName(), is(pipelineConfig.name()));
    }

    @Test //#6821
    public void shouldCopyOverAllEnvironmentVariablesWhileCloningAPipeline() throws InvalidCipherTextException {
        PipelineConfig source = PipelineConfigMother.createPipelineConfig("somePipeline", "stage", "job");
        source.addEnvironmentVariable("k1", "v1");
        source.addEnvironmentVariable("k2", "v2");
        GoCipher goCipher = mock(GoCipher.class);
        when(goCipher.encrypt("secret")).thenReturn("encrypted");
        source.addEnvironmentVariable(new EnvironmentVariableConfig(goCipher, "secret_key", "secret", true));

        PipelineConfig cloned = source.duplicate();
        EnvironmentVariablesConfig clonedEnvVariables = cloned.getPlainTextVariables();
        EnvironmentVariablesConfig sourceEnvVariables = source.getPlainTextVariables();
        assertThat(clonedEnvVariables.size(), is(sourceEnvVariables.size()));
        clonedEnvVariables.getPlainTextVariables().containsAll(sourceEnvVariables.getPlainTextVariables());
        assertThat(cloned.getSecureVariables().size(), is(source.getSecureVariables().size()));
        assertThat(cloned.getSecureVariables().containsAll(source.getSecureVariables()), is(true));
    }

    @Test
    public void shouldGetStageByName() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), (MaterialConfigs) null, completedStage(), buildingStage());
        assertThat(pipelineConfig.getStage(new CaseInsensitiveString("COMpleTEd stage")).name(), is(new CaseInsensitiveString("completed stage")));
        assertThat(pipelineConfig.getStage(new CaseInsensitiveString("Does-not-exist")), is(nullValue()));
    }

    @Test
    public void shouldReturnFalseIfThereIsNoNextStage() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), (MaterialConfigs) null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(buildingStage().name()), is(false));
    }

    @Test
    public void shouldReturnFalseIfThereIsNextStage() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), (MaterialConfigs) null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(completedStage().name()), is(true));
    }

    @Test
    public void shouldReturnFalseThePassInStageDoesNotExist() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), (MaterialConfigs) null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(new CaseInsensitiveString("notExist")), is(false));
    }

    @Test
    public void shouldReturnTrueIfThereNoStagesDefined() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), (MaterialConfigs) null);
        assertThat(pipelineConfig.hasNextStage(completedStage().name()), is(false));
    }

    @Test
    public void shouldGetDependenciesAsNode() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("framework"), new CaseInsensitiveString("dev")));
        pipelineConfig.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("middleware"), new CaseInsensitiveString("dev")));
        assertThat(pipelineConfig.getDependenciesAsNode(),
                is(new Node(
                        new Node.DependencyNode(new CaseInsensitiveString("framework"), new CaseInsensitiveString("dev")),
                        new Node.DependencyNode(new CaseInsensitiveString("middleware"), new CaseInsensitiveString("dev")))));
    }

    @Test
    public void shouldReturnTrueIfFirstStageIsManualApproved() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        pipelineConfig.getFirstStageConfig().updateApproval(Approval.manualApproval());
        assertThat("First stage should be manual approved", pipelineConfig.isFirstStageManualApproval(), is(true));
    }

    @Test
    public void shouldThrowExceptionForEmptyPipeline() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs());
        try {
            pipelineConfig.isFirstStageManualApproval();
            fail("Should throw exception if pipeline has no pipeline");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(),
                    containsString("Pipeline [" + pipelineConfig.name() + "] doesn't have any stage"));
        }
    }

    @Test
    public void shouldThrowExceptionOnAddingTemplatesIfItAlreadyHasStages() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        try {
            PipelineTemplateConfig template = new PipelineTemplateConfig();
            template.add(StageConfigMother.stageConfig("first"));
            pipelineConfig.setTemplateName(new CaseInsensitiveString("some-template"));
            fail("Should throw exception because the pipeline has stages already");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Cannot set template 'some-template' on pipeline 'pipeline' because it already has stages defined"));
        }
    }

    @Test
    public void shouldBombWhenAddingStagesIfItAlreadyHasATemplate() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("mingle"), (MaterialConfigs) null);
        try {
            pipelineConfig.setTemplateName(new CaseInsensitiveString("some-template"));
            pipelineConfig.add(StageConfigMother.stageConfig("second"));
            fail("Should throw exception because pipeline already has a template");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Cannot add stage 'second' to pipeline 'mingle', which already references template 'some-template'."));
        }
    }

    @Test
    public void shouldKnowIfATemplateWasApplied() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        assertThat(pipelineConfig.hasTemplateApplied(), is(false));
        pipelineConfig.clear();

        PipelineTemplateConfig template = new PipelineTemplateConfig();
        template.add(StageConfigMother.stageConfig("first"));
        pipelineConfig.usingTemplate(template);
        assertThat(pipelineConfig.hasTemplateApplied(), is(true));
    }

    @Test
    public void shouldGetAllTemplateVariableNames() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs());
        pipelineConfig.setLabelTemplate("pipeline-${COUNT}-${mymaterial}${hi}");

        Set<String> variables = pipelineConfig.getTemplateVariables();
        assertThat(variables.contains("COUNT"), is(true));
        assertThat(variables.contains("mymaterial"), is(true));
        assertThat(variables.contains("hi"), is(true));
    }

    @Test
    public void shouldValidateCorrectPipelineLabelWithoutAnyMaterial() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs(),new StageConfig(new CaseInsensitiveString("first"), new JobConfigs()));
        pipelineConfig.setLabelTemplate("pipeline-${COUNT}-alpha");
        pipelineConfig.validate(null);
        assertThat(pipelineConfig.errors().isEmpty(), is(true));
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), is(nullValue()));
    }

    @Test
    public void shouldValidateMissingLabel() {
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(null);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), is(PipelineConfig.BLANK_LABEL_TEMPLATE_ERROR_MESSAGE));

        pipelineConfig = createAndValidatePipelineLabel("");
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), is(PipelineConfig.BLANK_LABEL_TEMPLATE_ERROR_MESSAGE));
    }

    @Test
    public void shouldValidateCorrectPipelineLabelWithoutTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${git}-454";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), is(nullValue()));
    }

    @Test
    public void shouldValidatePipelineLabelWithNonExistingMaterial() {
        String labelFormat = "pipeline-${COUNT}-${NoSuchMaterial}";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), startsWith("You have defined a label template in pipeline"));
    }

    @Test
    public void shouldValidateCorrectPipelineLabelWithTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${git[:7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), is(nullValue()));
    }


    @Test
    public void shouldValidatePipelineLabelWithBrokenTruncationSyntax1() {
        String labelFormat = "pipeline-${COUNT}-${git[:7}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), startsWith("Invalid label."));
    }

    @Test
    public void shouldValidatePipelineLabelWithBrokenTruncationSyntax2() {
        String labelFormat = "pipeline-${COUNT}-${git[7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), startsWith("Invalid label."));
    }

    @Test
    public void shouldValidateIncorrectPipelineLabelWithTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${noSuch[:7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), startsWith("You have defined a label template in pipeline"));
    }

    @Test
    public void shouldNotAllowLabelTemplateWithLengthOfZeroInTruncationSyntax() throws Exception {
        String labelFormat = "pipeline-${COUNT}-${git[:0]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), is(String.format("Length of zero not allowed on label %s defined on pipeline %s.", labelFormat, pipelineConfig.name())));
    }

    @Test
    public void shouldNotAllowLabelTemplateWithLengthOfZeroInTruncationSyntax2() throws Exception {
        String labelFormat = "pipeline-${COUNT}-${git[:0]}${one[:00]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), is(String.format("Length of zero not allowed on label %s defined on pipeline %s.",labelFormat,pipelineConfig.name())));
    }

    @Test
    public void shouldSupportTruncationSyntax() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs());
        pipelineConfig.setLabelTemplate("pipeline-${COUNT}-${git[:7]}-alpha");

        Set<String> variables = pipelineConfig.getTemplateVariables();
        assertThat(variables, contains("COUNT", "git"));
        assertThat(variables.size(), is(2));
    }

    @Test
    public void shouldSupportSpecialCharactors() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs());
        pipelineConfig.setLabelTemplate("pipeline-${COUN_T}-${my-material}${h.i}${**}");

        Set<String> variables = pipelineConfig.getTemplateVariables();
        assertThat(variables, contains("COUN_T", "my-material", "h.i", "**"));
    }

    @Test
    public void shouldAllowColonInLabelTemplateVariable() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs());
        pipelineConfig.setLabelTemplate("pipeline-${COUN_T}:${repo:package}");
        Set<String> variables = pipelineConfig.getTemplateVariables();
        assertThat(variables.contains("repo:package"), is(true));
    }

    @Test
    public void shouldSetPipelineConfigFromConfigAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HashMap mingleConfigMap = new HashMap();
        mingleConfigMap.put("mingleconfig", "mingleconfig");
        HashMap trackingToolMap = new HashMap();
        trackingToolMap.put("trackingtool", "trackingtool");
        HashMap timerConfigMap = new HashMap();
        String cronSpec = "0 0 11 * * ?";
        timerConfigMap.put(TimerConfig.TIMER_SPEC, cronSpec);

        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "LABEL123-${COUNT}");
        configMap.put(PipelineConfig.MINGLE_CONFIG, mingleConfigMap);
        configMap.put(PipelineConfig.TRACKING_TOOL, trackingToolMap);
        configMap.put(PipelineConfig.TIMER_CONFIG, timerConfigMap);

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate(), is("LABEL123-${COUNT}"));
        assertThat(pipelineConfig.getTimer().getTimerSpec(), is(cronSpec));
        assertThat(pipelineConfig.getTimer().shouldTriggerOnlyOnChanges(), is(false));
    }

    @Test
    public void shouldSetPipelineConfigFromConfigAttributesForTimerConfig() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HashMap timerConfigMap = new HashMap();
        String cronSpec = "0 0 11 * * ?";
        timerConfigMap.put(TimerConfig.TIMER_SPEC, cronSpec);
        timerConfigMap.put(TimerConfig.TIMER_ONLY_ON_CHANGES, "1");

        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "LABEL123-${COUNT}");
        configMap.put(PipelineConfig.TIMER_CONFIG, timerConfigMap);

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate(), is("LABEL123-${COUNT}"));
        assertThat(pipelineConfig.getTimer().getTimerSpec(), is(cronSpec));
        assertThat(pipelineConfig.getTimer().shouldTriggerOnlyOnChanges(), is(true));
    }

    @Test
    public void shouldSetLabelTemplateToDefaultValueIfBlankIsEnteredWhileSettingConfigAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "");

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate(), is(PipelineLabel.COUNT_TEMPLATE));
    }

    @Test
    public void shouldNotSetLockStatusOnPipelineConfigWhenLockIs0() {
        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LOCK, "0");

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setConfigAttributes(configMap);
        assertThat(pipelineConfig.isLock(), is(false));
    }

    @Test
    public void shouldSetLockStatusOnPipelineConfigWhenLockIs1() {
        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LOCK, "1");

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setConfigAttributes(configMap);
        assertThat(pipelineConfig.isLock(), is(true));
    }

    @Test
    public void shouldPopulateEnvironmentVariablesFromAttributeMap() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("name", "FOO");
        valueHashMap.put("value", "BAR");
        map.put(PipelineConfig.ENVIRONMENT_VARIABLES, valueHashMap);
        pipelineConfig.setVariables(mockEnvironmentVariablesConfig);

        pipelineConfig.setConfigAttributes(map);
        verify(mockEnvironmentVariablesConfig).setConfigAttributes(valueHashMap);
    }

    @Test
    public void shouldPopulateParamsFromAttributeMapWhenConfigurationTypeIsNotSet() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        final HashMap map = new HashMap();
        final HashMap valueHashMap = new HashMap();
        valueHashMap.put("param-name", "FOO");
        valueHashMap.put("param-value", "BAR");
        map.put(PipelineConfig.PARAMS, valueHashMap);
        pipelineConfig.setParams(mockParamsConfig);

        pipelineConfig.setConfigAttributes(map);
        verify(mockParamsConfig).setConfigAttributes(valueHashMap);
    }

    @Test
    public void shouldPopulateParamsFromAttributeMapIfConfigurationTypeIsTemplate() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("param-name", "FOO");
        valueHashMap.put("param-value", "BAR");
        map.put(PipelineConfig.PARAMS, valueHashMap);
        map.put(PipelineConfig.CONFIGURATION_TYPE, PipelineConfig.CONFIGURATION_TYPE_TEMPLATE);
        map.put(PipelineConfig.TEMPLATE_NAME, "template");
        pipelineConfig.setParams(mockParamsConfig);

        pipelineConfig.setConfigAttributes(map);
        verify(mockParamsConfig).setConfigAttributes(valueHashMap);
    }

    @Test
    public void shouldNotPopulateParamsFromAttributeMapIfConfigurationTypeIsStages() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("param-name", "FOO");
        valueHashMap.put("param-value", "BAR");
        map.put(PipelineConfig.PARAMS, valueHashMap);
        map.put(PipelineConfig.CONFIGURATION_TYPE, PipelineConfig.CONFIGURATION_TYPE_STAGES);
        pipelineConfig.setParams(mockParamsConfig);

        pipelineConfig.setConfigAttributes(map);
        verify(mockParamsConfig, never()).setConfigAttributes(valueHashMap);
    }

    @Test
    public void shouldSetTheCorrectIntegrationType() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        assertThat(pipelineConfig.getIntegrationType(), is(PipelineConfig.INTEGRATION_TYPE_NONE));

        pipelineConfig = new PipelineConfig();
        pipelineConfig.setTrackingTool(new TrackingTool("link", "regex"));
        assertThat(pipelineConfig.getIntegrationType(), is(PipelineConfig.INTEGRATION_TYPE_TRACKING_TOOL));

        pipelineConfig = new PipelineConfig();
        pipelineConfig.setMingleConfig(new MingleConfig("baseUri", "projId"));
        assertThat(pipelineConfig.getIntegrationType(), is(PipelineConfig.INTEGRATION_TYPE_MINGLE));
    }

    @Test
    public void shouldSetIntegrationTypeToMingleInCaseAnEmptyMingleConfigIsSubmitted() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        MingleConfig mingleConfig = new MingleConfig();
        mingleConfig.addError(MingleConfig.BASE_URL, "some error");
        pipelineConfig.setMingleConfig(mingleConfig);

        String integrationType = pipelineConfig.getIntegrationType();

        assertThat(integrationType, is(PipelineConfig.INTEGRATION_TYPE_MINGLE));
    }

    @Test
    public void shouldPopulateTrackingToolWhenIntegrationTypeIsTrackingToolAndLinkAndRegexAreDefined() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setMingleConfig(new MingleConfig("baseUri", "go"));

        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("link", "GoleyLink");
        valueHashMap.put("regex", "GoleyRegex");

        map.put(PipelineConfig.TRACKING_TOOL, valueHashMap);
        map.put(PipelineConfig.INTEGRATION_TYPE, PipelineConfig.INTEGRATION_TYPE_TRACKING_TOOL);

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getTrackingTool(), is(new TrackingTool("GoleyLink", "GoleyRegex")));
        assertThat(pipelineConfig.getMingleConfig(), is(new MingleConfig()));
        assertThat(pipelineConfig.getIntegrationType(), is(PipelineConfig.INTEGRATION_TYPE_TRACKING_TOOL));
    }

    @Test
    public void shouldPopulateMingleConfigWhenIntegrationTypeIsMingle() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setTrackingTool(new TrackingTool("link", "regex"));

        Map map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put(MingleConfig.BASE_URL, "url");
        valueHashMap.put(MingleConfig.PROJECT_IDENTIFIER, "identifier");
        valueHashMap.put(MqlCriteria.MQL, "criteria");
        valueHashMap.put(MingleConfig.MQL_GROUPING_CONDITIONS, valueHashMap);

        map.put(PipelineConfig.MINGLE_CONFIG, valueHashMap);
        map.put(PipelineConfig.INTEGRATION_TYPE, PipelineConfig.INTEGRATION_TYPE_MINGLE);

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getMingleConfig(), is(new MingleConfig("url", "identifier", "criteria")));
        assertThat(pipelineConfig.getTrackingTool(), is(nullValue()));
        assertThat(pipelineConfig.getIntegrationType(), is(PipelineConfig.INTEGRATION_TYPE_MINGLE));
    }

    @Test
    public void shouldResetMingleConfigWhenIntegrationTypeIsNone() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setMingleConfig(new MingleConfig("baseUri", "go"));

        Map map = new HashMap();
        map.put(PipelineConfig.INTEGRATION_TYPE, PipelineConfig.INTEGRATION_TYPE_NONE);

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getMingleConfig(), is(new MingleConfig()));
        assertThat(pipelineConfig.getIntegrationType(), is(PipelineConfig.INTEGRATION_TYPE_NONE));
    }

    @Test
    public void shouldResetTrackingToolWhenIntegrationTypeIsNone() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setTrackingTool(new TrackingTool("link", "regex"));

        Map map = new HashMap();
        map.put(PipelineConfig.INTEGRATION_TYPE, PipelineConfig.INTEGRATION_TYPE_NONE);

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getTrackingTool(), is(nullValue()));
        assertThat(pipelineConfig.getIntegrationType(), is(PipelineConfig.INTEGRATION_TYPE_NONE));
    }

    @Test
    public void shouldGetTheCorrectConfigurationType() {
        PipelineConfig pipelineConfigWithTemplate = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        assertThat(pipelineConfigWithTemplate.getConfigurationType(), is(PipelineConfig.CONFIGURATION_TYPE_TEMPLATE));

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline");
        assertThat(pipelineConfig.getConfigurationType(), is(PipelineConfig.CONFIGURATION_TYPE_STAGES));
    }

    @Test
    public void shouldUseTemplateWhenSetConfigAttributesContainsTemplateName() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline");
        assertThat(pipelineConfig.hasTemplate(), is(false));

        Map map = new HashMap();
        map.put(PipelineConfig.CONFIGURATION_TYPE, PipelineConfig.CONFIGURATION_TYPE_TEMPLATE);
        map.put(PipelineConfig.TEMPLATE_NAME, "foo-template");

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getConfigurationType(), is(PipelineConfig.CONFIGURATION_TYPE_TEMPLATE));
        assertThat(pipelineConfig.getTemplateName(), is(new CaseInsensitiveString("foo-template")));
    }

    @Test
    public void shouldIncrementIndexBy1OfGivenStage() {
        StageConfig moveMeStage = StageConfigMother.stageConfig("move-me");
        StageConfig dontMoveMeStage = StageConfigMother.stageConfig("dont-move-me");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", moveMeStage, dontMoveMeStage);

        pipelineConfig.incrementIndex(moveMeStage);

        assertThat(pipelineConfig.indexOf(moveMeStage), is(1));
        assertThat(pipelineConfig.indexOf(dontMoveMeStage), is(0));
    }

    @Test
    public void shouldDecrementIndexBy1OfGivenStage() {
        StageConfig moveMeStage = StageConfigMother.stageConfig("move-me");
        StageConfig dontMoveMeStage = StageConfigMother.stageConfig("dont-move-me");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", dontMoveMeStage, moveMeStage);

        pipelineConfig.decrementIndex(moveMeStage);

        assertThat(pipelineConfig.indexOf(moveMeStage), is(0));
        assertThat(pipelineConfig.indexOf(dontMoveMeStage), is(1));
    }

    @Test
    public void shouldThrowExceptionWhenTheStageIsNotFound() {
        StageConfig moveMeStage = StageConfigMother.stageConfig("move-me");
        StageConfig dontMoveMeStage = StageConfigMother.stageConfig("dont-move-me");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", dontMoveMeStage);

        try {
            pipelineConfig.incrementIndex(moveMeStage);
            fail("Should fail to increment the index of a stage that is not found");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage(), is("Cannot find the stage 'move-me' in pipeline 'pipeline'"));
        }

        try {
            pipelineConfig.decrementIndex(moveMeStage);
            fail("Should fail to increment the index of a stage that is not found");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage(), is("Cannot find the stage 'move-me' in pipeline 'pipeline'"));
        }
    }

    @Test
    public void shouldReturnListOfStageConfigWhichIsApplicableForFetchArtifact() {
        PipelineConfig superUpstream = PipelineConfigMother.createPipelineConfigWithStages("superUpstream", "s1", "s2", "s3");
        PipelineConfig upstream = PipelineConfigMother.createPipelineConfigWithStages("upstream", "s4", "s5", "s6");
        upstream.addMaterialConfig(new DependencyMaterialConfig(superUpstream.name(), new CaseInsensitiveString("s2")));

        PipelineConfig downstream = PipelineConfigMother.createPipelineConfigWithStages("downstream", "s7");
        downstream.addMaterialConfig(new DependencyMaterialConfig(upstream.name(), new CaseInsensitiveString("s5")));

        List<StageConfig> fetchableStages = upstream.validStagesForFetchArtifact(downstream, new CaseInsensitiveString("s7"));

        assertThat(fetchableStages.size(), is(2));
        assertThat(fetchableStages, hasItem(upstream.get(0)));
        assertThat(fetchableStages, hasItem(upstream.get(1)));
    }

    @Test
    public void shouldReturnStagesBeforeCurrentForSelectedPipeline() {
        PipelineConfig downstream = PipelineConfigMother.createPipelineConfigWithStages("downstream", "s1", "s2");
        List<StageConfig> fetchableStages = downstream.validStagesForFetchArtifact(downstream, new CaseInsensitiveString("s2"));
        assertThat(fetchableStages.size(), is(1));
        assertThat(fetchableStages, hasItem(downstream.get(0)));
    }

    @Test
    public void shouldUpdateNameAndMaterialsOnAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        HashMap svnMaterialConfigMap = new HashMap();
        svnMaterialConfigMap.put(SvnMaterialConfig.URL, "http://url");
        svnMaterialConfigMap.put(SvnMaterialConfig.USERNAME, "loser");
        svnMaterialConfigMap.put(SvnMaterialConfig.PASSWORD, "passwd");
        svnMaterialConfigMap.put(SvnMaterialConfig.CHECK_EXTERNALS, false);

        HashMap materialConfigsMap = new HashMap();
        materialConfigsMap.put(AbstractMaterialConfig.MATERIAL_TYPE, SvnMaterialConfig.TYPE);
        materialConfigsMap.put(SvnMaterialConfig.TYPE, svnMaterialConfigMap);

        HashMap attributeMap = new HashMap();
        attributeMap.put(PipelineConfig.NAME, "startup");
        attributeMap.put(PipelineConfig.MATERIALS, materialConfigsMap);

        pipelineConfig.setConfigAttributes(attributeMap);

        assertThat(pipelineConfig.name(), is(new CaseInsensitiveString("startup")));
        assertThat((SvnMaterialConfig) pipelineConfig.materialConfigs().get(0), is(new SvnMaterialConfig("http://url", "loser", "passwd", false)));
    }

    @Test
    public void shouldUpdateStageOnAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        HashMap stageMap = new HashMap();
        List jobList = a(m(JobConfig.NAME, "JobName"));
        stageMap.put(StageConfig.NAME, "someStage");
        stageMap.put(StageConfig.JOBS, jobList);

        HashMap attributeMap = new HashMap();
        attributeMap.put(PipelineConfig.NAME, "startup");
        attributeMap.put(PipelineConfig.STAGE, stageMap);

        pipelineConfig.setConfigAttributes(attributeMap);

        assertThat(pipelineConfig.name(), is(new CaseInsensitiveString("startup")));
        assertThat(pipelineConfig.get(0).name(), is(new CaseInsensitiveString("someStage")));
        assertThat(pipelineConfig.get(0).getJobs().first().name(), is(new CaseInsensitiveString("JobName")));
    }

    @Test
    public void shouldValidatePipelineName() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("foo bar", "stage1", "job1");
        pipelineConfig.validate(null);
        assertThat(pipelineConfig.errors().isEmpty(), is(false));
        assertThat(pipelineConfig.errors().on(PipelineConfig.NAME),
                is("Invalid pipeline name 'foo bar'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldRemoveExistingStagesWhileDoingAStageUpdate() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("foo"), new MaterialConfigs(), new StageConfig(new CaseInsensitiveString("first"), new JobConfigs()),
                new StageConfig(
                        new CaseInsensitiveString("second"), new JobConfigs()));

        HashMap stageMap = new HashMap();
        List jobList = a(m(JobConfig.NAME, "JobName"));
        stageMap.put(StageConfig.NAME, "someStage");
        stageMap.put(StageConfig.JOBS, jobList);

        HashMap attributeMap = new HashMap();
        attributeMap.put(PipelineConfig.NAME, "startup");
        attributeMap.put(PipelineConfig.STAGE, stageMap);

        pipelineConfig.setConfigAttributes(attributeMap);

        assertThat(pipelineConfig.name(), is(new CaseInsensitiveString("startup")));
        assertThat(pipelineConfig.size(), is(1));
        assertThat(pipelineConfig.get(0).name(), is(new CaseInsensitiveString("someStage")));
        assertThat(pipelineConfig.get(0).getJobs().first().name(), is(new CaseInsensitiveString("JobName")));
    }

    @Test
    public void shouldGetAllFetchTasks() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("foo bar", "stage1", "job1");
        FetchTask firstFetch = new FetchTask();
        JobConfig firstJob = pipelineConfig.getFirstStageConfig().getJobs().get(0);
        firstJob.addTask(firstFetch);
        firstJob.addTask(new AntTask());

        JobConfig secondJob = new JobConfig();
        secondJob.addTask(new ExecTask());
        FetchTask secondFetch = new FetchTask();
        secondJob.addTask(secondFetch);
        pipelineConfig.add(new StageConfig(new CaseInsensitiveString("stage-2"), new JobConfigs(secondJob)));

        List<FetchTask> fetchTasks = pipelineConfig.getFetchTasks();
        assertThat(fetchTasks.size(), is(2));
        assertThat(fetchTasks.contains(firstFetch), is(true));
        assertThat(fetchTasks.contains(secondFetch), is(true));
    }

    @Test
    public void shouldGetOnlyPlainTextVariables() throws InvalidCipherTextException {
        PipelineConfig pipelineConfig = new PipelineConfig();
        EnvironmentVariableConfig username = new EnvironmentVariableConfig("username", "ram");
        pipelineConfig.addEnvironmentVariable(username);
        GoCipher goCipher = mock(GoCipher.class);
        when(goCipher.encrypt("=%HG*^&*&^")).thenReturn("encrypted");
        EnvironmentVariableConfig password = new EnvironmentVariableConfig(goCipher, "password", "=%HG*^&*&^", true);
        pipelineConfig.addEnvironmentVariable(password);
        EnvironmentVariablesConfig plainTextVariables = pipelineConfig.getPlainTextVariables();
        assertThat(plainTextVariables, not(hasItem(password)));
        assertThat(plainTextVariables, hasItem(username));
    }

    @Test
    public void shouldGetOnlySecureVariables() throws InvalidCipherTextException {
        PipelineConfig pipelineConfig = new PipelineConfig();
        EnvironmentVariableConfig username = new EnvironmentVariableConfig("username", "ram");
        pipelineConfig.addEnvironmentVariable(username);
        GoCipher goCipher = mock(GoCipher.class);
        when(goCipher.encrypt("=%HG*^&*&^")).thenReturn("encrypted");
        EnvironmentVariableConfig password = new EnvironmentVariableConfig(goCipher, "password", "=%HG*^&*&^", true);
        pipelineConfig.addEnvironmentVariable(password);
        List<EnvironmentVariableConfig> plainTextVariables = pipelineConfig.getSecureVariables();
        assertThat(plainTextVariables, hasItem(password));
        assertThat(plainTextVariables, not(hasItem(username)));
    }

    @Test
    public void shouldTemplatizeAPipeline() {
        PipelineConfig config = PipelineConfigMother.createPipelineConfigWithStages("pipeline", "stage1", "stage2");
        config.templatize(new CaseInsensitiveString("template"));
        assertThat(config.hasTemplate(), is(true));
        assertThat(config.hasTemplateApplied(), is(false));
        assertThat(config.getTemplateName(), is(new CaseInsensitiveString("template")));
        assertThat(config.isEmpty(), is(true));
        config.templatize(new CaseInsensitiveString(""));
        assertThat(config.hasTemplate(), is(false));
        config.templatize(null);
        assertThat(config.hasTemplate(), is(false));
    }

    @Test
    public void shouldAssignApprovalTypeOnFirstStageAsAuto() throws Exception {
        Map approvalAttributes = Collections.singletonMap(Approval.TYPE, Approval.SUCCESS);
        Map<String, Map> map = Collections.singletonMap(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("p1", "s1", "j1");
        pipelineConfig.get(0).updateApproval(Approval.manualApproval());

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType(), is(Approval.SUCCESS));
    }

    @Test
    public void shouldAssignApprovalTypeOnFirstStageAsManual() throws Exception {
        Map approvalAttributes = Collections.singletonMap(Approval.TYPE, Approval.MANUAL);
        Map<String, Map> map = Collections.singletonMap(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("p1", "s1", "j1");
        pipelineConfig.get(0).updateApproval(Approval.manualApproval());

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType(), is(Approval.MANUAL));
    }

    @Test
    public void shouldAssignApprovalTypeOnFirstStageAsManualAndRestOfStagesAsUntouched() throws Exception {
        Map approvalAttributes = Collections.singletonMap(Approval.TYPE, Approval.MANUAL);
        Map<String, Map> map = Collections.singletonMap(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", StageConfigMother.custom("s1", Approval.automaticApproval()),
                StageConfigMother.custom("s2", Approval.automaticApproval()));

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType(), is(Approval.MANUAL));
        assertThat(pipelineConfig.get(1).getApproval().getType(), is(Approval.SUCCESS));
    }

    @Test
    public void shouldGetPackageMaterialConfigs() throws Exception {
        SvnMaterialConfig svn = new SvnMaterialConfig("svn", false);
        PackageMaterialConfig packageMaterialOne = new PackageMaterialConfig();
        PackageMaterialConfig packageMaterialTwo = new PackageMaterialConfig();

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(svn, packageMaterialOne, packageMaterialTwo));
        List<PackageMaterialConfig> packageMaterialConfigs = pipelineConfig.packageMaterialConfigs();

        assertThat(packageMaterialConfigs.size(), is(2));
        assertThat(packageMaterialConfigs, hasItems(packageMaterialOne, packageMaterialTwo));
    }

    @Test
    public void shouldGetPluggableSCMMaterialConfigs() throws Exception {
        SvnMaterialConfig svn = new SvnMaterialConfig("svn", false);
        PluggableSCMMaterialConfig pluggableSCMMaterialOne = new PluggableSCMMaterialConfig("scm-id-1");
        PluggableSCMMaterialConfig pluggableSCMMaterialTwo = new PluggableSCMMaterialConfig("scm-id-2");

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(svn, pluggableSCMMaterialOne, pluggableSCMMaterialTwo));
        List<PluggableSCMMaterialConfig> pluggableSCMMaterialConfigs = pipelineConfig.pluggableSCMMaterialConfigs();

        assertThat(pluggableSCMMaterialConfigs.size(), is(2));
        assertThat(pluggableSCMMaterialConfigs, hasItems(pluggableSCMMaterialOne, pluggableSCMMaterialTwo));
    }

    @Test
    public void shouldReturnTrueWhenOneOfPipelineMaterialsIsTheSameAsConfigOrigin()
    {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(material, "plugin"), "1233"));

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials(),is(true));
    }
    @Test
    public void shouldReturnTrueWhenOneOfPipelineMaterialsIsTheSameAsConfigOriginButDestinationIsDifferent()
    {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        pipelineConfig.materialConfigs().clear();
        GitMaterialConfig pipeMaterialConfig = new GitMaterialConfig("http://git");
        pipeMaterialConfig.setFolder("dest1");
        pipelineConfig.materialConfigs().add(pipeMaterialConfig);

        GitMaterialConfig repoMaterialConfig = new GitMaterialConfig("http://git");

        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(repoMaterialConfig,"plugin"),"1233"));

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials(),is(true));
    }

    @Test
    public void shouldReturnFalseWhenOneOfPipelineMaterialsIsNotTheSameAsConfigOrigin()
    {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = new GitMaterialConfig("http://git");
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(material, "plugin"), "1233"));

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials(),is(false));
    }

    @Test
    public void shouldReturnFalseIfOneOfPipelineMaterialsIsTheSameAsConfigOrigin_WhenOriginIsFile()
    {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        pipelineConfig.setOrigin(new FileConfigOrigin());

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials(),is(false));
    }

    @Test
    public void shouldReturnTrueWhenConfigRevisionIsEqualToQuery()
    {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(material, "plugin"), "1233"));

        assertThat(pipelineConfig.isConfigOriginFromRevision("1233"),is(true));
    }
    @Test
    public void shouldReturnFalseWhenConfigRevisionIsNotEqualToQuery()
    {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(material, "plugin"), "1233"));

        assertThat(pipelineConfig.isConfigOriginFromRevision("32"),is(false));
    }

    @Test
    public void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsRemote() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "revision1"));
        assertThat(pipelineConfig.getOriginDisplayName(), is("AwesomeGitMaterial at revision1"));
    }

    @Test
    public void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsFile() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setOrigin(new FileConfigOrigin());
        assertThat(pipelineConfig.getOriginDisplayName(), is("cruise-config.xml"));
    }

    @Test
    public void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsNotSet() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        assertThat(pipelineConfig.getOriginDisplayName(), is("cruise-config.xml"));
    }

    private StageConfig completedStage() {
        JobConfigs plans = new JobConfigs();
        plans.add(new JobConfig("completed"));
        return new StageConfig(new CaseInsensitiveString("completed stage"), plans);
    }

    private StageConfig buildingStage() {
        JobConfigs plans = new JobConfigs();
        plans.add(new JobConfig(BUILDING_PLAN_NAME));
        return new StageConfig(new CaseInsensitiveString("building stage"), plans);
    }

    private PipelineConfig createAndValidatePipelineLabel(String labelFormat) {
        GitMaterialConfig git = new GitMaterialConfig("git@github.com:gocd/gocd.git");
        git.setName(new CaseInsensitiveString("git"));

        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs(git));
        pipelineConfig.setLabelTemplate(labelFormat);

        pipelineConfig.validate(null);

        return pipelineConfig;
    }
}
