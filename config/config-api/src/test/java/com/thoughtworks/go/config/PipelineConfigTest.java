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
package com.thoughtworks.go.config;

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
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.Node;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svn;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

public class PipelineConfigTest {
    private static final String BUILDING_PLAN_NAME = "building";

    private EnvironmentVariablesConfig mockEnvironmentVariablesConfig = mock(EnvironmentVariablesConfig.class);
    private ParamsConfig mockParamsConfig = mock(ParamsConfig.class);

    public enum Foo {
        Bar, Baz;
    }

    @Test
    void shouldFindByName() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.findBy(new CaseInsensitiveString("completed stage")).name()).isEqualTo(new CaseInsensitiveString("completed stage"));
    }

    @Test
    void shouldReturnDuplicateWithoutName() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("somePipeline");
        PipelineConfig clonedPipelineConfig = pipelineConfig.duplicate();
        assertThat(clonedPipelineConfig.name()).isEqualTo(new CaseInsensitiveString(""));
        assertThat(clonedPipelineConfig.materialConfigs()).isEqualTo(pipelineConfig.materialConfigs());
        assertThat(clonedPipelineConfig.getFirstStageConfig()).isEqualTo(pipelineConfig.getFirstStageConfig());
    }

    @Test
    void shouldReturnDuplicateWithPipelineNameEmptyIfFetchArtifactTaskIsFetchingFromSamePipeline() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("somePipeline", "stage", "job");
        StageConfig stageConfig = pipelineConfig.get(0);
        JobConfig jobConfig = stageConfig.getJobs().get(0);
        Tasks originalTasks = jobConfig.getTasks();
        originalTasks.add(new FetchTask(pipelineConfig.name(), stageConfig.name(), jobConfig.name(), "src", "dest"));
        originalTasks.add(new FetchTask(new CaseInsensitiveString("some_other_pipeline"), stageConfig.name(), jobConfig.name(), "src", "dest"));
        PipelineConfig clone = pipelineConfig.duplicate();
        Tasks clonedTasks = clone.get(0).getJobs().get(0).getTasks();
        assertThat(((FetchTask) clonedTasks.get(1)).getTargetPipelineName()).isEqualTo(new CaseInsensitiveString(""));
        assertThat(((FetchTask) clonedTasks.get(2)).getTargetPipelineName()).isEqualTo(new CaseInsensitiveString("some_other_pipeline"));
        assertThat(((FetchTask) originalTasks.get(1)).getTargetPipelineName()).isEqualTo(pipelineConfig.name());
    }

    @Test
        //#6821
    void shouldCopyOverAllEnvironmentVariablesWhileCloningAPipeline() throws CryptoException {
        PipelineConfig source = PipelineConfigMother.createPipelineConfig("somePipeline", "stage", "job");
        source.addEnvironmentVariable("k1", "v1");
        source.addEnvironmentVariable("k2", "v2");
        GoCipher goCipher = new GoCipher();
        source.addEnvironmentVariable(new EnvironmentVariableConfig(goCipher, "secret_key", "secret", true));

        PipelineConfig cloned = source.duplicate();
        EnvironmentVariablesConfig clonedEnvVariables = cloned.getPlainTextVariables();
        EnvironmentVariablesConfig sourceEnvVariables = source.getPlainTextVariables();
        assertThat(clonedEnvVariables.size()).isEqualTo(sourceEnvVariables.size());
        clonedEnvVariables.getPlainTextVariables().containsAll(sourceEnvVariables.getPlainTextVariables());
        assertThat(cloned.getSecureVariables().size()).isEqualTo(source.getSecureVariables().size());
        assertThat(cloned.getSecureVariables().containsAll(source.getSecureVariables())).isTrue();
    }

    @Test
    void shouldGetStageByName() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.getStage(new CaseInsensitiveString("COMpleTEd stage")).name()).isEqualTo(new CaseInsensitiveString("completed stage"));
        assertThat(pipelineConfig.getStage(new CaseInsensitiveString("Does-not-exist"))).isNull();
    }

    @Test
    void shouldReturnFalseIfThereIsNoNextStage() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(buildingStage().name())).isFalse();
    }

    @Test
    void shouldReturnFalseIfThereIsNextStage() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(completedStage().name())).isTrue();
    }

    @Test
    void shouldReturnFalseThePassInStageDoesNotExist() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(new CaseInsensitiveString("notExist"))).isFalse();
    }

    @Test
    void shouldReturnTrueIfThereNoStagesDefined() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null);
        assertThat(pipelineConfig.hasNextStage(completedStage().name())).isFalse();
    }

    @Test
    void shouldGetDependenciesAsNode() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("framework"), new CaseInsensitiveString("dev")));
        pipelineConfig.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("middleware"), new CaseInsensitiveString("dev")));
        assertThat(pipelineConfig.getDependenciesAsNode()).isEqualTo(new Node(
                new Node.DependencyNode(new CaseInsensitiveString("framework"), new CaseInsensitiveString("dev")),
                new Node.DependencyNode(new CaseInsensitiveString("middleware"), new CaseInsensitiveString("dev"))));
    }

    @Test
    void shouldReturnTrueIfFirstStageIsManualApproved() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        pipelineConfig.getFirstStageConfig().updateApproval(Approval.manualApproval());
        assertThat(pipelineConfig.isFirstStageManualApproval()).as("First stage should be manual approved").isTrue();
    }

    @Test
    void shouldThrowExceptionForEmptyPipeline() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs());
        try {
            pipelineConfig.isFirstStageManualApproval();
            fail("Should throw exception if pipeline has no pipeline");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Pipeline [" + pipelineConfig.name() + "] doesn't have any stage");
        }
    }

    @Test
    void shouldThrowExceptionOnAddingTemplatesIfItAlreadyHasStages() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        try {
            PipelineTemplateConfig template = new PipelineTemplateConfig();
            template.add(StageConfigMother.stageConfig("first"));
            pipelineConfig.setTemplateName(new CaseInsensitiveString("some-template"));
            fail("Should throw exception because the pipeline has stages already");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Cannot set template 'some-template' on pipeline 'pipeline' because it already has stages defined");
        }
    }

    @Test
    void shouldBombWhenAddingStagesIfItAlreadyHasATemplate() {
        try {
            PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("mingle"), null);
            pipelineConfig.setTemplateName(new CaseInsensitiveString("some-template"));
            pipelineConfig.add(StageConfigMother.stageConfig("second"));
            fail("Should throw exception because pipeline already has a template");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Cannot add stage 'second' to pipeline 'mingle', which already references template 'some-template'.");
        }
    }

    @Test
    void shouldKnowIfATemplateWasApplied() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        assertThat(pipelineConfig.hasTemplateApplied()).isFalse();
        pipelineConfig.clear();

        PipelineTemplateConfig template = new PipelineTemplateConfig();
        template.add(StageConfigMother.stageConfig("first"));
        pipelineConfig.usingTemplate(template);
        assertThat(pipelineConfig.hasTemplateApplied()).isTrue();
    }

    @Test
    void shouldValidateCorrectPipelineLabelWithoutAnyMaterial() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs(), new StageConfig(new CaseInsensitiveString("first"), new JobConfigs()));
        pipelineConfig.setLabelTemplate("pipeline-${COUNT}-alpha");
        pipelineConfig.validate(null);
        assertThat(pipelineConfig.errors().isEmpty()).isTrue();
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).isNull();
    }

    @Test
    void shouldValidateMissingLabel() {
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(null);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).isEqualTo(PipelineConfig.BLANK_LABEL_TEMPLATE_ERROR_MESSAGE);

        pipelineConfig = createAndValidatePipelineLabel("");
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).isEqualTo(PipelineConfig.BLANK_LABEL_TEMPLATE_ERROR_MESSAGE);
    }

    @Test
    void shouldValidateCorrectPipelineLabelWithoutTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${git}-454";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).isNull();
    }

    @Test
    void shouldValidatePipelineLabelWithNonExistingMaterial() {
        String labelFormat = "pipeline-${COUNT}-${NoSuchMaterial}";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).startsWith("You have defined a label template in pipeline");
    }

    @Test
    void shouldValidatePipelineLabelWithEnvironmentVariable() {
        String labelFormat = "pipeline-${COUNT}-${env:SOME_VAR}";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).isNull();
    }

    @Test
    void shouldValidateCorrectPipelineLabelWithTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${git[:7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).isNull();
    }


    @Test
    void shouldValidatePipelineLabelWithBrokenTruncationSyntax1() {
        String labelFormat = "pipeline-${COUNT}-${git[:7}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        String expectedLabelTemplate = "Invalid label 'pipeline-${COUNT}-${git[:7}-alpha'.";
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).startsWith(expectedLabelTemplate);
    }

    @Test
    void shouldValidatePipelineLabelWithBrokenTruncationSyntax2() {
        String labelFormat = "pipeline-${COUNT}-${git[7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        String expectedLabelTemplate = "Invalid label 'pipeline-${COUNT}-${git[7]}-alpha'.";
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).startsWith(expectedLabelTemplate);
    }

    @Test
    void shouldValidateIncorrectPipelineLabelWithTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${noSuch[:7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).startsWith("You have defined a label template in pipeline");
    }

    @Test
    void shouldNotAllowInvalidLabelTemplate() {
        assertPipelineLabelTemplateIsInvalid("1.3.0",
                "Invalid label '1.3.0'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.");
        assertPipelineLabelTemplateIsInvalid("1.3.0-{COUNT}",
                "Invalid label '1.3.0-{COUNT}'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.");
        assertPipelineLabelTemplateIsInvalid("1.3.0-$COUNT}",
                "Invalid label '1.3.0-$COUNT}'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.");
        assertPipelineLabelTemplateIsInvalid("1.3.0-${COUNT",
                "Invalid label '1.3.0-${COUNT'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.");
        assertPipelineLabelTemplateIsInvalid("1.3.0-${}",
                "Label template variable cannot be blank.");

        assertPipelineLabelTemplateIsInvalid("1.3.0-${COUNT}-${git:7]}",
                "You have defined a label template in pipeline 'cruise' that refers to a material called 'git:7]', but no material with this name is defined.");
        assertPipelineLabelTemplateIsInvalid("1.3.0-${COUNT}-${git[:7}",
                "Invalid label '1.3.0-${COUNT}-${git[:7}'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.");
        assertPipelineLabelTemplateIsInvalid("1.3.0-${COUNT}-${git[7]}",
                "Invalid label '1.3.0-${COUNT}-${git[7]}'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.");
        assertPipelineLabelTemplateIsInvalid("1.3.0-${COUNT}-${git[:]}",
                "Invalid label '1.3.0-${COUNT}-${git[:]}'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.");
        assertPipelineLabelTemplateIsInvalid("1.3.0-${COUNT}-${git[:-1]}",
                "Invalid label '1.3.0-${COUNT}-${git[:-1]}'. Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.");
    }

    void assertPipelineLabelTemplateIsInvalid(String labelTemplate, String expectedError) {
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelTemplate);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).contains(expectedError);
    }

    @Test
    void shouldNotAllowLabelTemplateWithLengthOfZeroInTruncationSyntax() throws Exception {
        String labelFormat = "pipeline-${COUNT}-${git[:0]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).isEqualTo(String.format("Length of zero not allowed on label %s defined on pipeline %s.", labelFormat, pipelineConfig.name()));
    }

    @Test
    void shouldNotAllowLabelTemplateWithLengthOfZeroInTruncationSyntax2() throws Exception {
        String labelFormat = "pipeline-${COUNT}-${git[:0]}${one[:00]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE)).isEqualTo(String.format("Length of zero not allowed on label %s defined on pipeline %s.", labelFormat, pipelineConfig.name()));
    }


    @Test
    void shouldSetPipelineConfigFromConfigAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HashMap trackingToolMap = new HashMap();
        trackingToolMap.put("trackingtool", "trackingtool");
        HashMap timerConfigMap = new HashMap();
        String cronSpec = "0 0 11 * * ?";
        timerConfigMap.put(TimerConfig.TIMER_SPEC, cronSpec);

        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "LABEL123-${COUNT}");
        configMap.put(PipelineConfig.TRACKING_TOOL, trackingToolMap);
        configMap.put(PipelineConfig.TIMER_CONFIG, timerConfigMap);

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo("LABEL123-${COUNT}");
        assertThat(pipelineConfig.getTimer().getTimerSpec()).isEqualTo(cronSpec);
        assertThat(pipelineConfig.getTimer().shouldTriggerOnlyOnChanges()).isFalse();
    }

    @Test
    void shouldSetPipelineConfigFromConfigAttributesForTimerConfig() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HashMap timerConfigMap = new HashMap();
        String cronSpec = "0 0 11 * * ?";
        timerConfigMap.put(TimerConfig.TIMER_SPEC, cronSpec);
        timerConfigMap.put(TimerConfig.TIMER_ONLY_ON_CHANGES, "1");

        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "LABEL123-${COUNT}");
        configMap.put(PipelineConfig.TIMER_CONFIG, timerConfigMap);

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo("LABEL123-${COUNT}");
        assertThat(pipelineConfig.getTimer().getTimerSpec()).isEqualTo(cronSpec);
        assertThat(pipelineConfig.getTimer().shouldTriggerOnlyOnChanges()).isTrue();
    }

    @Test
    void shouldSetLabelTemplateToDefaultValueIfBlankIsEnteredWhileSettingConfigAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "");

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo(PipelineLabel.COUNT_TEMPLATE);
    }

    @Test
    void shouldNotSetLockStatusOnPipelineConfigWhenValueIsNone() {
        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LOCK_BEHAVIOR, PipelineConfig.LOCK_VALUE_NONE);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setConfigAttributes(configMap);
        assertThat(pipelineConfig.isLockable()).isFalse();
    }

    @Test
    void shouldSetLockStatusOnPipelineConfigWhenValueIsLockOnFailure() {
        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LOCK_BEHAVIOR, PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setConfigAttributes(configMap);
        assertThat(pipelineConfig.isLockable()).isTrue();
    }

    @Test
    void shouldSetLockStatusOnPipelineConfigWhenValueIsUnlockWhenFinished() {
        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LOCK_BEHAVIOR, PipelineConfig.LOCK_VALUE_UNLOCK_WHEN_FINISHED);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setConfigAttributes(configMap);
        assertThat(pipelineConfig.isPipelineUnlockableWhenFinished()).isTrue();
    }

    @Test
    void shouldNotResetTrackingToolWhenNotSpecifiedInConfigAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        TrackingTool trackingTool = new TrackingTool("link", "regex");
        pipelineConfig.setTrackingTool(trackingTool);

        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "LABEL123-${COUNT}");

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo("LABEL123-${COUNT}");
        assertThat(pipelineConfig.getTrackingTool()).isEqualTo(trackingTool);
    }

    @Test
    void isNotLockableWhenLockValueHasNotBeenSet() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        assertThat(pipelineConfig.hasExplicitLock()).isFalse();
        assertThat(pipelineConfig.isLockable()).isFalse();
    }

    @Test
    void shouldValidateLockBehaviorValues() throws Exception {
        Map configMap = new HashMap();
        configMap.put(PipelineConfig.LOCK_BEHAVIOR, "someRandomValue");

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1");
        pipelineConfig.setConfigAttributes(configMap);
        pipelineConfig.validate(null);

        assertThat(pipelineConfig.errors().isEmpty()).isFalse();
        assertThat(pipelineConfig.errors().on(PipelineConfig.LOCK_BEHAVIOR)).contains("Lock behavior has an invalid value (someRandomValue). Valid values are: ");
    }

    @Test
    void shouldAllowNullForLockBehavior() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1");
        pipelineConfig.setLockBehaviorIfNecessary(null);
        pipelineConfig.validate(null);

        assertThat(pipelineConfig.errors().isEmpty()).as(pipelineConfig.errors().toString()).isTrue();
    }

    @Test
    void shouldPopulateEnvironmentVariablesFromAttributeMap() {
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
    void shouldPopulateParamsFromAttributeMapWhenConfigurationTypeIsNotSet() {
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
    void shouldPopulateParamsFromAttributeMapIfConfigurationTypeIsTemplate() {
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
    void shouldNotPopulateParamsFromAttributeMapIfConfigurationTypeIsStages() {
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
    void shouldPopulateTrackingToolWhenTrackingToolAndLinkAndRegexAreDefined() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("link", "GoleyLink");
        valueHashMap.put("regex", "GoleyRegex");

        map.put(PipelineConfig.TRACKING_TOOL, valueHashMap);

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getTrackingTool()).isEqualTo(new TrackingTool("GoleyLink", "GoleyRegex"));
    }

    @Test
    void shouldGetTheCorrectConfigurationType() {
        PipelineConfig pipelineConfigWithTemplate = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        assertThat(pipelineConfigWithTemplate.getConfigurationType()).isEqualTo(PipelineConfig.CONFIGURATION_TYPE_TEMPLATE);

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline");
        assertThat(pipelineConfig.getConfigurationType()).isEqualTo(PipelineConfig.CONFIGURATION_TYPE_STAGES);
    }

    @Test
    void shouldUseTemplateWhenSetConfigAttributesContainsTemplateName() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline");
        assertThat(pipelineConfig.hasTemplate()).isFalse();

        Map map = new HashMap();
        map.put(PipelineConfig.CONFIGURATION_TYPE, PipelineConfig.CONFIGURATION_TYPE_TEMPLATE);
        map.put(PipelineConfig.TEMPLATE_NAME, "foo-template");

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getConfigurationType()).isEqualTo(PipelineConfig.CONFIGURATION_TYPE_TEMPLATE);
        assertThat(pipelineConfig.getTemplateName()).isEqualTo(new CaseInsensitiveString("foo-template"));
    }

    @Test
    void shouldIncrementIndexBy1OfGivenStage() {
        StageConfig moveMeStage = StageConfigMother.stageConfig("move-me");
        StageConfig dontMoveMeStage = StageConfigMother.stageConfig("dont-move-me");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", moveMeStage, dontMoveMeStage);

        pipelineConfig.incrementIndex(moveMeStage);

        assertThat(pipelineConfig.indexOf(moveMeStage)).isEqualTo(1);
        assertThat(pipelineConfig.indexOf(dontMoveMeStage)).isEqualTo(0);
    }

    @Test
    void shouldDecrementIndexBy1OfGivenStage() {
        StageConfig moveMeStage = StageConfigMother.stageConfig("move-me");
        StageConfig dontMoveMeStage = StageConfigMother.stageConfig("dont-move-me");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", dontMoveMeStage, moveMeStage);

        pipelineConfig.decrementIndex(moveMeStage);

        assertThat(pipelineConfig.indexOf(moveMeStage)).isEqualTo(0);
        assertThat(pipelineConfig.indexOf(dontMoveMeStage)).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionWhenTheStageIsNotFound() {
        StageConfig moveMeStage = StageConfigMother.stageConfig("move-me");
        StageConfig dontMoveMeStage = StageConfigMother.stageConfig("dont-move-me");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", dontMoveMeStage);

        try {
            pipelineConfig.incrementIndex(moveMeStage);
            fail("Should fail to increment the index of a stage that is not found");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage()).isEqualTo("Cannot find the stage 'move-me' in pipeline 'pipeline'");
        }

        try {
            pipelineConfig.decrementIndex(moveMeStage);
            fail("Should fail to increment the index of a stage that is not found");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage()).isEqualTo("Cannot find the stage 'move-me' in pipeline 'pipeline'");
        }
    }

    @Test
    void shouldReturnListOfStageConfigWhichIsApplicableForFetchArtifact() {
        PipelineConfig superUpstream = PipelineConfigMother.createPipelineConfigWithStages("superUpstream", "s1", "s2", "s3");
        PipelineConfig upstream = PipelineConfigMother.createPipelineConfigWithStages("upstream", "s4", "s5", "s6");
        upstream.addMaterialConfig(new DependencyMaterialConfig(superUpstream.name(), new CaseInsensitiveString("s2")));

        PipelineConfig downstream = PipelineConfigMother.createPipelineConfigWithStages("downstream", "s7");
        downstream.addMaterialConfig(new DependencyMaterialConfig(upstream.name(), new CaseInsensitiveString("s5")));

        List<StageConfig> fetchableStages = upstream.validStagesForFetchArtifact(downstream, new CaseInsensitiveString("s7"));

        assertThat(fetchableStages.size()).isEqualTo(2);
        assertThat(fetchableStages).contains(upstream.get(0));
        assertThat(fetchableStages).contains(upstream.get(1));
    }

    @Test
    void shouldReturnStagesBeforeCurrentForSelectedPipeline() {
        PipelineConfig downstream = PipelineConfigMother.createPipelineConfigWithStages("downstream", "s1", "s2");
        List<StageConfig> fetchableStages = downstream.validStagesForFetchArtifact(downstream, new CaseInsensitiveString("s2"));
        assertThat(fetchableStages.size()).isEqualTo(1);
        assertThat(fetchableStages).contains(downstream.get(0));
    }

    @Test
    void shouldUpdateNameAndMaterialsOnAttributes() {
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

        assertThat(pipelineConfig.name()).isEqualTo(new CaseInsensitiveString("startup"));
        assertThat(pipelineConfig.materialConfigs().get(0)).isEqualTo(svn("http://url", "loser", "passwd", false));
    }

    @Test
    void shouldUpdateStageOnAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        HashMap stageMap = new HashMap();
        List jobList = a(m(JobConfig.NAME, "JobName"));
        stageMap.put(StageConfig.NAME, "someStage");
        stageMap.put(StageConfig.JOBS, jobList);

        HashMap attributeMap = new HashMap();
        attributeMap.put(PipelineConfig.NAME, "startup");
        attributeMap.put(PipelineConfig.STAGE, stageMap);

        pipelineConfig.setConfigAttributes(attributeMap);

        assertThat(pipelineConfig.name()).isEqualTo(new CaseInsensitiveString("startup"));
        assertThat(pipelineConfig.get(0).name()).isEqualTo(new CaseInsensitiveString("someStage"));
        assertThat(pipelineConfig.get(0).getJobs().first().name()).isEqualTo(new CaseInsensitiveString("JobName"));
    }

    @Test
    void shouldValidatePipelineName() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("foo bar", "stage1", "job1");
        pipelineConfig.validate(null);
        assertThat(pipelineConfig.errors().isEmpty()).isFalse();
        assertThat(pipelineConfig.errors().on(PipelineConfig.NAME)).isEqualTo("Invalid pipeline name 'foo bar'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    void shouldRemoveExistingStagesWhileDoingAStageUpdate() {
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

        assertThat(pipelineConfig.name()).isEqualTo(new CaseInsensitiveString("startup"));
        assertThat(pipelineConfig.size()).isEqualTo(1);
        assertThat(pipelineConfig.get(0).name()).isEqualTo(new CaseInsensitiveString("someStage"));
        assertThat(pipelineConfig.get(0).getJobs().first().name()).isEqualTo(new CaseInsensitiveString("JobName"));
    }

    @Test
    void shouldGetAllFetchTasks() {
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
        assertThat(fetchTasks.size()).isEqualTo(2);
        assertThat(fetchTasks.contains(firstFetch)).isTrue();
        assertThat(fetchTasks.contains(secondFetch)).isTrue();
    }

    @Test
    void shouldGetOnlyPlainTextVariables() throws CryptoException {
        PipelineConfig pipelineConfig = new PipelineConfig();
        EnvironmentVariableConfig username = new EnvironmentVariableConfig("username", "ram");
        pipelineConfig.addEnvironmentVariable(username);
        GoCipher goCipher = mock(GoCipher.class);
        when(goCipher.encrypt("=%HG*^&*&^")).thenReturn("encrypted");
        EnvironmentVariableConfig password = new EnvironmentVariableConfig(goCipher, "password", "=%HG*^&*&^", true);
        pipelineConfig.addEnvironmentVariable(password);
        EnvironmentVariablesConfig plainTextVariables = pipelineConfig.getPlainTextVariables();
        assertThat(plainTextVariables).doesNotContain(password);
        assertThat(plainTextVariables).contains(username);
    }

    @Test
    void shouldGetOnlySecureVariables() throws CryptoException {
        PipelineConfig pipelineConfig = new PipelineConfig();
        EnvironmentVariableConfig username = new EnvironmentVariableConfig("username", "ram");
        pipelineConfig.addEnvironmentVariable(username);
        GoCipher goCipher = mock(GoCipher.class);
        when(goCipher.encrypt("=%HG*^&*&^")).thenReturn("encrypted");
        EnvironmentVariableConfig password = new EnvironmentVariableConfig(goCipher, "password", "=%HG*^&*&^", true);
        pipelineConfig.addEnvironmentVariable(password);
        List<EnvironmentVariableConfig> plainTextVariables = pipelineConfig.getSecureVariables();
        assertThat(plainTextVariables).contains(password);
        assertThat(plainTextVariables).doesNotContain(username);
    }

    @Test
    void shouldTemplatizeAPipeline() {
        PipelineConfig config = PipelineConfigMother.createPipelineConfigWithStages("pipeline", "stage1", "stage2");
        config.templatize(new CaseInsensitiveString("template"));
        assertThat(config.hasTemplate()).isTrue();
        assertThat(config.hasTemplateApplied()).isFalse();
        assertThat(config.getTemplateName()).isEqualTo(new CaseInsensitiveString("template"));
        assertThat(config.isEmpty()).isTrue();
        config.templatize(new CaseInsensitiveString(""));
        assertThat(config.hasTemplate()).isFalse();
        config.templatize(null);
        assertThat(config.hasTemplate()).isFalse();
    }

    @Test
    void shouldAssignApprovalTypeOnFirstStageAsAuto() throws Exception {
        Map approvalAttributes = Collections.singletonMap(Approval.TYPE, Approval.SUCCESS);
        Map<String, Map> map = Collections.singletonMap(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("p1", "s1", "j1");
        pipelineConfig.get(0).updateApproval(Approval.manualApproval());

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType()).isEqualTo(Approval.SUCCESS);
    }

    @Test
    void shouldAssignApprovalTypeOnFirstStageAsManual() throws Exception {
        Map approvalAttributes = Collections.singletonMap(Approval.TYPE, Approval.MANUAL);
        Map<String, Map> map = Collections.singletonMap(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("p1", "s1", "j1");
        pipelineConfig.get(0).updateApproval(Approval.manualApproval());

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType()).isEqualTo(Approval.MANUAL);
    }

    @Test
    void shouldAssignApprovalTypeOnFirstStageAsManualAndRestOfStagesAsUntouched() throws Exception {
        Map approvalAttributes = Collections.singletonMap(Approval.TYPE, Approval.MANUAL);
        Map<String, Map> map = Collections.singletonMap(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", StageConfigMother.custom("s1", Approval.automaticApproval()),
                StageConfigMother.custom("s2", Approval.automaticApproval()));

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType()).isEqualTo(Approval.MANUAL);
        assertThat(pipelineConfig.get(1).getApproval().getType()).isEqualTo(Approval.SUCCESS);
    }

    @Test
    void shouldGetPackageMaterialConfigs() throws Exception {
        SvnMaterialConfig svn = svn("svn", false);
        PackageMaterialConfig packageMaterialOne = new PackageMaterialConfig();
        PackageMaterialConfig packageMaterialTwo = new PackageMaterialConfig();

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(svn, packageMaterialOne, packageMaterialTwo));
        List<PackageMaterialConfig> packageMaterialConfigs = pipelineConfig.packageMaterialConfigs();

        assertThat(packageMaterialConfigs.size()).isEqualTo(2);
        assertThat(packageMaterialConfigs).contains(packageMaterialOne, packageMaterialTwo);
    }

    @Test
    void shouldGetPluggableSCMMaterialConfigs() throws Exception {
        SvnMaterialConfig svn = svn("svn", false);
        PluggableSCMMaterialConfig pluggableSCMMaterialOne = new PluggableSCMMaterialConfig("scm-id-1");
        PluggableSCMMaterialConfig pluggableSCMMaterialTwo = new PluggableSCMMaterialConfig("scm-id-2");

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(svn, pluggableSCMMaterialOne, pluggableSCMMaterialTwo));
        List<PluggableSCMMaterialConfig> pluggableSCMMaterialConfigs = pipelineConfig.pluggableSCMMaterialConfigs();

        assertThat(pluggableSCMMaterialConfigs.size()).isEqualTo(2);
        assertThat(pluggableSCMMaterialConfigs).contains(pluggableSCMMaterialOne, pluggableSCMMaterialTwo);
    }

    @Test
    void shouldReturnTrueWhenOneOfPipelineMaterialsIsTheSameAsConfigOrigin() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(material, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials()).isTrue();
    }

    @Test
    void shouldReturnTrueWhenOneOfPipelineMaterialsIsTheSameAsConfigOriginButDestinationIsDifferent() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        pipelineConfig.materialConfigs().clear();
        GitMaterialConfig pipeMaterialConfig = git("http://git");
        pipeMaterialConfig.setFolder("dest1");
        pipelineConfig.materialConfigs().add(pipeMaterialConfig);

        GitMaterialConfig repoMaterialConfig = git("http://git");

        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(repoMaterialConfig, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenOneOfPipelineMaterialsIsNotTheSameAsConfigOrigin() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = git("http://git");
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(material, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials()).isFalse();
    }

    @Test
    void shouldReturnFalseIfOneOfPipelineMaterialsIsTheSameAsConfigOrigin_WhenOriginIsFile() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        pipelineConfig.setOrigin(new FileConfigOrigin());

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials()).isFalse();
    }

    @Test
    void shouldReturnTrueWhenConfigRevisionIsEqualToQuery() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(material, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginFromRevision("1233")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenConfigRevisionIsNotEqualToQuery() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(material, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginFromRevision("32")).isFalse();
    }

    @Test
    void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsRemote() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin", "id"), "revision1"));
        assertThat(pipelineConfig.getOriginDisplayName()).isEqualTo("AwesomeGitMaterial at revision1");
    }

    @Test
    void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsFile() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setOrigin(new FileConfigOrigin());
        assertThat(pipelineConfig.getOriginDisplayName()).isEqualTo("cruise-config.xml");
    }

    @Test
    void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsNotSet() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        assertThat(pipelineConfig.getOriginDisplayName()).isEqualTo("cruise-config.xml");
    }

    @Test
    void shouldNotEncryptSecurePropertiesInStagesIfPipelineHasATemplate() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setTemplateName(new CaseInsensitiveString("some-template"));
        StageConfig mockStageConfig = mock(StageConfig.class);
        pipelineConfig.addStageWithoutValidityAssertion(mockStageConfig);

        pipelineConfig.encryptSecureProperties(new BasicCruiseConfig(), pipelineConfig);

        verify(mockStageConfig, never()).encryptSecureProperties(eq(new BasicCruiseConfig()), eq(pipelineConfig), ArgumentMatchers.any(StageConfig.class));
    }

    @Test
    void shouldEncryptSecurePropertiesInStagesIfPipelineHasStagesDefined() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        StageConfig mockStageConfig = mock(StageConfig.class);
        pipelineConfig.add(mockStageConfig);
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"));
        jobConfig.artifactTypeConfigs().add(new PluggableArtifactConfig("foo", "bar"));
        when(mockStageConfig.getJobs()).thenReturn(new JobConfigs(jobConfig));
        when(mockStageConfig.name()).thenReturn(new CaseInsensitiveString("stage"));

        pipelineConfig.encryptSecureProperties(new BasicCruiseConfig(), pipelineConfig);

        verify(mockStageConfig).encryptSecureProperties(eq(new BasicCruiseConfig()), eq(pipelineConfig), ArgumentMatchers.any(StageConfig.class));
    }

    @Test
    void shouldNotAttemptToEncryptPropertiesIfThereAreNoPluginConfigs() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        StageConfig mockStageConfig = mock(StageConfig.class);
        pipelineConfig.add(mockStageConfig);
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"));
        when(mockStageConfig.getJobs()).thenReturn(new JobConfigs(jobConfig));
        when(mockStageConfig.name()).thenReturn(new CaseInsensitiveString("stage"));

        pipelineConfig.encryptSecureProperties(new BasicCruiseConfig(), pipelineConfig);

        verify(mockStageConfig, never()).encryptSecureProperties(eq(new BasicCruiseConfig()), eq(pipelineConfig), ArgumentMatchers.any(StageConfig.class));
    }


    @Test
    void shouldValidateElasticProfileId() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setElasticProfileId("non-existent-profile-id");

        ValidationContext validationContext = mock(ValidationContext.class);
        when(validationContext.isValidProfileId("non-existent-profile-id")).thenReturn(false);

        pipelineConfig.validate(validationContext);

        assertThat(pipelineConfig.errors().isEmpty()).isFalse();
        assertThat(pipelineConfig.errors().on(JobConfig.ELASTIC_PROFILE_ID)).isEqualTo("No profile defined corresponding to profile_id 'non-existent-profile-id'");
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
        GitMaterialConfig git = git("git@github.com:gocd/gocd.git");
        git.setName(new CaseInsensitiveString("git"));

        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs(git));
        pipelineConfig.setLabelTemplate(labelFormat);

        pipelineConfig.validate(null);

        return pipelineConfig;
    }
}
