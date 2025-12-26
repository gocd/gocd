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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "CollectionAddedToSelf"})
public class PipelineConfigTest {
    private static final String BUILDING_PLAN_NAME = "building";

    private final EnvironmentVariablesConfig mockEnvironmentVariablesConfig = mock(EnvironmentVariablesConfig.class);
    private final ParamsConfig mockParamsConfig = mock(ParamsConfig.class);

    @Test
    public void shouldFindByName() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.findBy(new CaseInsensitiveString("completed stage")).name()).isEqualTo(new CaseInsensitiveString("completed stage"));
    }

    @Test
    public void shouldGetStageByName() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.getStage(new CaseInsensitiveString("COMpleTEd stage")).name()).isEqualTo(new CaseInsensitiveString("completed stage"));
        assertThat(pipelineConfig.getStage(new CaseInsensitiveString("Does-not-exist"))).isNull();
    }

    @Test
    public void shouldReturnFalseIfThereIsNoNextStage() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(buildingStage().name())).isFalse();
    }

    @Test
    public void shouldReturnFalseIfThereIsNextStage() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(completedStage().name())).isTrue();
    }

    @Test
    public void shouldReturnFalseThePassInStageDoesNotExist() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null, completedStage(), buildingStage());
        assertThat(pipelineConfig.hasNextStage(new CaseInsensitiveString("notExist"))).isFalse();
    }

    @Test
    public void shouldReturnTrueIfThereNoStagesDefined() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), null);
        assertThat(pipelineConfig.hasNextStage(completedStage().name())).isFalse();
    }

    @Test
    public void shouldGetDependenciesAsNode() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("framework"), new CaseInsensitiveString("dev")));
        pipelineConfig.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("middleware"), new CaseInsensitiveString("dev")));
        assertThat(pipelineConfig.getDependenciesAsNode()).isEqualTo(new Node(
                        new Node.DependencyNode(new CaseInsensitiveString("framework"), new CaseInsensitiveString("dev")),
                        new Node.DependencyNode(new CaseInsensitiveString("middleware"), new CaseInsensitiveString("dev"))));
    }

    @Test
    public void shouldReturnTrueIfFirstStageIsManualApproved() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        pipelineConfig.getFirstStageConfig().updateApproval(Approval.manualApproval());
        assertThat(pipelineConfig.isFirstStageManualApproval()).isTrue();
    }

    @Test
    public void shouldThrowExceptionForEmptyPipeline() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs());
        try {
            pipelineConfig.isFirstStageManualApproval();
            fail("Should throw exception if pipeline has no pipeline");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Pipeline [" + pipelineConfig.name() + "] doesn't have any stage");
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
            assertThat(e.getMessage()).contains("Cannot set template 'some-template' on pipeline 'pipeline' because it already has stages defined");
        }
    }

    @Test
    public void shouldBombWhenAddingStagesIfItAlreadyHasATemplate() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("mingle"), null);
        try {
            pipelineConfig.setTemplateName(new CaseInsensitiveString("some-template"));
            pipelineConfig.add(StageConfigMother.stageConfig("second"));
            fail("Should throw exception because pipeline already has a template");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Cannot add stage 'second' to pipeline 'mingle', which already references template 'some-template'.");
        }
    }

    @Test
    public void shouldKnowIfATemplateWasApplied() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        assertThat(pipelineConfig.hasTemplateApplied()).isFalse();
        pipelineConfig.clear();

        PipelineTemplateConfig template = new PipelineTemplateConfig();
        template.add(StageConfigMother.stageConfig("first"));
        pipelineConfig.usingTemplate(template);
        assertThat(pipelineConfig.hasTemplateApplied()).isTrue();
    }

    @Test
    public void shouldValidateCorrectPipelineLabelWithoutAnyMaterial() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs(), new StageConfig(new CaseInsensitiveString("first"), new JobConfigs()));
        pipelineConfig.setLabelTemplate("pipeline-${COUNT}-alpha");
        pipelineConfig.validate(null);
        assertThat(pipelineConfig.errors().isEmpty()).isTrue();
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).isNull();
    }

    @Test
    public void shouldValidateMissingLabel() {
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(null);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).isEqualTo(PipelineConfig.BLANK_LABEL_TEMPLATE_ERROR_MESSAGE);

        pipelineConfig = createAndValidatePipelineLabel("");
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).isEqualTo(PipelineConfig.BLANK_LABEL_TEMPLATE_ERROR_MESSAGE);
    }

    @Test
    public void shouldValidateCorrectPipelineLabelWithoutTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${git}-454";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).isNull();
    }

    @Test
    public void shouldValidatePipelineLabelWithNonExistingMaterial() {
        String labelFormat = "pipeline-${COUNT}-${NoSuchMaterial}";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).startsWith("You have defined a label template in pipeline");
    }

    @Test
    public void shouldValidatePipelineLabelWithEnvironmentVariable() {
        String labelFormat = "pipeline-${COUNT}-${env:SOME_VAR}";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).isNull();
    }

    @Test
    public void shouldValidateCorrectPipelineLabelWithTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${git[:7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).isNull();
    }


    @Test
    public void shouldValidatePipelineLabelWithBrokenTruncationSyntax1() {
        String labelFormat = "pipeline-${COUNT}-${git[:7}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        String expectedLabelTemplate = "Invalid label 'pipeline-${COUNT}-${git[:7}-alpha'.";
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).startsWith(expectedLabelTemplate);
    }

    @Test
    public void shouldValidatePipelineLabelWithBrokenTruncationSyntax2() {
        String labelFormat = "pipeline-${COUNT}-${git[7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        String expectedLabelTemplate = "Invalid label 'pipeline-${COUNT}-${git[7]}-alpha'.";
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).startsWith(expectedLabelTemplate);
    }

    @Test
    public void shouldValidateIncorrectPipelineLabelWithTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${noSuch[:7]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).startsWith("You have defined a label template in pipeline");
    }

    @Test
    public void shouldNotAllowInvalidLabelTemplate() {
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

    public void assertPipelineLabelTemplateIsInvalid(String labelTemplate, String expectedError) {
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelTemplate);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).contains(expectedError);
    }

    @Test
    public void shouldNotAllowLabelTemplateWithLengthOfZeroInTruncationSyntax() {
        String labelFormat = "pipeline-${COUNT}-${git[:0]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).isEqualTo(String.format("Length of zero not allowed on label %s defined on pipeline %s.", labelFormat, pipelineConfig.name()));
    }

    @Test
    public void shouldNotAllowLabelTemplateWithLengthOfZeroInTruncationSyntax2() {
        String labelFormat = "pipeline-${COUNT}-${git[:0]}${one[:00]}-alpha";
        PipelineConfig pipelineConfig = createAndValidatePipelineLabel(labelFormat);
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LABEL_TEMPLATE)).isEqualTo(String.format("Length of zero not allowed on label %s defined on pipeline %s.", labelFormat, pipelineConfig.name()));
    }


    @Test
    public void shouldSetPipelineConfigFromConfigAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        Map<String, String> trackingToolMap = new HashMap<>();
        trackingToolMap.put("trackingtool", "trackingtool");
        Map<String, String> timerConfigMap = new HashMap<>();
        String cronSpec = "0 0 11 * * ?";
        timerConfigMap.put(TimerConfig.TIMER_SPEC, cronSpec);

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "LABEL123-${COUNT}");
        configMap.put(PipelineConfig.TRACKING_TOOL, trackingToolMap);
        configMap.put(PipelineConfig.TIMER_CONFIG, timerConfigMap);

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo("LABEL123-${COUNT}");
        assertThat(pipelineConfig.getTimer().getTimerSpec()).isEqualTo(cronSpec);
        assertThat(pipelineConfig.getTimer().shouldTriggerOnlyOnChanges()).isFalse();
    }

    @Test
    public void shouldSetPipelineConfigFromConfigAttributesForTimerConfig() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        Map<String, String> timerConfigMap = new HashMap<>();
        String cronSpec = "0 0 11 * * ?";
        timerConfigMap.put(TimerConfig.TIMER_SPEC, cronSpec);
        timerConfigMap.put(TimerConfig.TIMER_ONLY_ON_CHANGES, "1");

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "LABEL123-${COUNT}");
        configMap.put(PipelineConfig.TIMER_CONFIG, timerConfigMap);

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo("LABEL123-${COUNT}");
        assertThat(pipelineConfig.getTimer().getTimerSpec()).isEqualTo(cronSpec);
        assertThat(pipelineConfig.getTimer().shouldTriggerOnlyOnChanges()).isTrue();
    }

    @Test
    public void shouldSetLabelTemplateToDefaultValueIfBlankIsEnteredWhileSettingConfigAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "");

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo(PipelineLabel.COUNT_TEMPLATE);
    }

    @Test
    public void shouldNotSetLockStatusOnPipelineConfigWhenValueIsNone() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(PipelineConfig.LOCK_BEHAVIOR, PipelineConfig.LOCK_VALUE_NONE);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setConfigAttributes(configMap);
        assertThat(pipelineConfig.isLockable()).isFalse();
    }

    @Test
    public void shouldSetLockStatusOnPipelineConfigWhenValueIsLockOnFailure() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(PipelineConfig.LOCK_BEHAVIOR, PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setConfigAttributes(configMap);
        assertThat(pipelineConfig.isLockable()).isTrue();
    }

    @Test
    public void shouldSetLockStatusOnPipelineConfigWhenValueIsUnlockWhenFinished() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(PipelineConfig.LOCK_BEHAVIOR, PipelineConfig.LOCK_VALUE_UNLOCK_WHEN_FINISHED);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setConfigAttributes(configMap);
        assertThat(pipelineConfig.isPipelineUnlockableWhenFinished()).isTrue();
    }

    @Test
    public void shouldNotResetTrackingToolWhenNotSpecifiedInConfigAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        TrackingTool trackingTool = new TrackingTool("link", "regex");
        pipelineConfig.setTrackingTool(trackingTool);

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(PipelineConfig.LABEL_TEMPLATE, "LABEL123-${COUNT}");

        pipelineConfig.setConfigAttributes(configMap);

        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo("LABEL123-${COUNT}");
        assertThat(pipelineConfig.getTrackingTool()).isEqualTo(trackingTool);
    }

    @Test
    public void isNotLockableWhenLockValueHasNotBeenSet() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        assertThat(pipelineConfig.hasExplicitLock()).isFalse();
        assertThat(pipelineConfig.isLockable()).isFalse();
    }

    @Test
    public void shouldValidateLockBehaviorValues() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(PipelineConfig.LOCK_BEHAVIOR, "someRandomValue");

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1");
        pipelineConfig.setConfigAttributes(configMap);
        pipelineConfig.validate(null);

        assertThat(pipelineConfig.errors().isEmpty()).isFalse();
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.LOCK_BEHAVIOR))
            .contains("Lock behavior has an invalid value (someRandomValue). Valid values are: ");
    }

    @Test
    public void shouldAllowNullForLockBehavior() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1");
        pipelineConfig.setLockBehaviorIfNecessary(null);
        pipelineConfig.validate(null);

        assertThat(pipelineConfig.errors()).isEmpty();
    }

    @Test
    public void shouldPopulateEnvironmentVariablesFromAttributeMap() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        Map<String, Object> map = new HashMap<>();
        Map<String, String> valueHashMap = new HashMap<>();
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

        final Map<String, Object> map = new HashMap<>();
        final Map<String, String> valueHashMap = new HashMap<>();
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
        Map<String, Object> map = new HashMap<>();
        Map<String, String> valueHashMap = new HashMap<>();
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
        Map<String, Object> map = new HashMap<>();
        Map<String, String> valueHashMap = new HashMap<>();
        valueHashMap.put("param-name", "FOO");
        valueHashMap.put("param-value", "BAR");
        map.put(PipelineConfig.PARAMS, valueHashMap);
        map.put(PipelineConfig.CONFIGURATION_TYPE, PipelineConfig.CONFIGURATION_TYPE_STAGES);
        pipelineConfig.setParams(mockParamsConfig);

        pipelineConfig.setConfigAttributes(map);
        verify(mockParamsConfig, never()).setConfigAttributes(valueHashMap);
    }

    @Test
    public void shouldPopulateTrackingToolWhenTrackingToolAndLinkAndRegexAreDefined() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        Map<String, Object> map = new HashMap<>();
        Map<String, String> valueHashMap = new HashMap<>();
        valueHashMap.put("link", "GoleyLink");
        valueHashMap.put("regex", "GoleyRegex");

        map.put(PipelineConfig.TRACKING_TOOL, valueHashMap);

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getTrackingTool()).isEqualTo(new TrackingTool("GoleyLink", "GoleyRegex"));
    }

    @Test
    public void shouldGetTheCorrectConfigurationType() {
        PipelineConfig pipelineConfigWithTemplate = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        assertThat(pipelineConfigWithTemplate.getConfigurationType()).isEqualTo(PipelineConfig.CONFIGURATION_TYPE_TEMPLATE);

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline");
        assertThat(pipelineConfig.getConfigurationType()).isEqualTo(PipelineConfig.CONFIGURATION_TYPE_STAGES);
    }

    @Test
    public void shouldUseTemplateWhenSetConfigAttributesContainsTemplateName() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline");
        assertThat(pipelineConfig.hasTemplate()).isFalse();

        Map<String, Object> map = new HashMap<>();
        map.put(PipelineConfig.CONFIGURATION_TYPE, PipelineConfig.CONFIGURATION_TYPE_TEMPLATE);
        map.put(PipelineConfig.TEMPLATE_NAME, "foo-template");

        pipelineConfig.setConfigAttributes(map);
        assertThat(pipelineConfig.getConfigurationType()).isEqualTo(PipelineConfig.CONFIGURATION_TYPE_TEMPLATE);
        assertThat(pipelineConfig.getTemplateName()).isEqualTo(new CaseInsensitiveString("foo-template"));
    }

    @Test
    public void shouldIncrementIndexBy1OfGivenStage() {
        StageConfig moveMeStage = StageConfigMother.stageConfig("move-me");
        StageConfig dontMoveMeStage = StageConfigMother.stageConfig("dont-move-me");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", moveMeStage, dontMoveMeStage);

        pipelineConfig.incrementIndex(moveMeStage);

        assertThat(pipelineConfig.indexOf(moveMeStage)).isEqualTo(1);
        assertThat(pipelineConfig.indexOf(dontMoveMeStage)).isEqualTo(0);
    }

    @Test
    public void shouldDecrementIndexBy1OfGivenStage() {
        StageConfig moveMeStage = StageConfigMother.stageConfig("move-me");
        StageConfig dontMoveMeStage = StageConfigMother.stageConfig("dont-move-me");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", dontMoveMeStage, moveMeStage);

        pipelineConfig.decrementIndex(moveMeStage);

        assertThat(pipelineConfig.indexOf(moveMeStage)).isEqualTo(0);
        assertThat(pipelineConfig.indexOf(dontMoveMeStage)).isEqualTo(1);
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
    public void shouldReturnListOfStageConfigWhichIsApplicableForFetchArtifact() {
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
    public void shouldReturnStagesBeforeCurrentForSelectedPipeline() {
        PipelineConfig downstream = PipelineConfigMother.createPipelineConfigWithStages("downstream", "s1", "s2");
        @SuppressWarnings("CollectionAddedToSelf") List<StageConfig> fetchableStages = downstream.validStagesForFetchArtifact(downstream, new CaseInsensitiveString("s2"));
        assertThat(fetchableStages.size()).isEqualTo(1);
        assertThat(fetchableStages).contains(downstream.get(0));
    }

    @Test
    public void shouldUpdateNameAndMaterialsOnAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        Map<String, Object> svnMaterialConfigMap = new HashMap<>();
        svnMaterialConfigMap.put(SvnMaterialConfig.URL, "http://url");
        svnMaterialConfigMap.put(SvnMaterialConfig.USERNAME, "loser");
        svnMaterialConfigMap.put(SvnMaterialConfig.PASSWORD, "passwd");
        svnMaterialConfigMap.put(SvnMaterialConfig.CHECK_EXTERNALS, false);

        Map<String, Object> materialConfigsMap = new HashMap<>();
        materialConfigsMap.put(AbstractMaterialConfig.MATERIAL_TYPE, SvnMaterialConfig.TYPE);
        materialConfigsMap.put(SvnMaterialConfig.TYPE, svnMaterialConfigMap);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(PipelineConfig.NAME, "startup");
        attributeMap.put(PipelineConfig.MATERIALS, materialConfigsMap);

        pipelineConfig.setConfigAttributes(attributeMap);

        assertThat(pipelineConfig.name()).isEqualTo(new CaseInsensitiveString("startup"));
        assertThat(pipelineConfig.materialConfigs().get(0)).isEqualTo(svn("http://url", "loser", "passwd", false));
    }

    @Test
    public void shouldUpdateStageOnAttributes() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        Map<String, Object> stageMap = new HashMap<>();
        List<Map<String, String>> jobList = List.of(Map.of(JobConfig.NAME, "JobName"));
        stageMap.put(StageConfig.NAME, "someStage");
        stageMap.put(StageConfig.JOBS, jobList);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(PipelineConfig.NAME, "startup");
        attributeMap.put(PipelineConfig.STAGE, stageMap);

        pipelineConfig.setConfigAttributes(attributeMap);

        assertThat(pipelineConfig.name()).isEqualTo(new CaseInsensitiveString("startup"));
        assertThat(pipelineConfig.get(0).name()).isEqualTo(new CaseInsensitiveString("someStage"));
        assertThat(pipelineConfig.get(0).getJobs().first().name()).isEqualTo(new CaseInsensitiveString("JobName"));
    }

    @Test
    public void shouldValidatePipelineName() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("foo bar", "stage1", "job1");
        pipelineConfig.validate(null);
        assertThat(pipelineConfig.errors().isEmpty()).isFalse();
        assertThat(pipelineConfig.errors().firstErrorOn(PipelineConfig.NAME)).isEqualTo("Invalid pipeline name 'foo bar'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldRemoveExistingStagesWhileDoingAStageUpdate() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("foo"), new MaterialConfigs(), new StageConfig(new CaseInsensitiveString("first"), new JobConfigs()),
                new StageConfig(
                        new CaseInsensitiveString("second"), new JobConfigs()));

        Map<String, Object> stageMap = new HashMap<>();
        List<Map<String, String>> jobList = List.of(Map.of(JobConfig.NAME, "JobName"));
        stageMap.put(StageConfig.NAME, "someStage");
        stageMap.put(StageConfig.JOBS, jobList);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(PipelineConfig.NAME, "startup");
        attributeMap.put(PipelineConfig.STAGE, stageMap);

        pipelineConfig.setConfigAttributes(attributeMap);

        assertThat(pipelineConfig.name()).isEqualTo(new CaseInsensitiveString("startup"));
        assertThat(pipelineConfig.size()).isEqualTo(1);
        assertThat(pipelineConfig.get(0).name()).isEqualTo(new CaseInsensitiveString("someStage"));
        assertThat(pipelineConfig.get(0).getJobs().first().name()).isEqualTo(new CaseInsensitiveString("JobName"));
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
        assertThat(fetchTasks.size()).isEqualTo(2);
        assertThat(fetchTasks.contains(firstFetch)).isTrue();
        assertThat(fetchTasks.contains(secondFetch)).isTrue();
    }

    @Test
    public void shouldGetOnlyPlainTextVariables() throws CryptoException {
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
    public void shouldGetOnlySecureVariables() throws CryptoException {
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
    public void shouldTemplatizeAPipeline() {
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
    public void shouldAssignApprovalTypeOnFirstStageAsAuto() {
        Map<String, Object> approvalAttributes = Map.of(Approval.TYPE, Approval.SUCCESS);
        Map<String, Map<String, Object>> map = Map.of(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("p1", "s1", "j1");
        pipelineConfig.get(0).updateApproval(Approval.manualApproval());

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType()).isEqualTo(Approval.SUCCESS);
    }

    @Test
    public void shouldAssignApprovalTypeOnFirstStageAsManual() {
        Map<String, Object> approvalAttributes = Map.of(Approval.TYPE, Approval.MANUAL);
        Map<String, Map<String, Object>> map = Map.of(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("p1", "s1", "j1");
        pipelineConfig.get(0).updateApproval(Approval.manualApproval());

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType()).isEqualTo(Approval.MANUAL);
    }

    @Test
    public void shouldAssignApprovalTypeOnFirstStageAsManualAndRestOfStagesAsUntouched() {
        Map<String, Object> approvalAttributes = Map.of(Approval.TYPE, Approval.MANUAL);
        Map<String, Map<String, Object>> map = Map.of(StageConfig.APPROVAL, approvalAttributes);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", StageConfigMother.custom("s1", Approval.automaticApproval()),
                StageConfigMother.custom("s2", Approval.automaticApproval()));

        pipelineConfig.setConfigAttributes(map);

        assertThat(pipelineConfig.get(0).getApproval().getType()).isEqualTo(Approval.MANUAL);
        assertThat(pipelineConfig.get(1).getApproval().getType()).isEqualTo(Approval.SUCCESS);
    }

    @Test
    public void shouldGetPackageMaterialConfigs() {
        SvnMaterialConfig svn = svn("svn", false);
        PackageMaterialConfig packageMaterialOne = new PackageMaterialConfig();
        PackageMaterialConfig packageMaterialTwo = new PackageMaterialConfig();

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(svn, packageMaterialOne, packageMaterialTwo));
        List<PackageMaterialConfig> packageMaterialConfigs = pipelineConfig.packageMaterialConfigs();

        assertThat(packageMaterialConfigs.size()).isEqualTo(2);
        assertThat(packageMaterialConfigs).contains(packageMaterialOne, packageMaterialTwo);
    }

    @Test
    public void shouldGetPluggableSCMMaterialConfigs() {
        SvnMaterialConfig svn = svn("svn", false);
        PluggableSCMMaterialConfig pluggableSCMMaterialOne = new PluggableSCMMaterialConfig("scm-id-1");
        PluggableSCMMaterialConfig pluggableSCMMaterialTwo = new PluggableSCMMaterialConfig("scm-id-2");

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(svn, pluggableSCMMaterialOne, pluggableSCMMaterialTwo));
        List<PluggableSCMMaterialConfig> pluggableSCMMaterialConfigs = pipelineConfig.pluggableSCMMaterialConfigs();

        assertThat(pluggableSCMMaterialConfigs.size()).isEqualTo(2);
        assertThat(pluggableSCMMaterialConfigs).contains(pluggableSCMMaterialOne, pluggableSCMMaterialTwo);
    }

    @Test
    public void shouldReturnTrueWhenOneOfPipelineMaterialsIsTheSameAsConfigOrigin() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(material, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials()).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenOneOfPipelineMaterialsIsTheSameAsConfigOriginButDestinationIsDifferent() {
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
    public void shouldReturnFalseWhenOneOfPipelineMaterialsIsNotTheSameAsConfigOrigin() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = git("http://git");
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(material, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials()).isFalse();
    }

    @Test
    public void shouldReturnFalseIfOneOfPipelineMaterialsIsTheSameAsConfigOrigin_WhenOriginIsFile() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        pipelineConfig.setOrigin(new FileConfigOrigin());

        assertThat(pipelineConfig.isConfigOriginSameAsOneOfMaterials()).isFalse();
    }

    @Test
    public void shouldReturnTrueWhenConfigRevisionIsEqualToQuery() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(material, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginFromRevision("1233")).isTrue();
    }

    @Test
    public void shouldReturnFalseWhenConfigRevisionIsNotEqualToQuery() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "build");
        MaterialConfig material = pipelineConfig.materialConfigs().first();
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(material, "plugin", "id"), "1233"));

        assertThat(pipelineConfig.isConfigOriginFromRevision("32")).isFalse();
    }

    @Test
    public void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsRemote() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin", "id"), "revision1"));
        assertThat(pipelineConfig.getOriginDisplayName()).isEqualTo("AwesomeGitMaterial at revision revision1");
    }

    @Test
    public void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsFile() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setOrigin(new FileConfigOrigin());
        assertThat(pipelineConfig.getOriginDisplayName()).isEqualTo("cruise-config.xml");
    }

    @Test
    public void shouldReturnConfigRepoOriginDisplayNameWhenOriginIsNotSet() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        assertThat(pipelineConfig.getOriginDisplayName()).isEqualTo("cruise-config.xml");
    }

    @Test
    public void shouldNotEncryptSecurePropertiesInStagesIfPipelineHasATemplate() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setTemplateName(new CaseInsensitiveString("some-template"));
        StageConfig mockStageConfig = mock(StageConfig.class);
        pipelineConfig.addStageWithoutValidityAssertion(mockStageConfig);

        pipelineConfig.encryptSecureProperties(new BasicCruiseConfig(), pipelineConfig);

        verify(mockStageConfig, never()).encryptSecureProperties(eq(new BasicCruiseConfig()), eq(pipelineConfig), ArgumentMatchers.any());
    }

    @Test
    public void shouldEncryptSecurePropertiesInStagesIfPipelineHasStagesDefined() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        StageConfig mockStageConfig = mock(StageConfig.class);
        pipelineConfig.add(mockStageConfig);
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"));
        jobConfig.artifactTypeConfigs().add(new PluggableArtifactConfig("foo", "bar"));
        when(mockStageConfig.getJobs()).thenReturn(new JobConfigs(jobConfig));
        when(mockStageConfig.name()).thenReturn(new CaseInsensitiveString("stage"));

        pipelineConfig.encryptSecureProperties(new BasicCruiseConfig(), pipelineConfig);

        verify(mockStageConfig).encryptSecureProperties(eq(new BasicCruiseConfig()), eq(pipelineConfig), ArgumentMatchers.any());
    }

    @Test
    public void shouldNotAttemptToEncryptPropertiesIfThereAreNoPluginConfigs() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        StageConfig mockStageConfig = mock(StageConfig.class);
        pipelineConfig.add(mockStageConfig);
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"));
        when(mockStageConfig.getJobs()).thenReturn(new JobConfigs(jobConfig));
        when(mockStageConfig.name()).thenReturn(new CaseInsensitiveString("stage"));

        pipelineConfig.encryptSecureProperties(new BasicCruiseConfig(), pipelineConfig);

        verify(mockStageConfig, never()).encryptSecureProperties(eq(new BasicCruiseConfig()), eq(pipelineConfig), ArgumentMatchers.any());
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
