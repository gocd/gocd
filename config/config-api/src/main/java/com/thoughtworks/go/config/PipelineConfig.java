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
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.preprocessor.ParamResolver;
import com.thoughtworks.go.config.preprocessor.ParamScope;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigOriginTraceable;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.CommentRenderer;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.service.TaskFactory;
import com.thoughtworks.go.util.Node;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.thoughtworks.go.domain.label.PipelineLabel.COUNT;
import static com.thoughtworks.go.domain.label.PipelineLabel.ENV_VAR_PREFIX;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringsBetween;

/**
 * @understands how a cruise pipeline is configured by the user
 */
@ConfigTag("pipeline")
@ConfigCollection(StageConfig.class)
public class PipelineConfig extends BaseCollection<StageConfig> implements ParamScope, ParamsAttributeAware,
        Validatable, EnvironmentVariableScope, ConfigOriginTraceable {
    private static final Cloner CLONER = new Cloner();

    public static final String LABEL_TEMPLATE = "labelTemplate";
    public static final String TRACKING_TOOL = "trackingTool";
    public static final String TIMER_CONFIG = "timer";
    public static final String ENVIRONMENT_VARIABLES = "variables";
    public static final String PARAMS = "params";

    public static final String TEMPLATE_NAME = "templateName";

    public static final String LOCK_BEHAVIOR = "lockBehavior";
    public static final String LOCK_VALUE_LOCK_ON_FAILURE = "lockOnFailure";
    public static final String LOCK_VALUE_UNLOCK_WHEN_FINISHED = "unlockWhenFinished";
    public static final String LOCK_VALUE_NONE = "none";
    public static final List<String> VALID_LOCK_VALUES = asList(LOCK_VALUE_LOCK_ON_FAILURE, LOCK_VALUE_UNLOCK_WHEN_FINISHED, LOCK_VALUE_NONE);

    public static final String CONFIGURATION_TYPE = "configurationType";
    public static final String CONFIGURATION_TYPE_STAGES = "configurationType_stages";
    public static final String CONFIGURATION_TYPE_TEMPLATE = "configurationType_template";
    public static final String LABEL_TEMPLATE_FORMAT_MESSAGE = "Label should be composed of alphanumeric text, it can contain the build number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.";
    public static final String LABEL_TEMPLATE_ERROR_MESSAGE = "Invalid label '%s'. ".concat(LABEL_TEMPLATE_FORMAT_MESSAGE);
    public static final String BLANK_LABEL_TEMPLATE_ERROR_MESSAGE = "Label cannot be blank. ".concat(LABEL_TEMPLATE_FORMAT_MESSAGE);

    @SkipParameterResolution
    @ConfigAttribute(value = "name", optional = false)
    private CaseInsensitiveString name;

    @ConfigAttribute(value = "labeltemplate", optional = true)
    private String labelTemplate = PipelineLabel.COUNT_TEMPLATE;

    @ConfigSubtag
    @SkipParameterResolution
    private ParamsConfig params = new ParamsConfig();

    @ConfigSubtag
    private TrackingTool trackingTool;

    @ConfigSubtag(optional = true)
    private TimerConfig timer;

    @ConfigSubtag
    private EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();

    @ConfigSubtag(optional = false)
    private MaterialConfigs materialConfigs = new MaterialConfigs();

    @ConfigAttribute(value = "lockBehavior", optional = true, allowNull = true)
    private String lockBehavior;

    @SkipParameterResolution
    @ConfigAttribute(value = "template", optional = true, allowNull = true)
    private CaseInsensitiveString templateName;

    private ConfigOrigin origin;
    private int displayOrderWeight = -1;

    private boolean templateApplied;

    private CachedPluggableArtifactConfigs externalArtifactConfigs = null;
    private CachedFetchPluggableArtifactTasks fetchExternalArtifactTasks = null;

    private ConfigErrors errors = new ConfigErrors();
    public static final String NAME = "name";
    public static final String MATERIALS = "materials";
    public static final String STAGE = "stage";
    public static final Pattern LABEL_TEMPLATE_TOKEN_PATTERN = Pattern.compile("(?<groupName>[^\\[]*)(\\[:(?<truncationLength>\\d+)\\])?$");

    public PipelineConfig() {
    }

    public PipelineConfig(final CaseInsensitiveString name, MaterialConfigs materialConfigs, StageConfig... stageConfigs) {
        super(stageConfigs);
        this.name = name;
        this.labelTemplate = PipelineLabel.COUNT_TEMPLATE;
        this.materialConfigs = materialConfigs;
    }

    public PipelineConfig(CaseInsensitiveString name, String labelTemplate, String cronSpec, boolean timerShouldTriggerOnlyOnMaterialChanges, MaterialConfigs materialConfigs,
                          List<StageConfig> stageConfigs) {
        super(stageConfigs);
        this.name = name;
        this.labelTemplate = labelTemplate;
        this.setMaterialConfigs(materialConfigs);
        if (cronSpec != null) {
            this.timer = new TimerConfig(cronSpec, timerShouldTriggerOnlyOnMaterialChanges);
        }
    }

    @Override
    public String toString() {
        return format("PipelineConfig: %s", name);
    }

    public boolean validateTree(PipelineConfigSaveValidationContext validationContext) {
        return new PipelineConfigTreeValidator(this).validate(validationContext);
    }

    public void encryptSecureProperties(CruiseConfig preprocessedConfig, PipelineConfig preprocessedPipelineConfig) {
        if (hasTemplate() || doesNotHavePublishAndFetchExternalConfig()) {
            return;
        }
        for (StageConfig stageConfig : getStages()) {
            stageConfig.encryptSecureProperties(preprocessedConfig, preprocessedPipelineConfig, preprocessedPipelineConfig.getStage(stageConfig.name()));
        }
    }

    private boolean doesNotHavePublishAndFetchExternalConfig() {
        if (externalArtifactConfigs == null || fetchExternalArtifactTasks == null) {
            cachePublishAndFetchExternalConfig();
        }
        return externalArtifactConfigs.isEmpty() && fetchExternalArtifactTasks.isEmpty();
    }

    private void cachePublishAndFetchExternalConfig() {
        externalArtifactConfigs = new CachedPluggableArtifactConfigs();
        fetchExternalArtifactTasks = new CachedFetchPluggableArtifactTasks();
        for (StageConfig stageConfig : getStages()) {
            for (JobConfig jobConfig : stageConfig.getJobs()) {
                externalArtifactConfigs.addAll(jobConfig.artifactTypeConfigs().getPluggableArtifactConfigs());
                for (Task task : jobConfig.getTasks()) {
                    if (task instanceof FetchPluggableArtifactTask) {
                        fetchExternalArtifactTasks.add((FetchPluggableArtifactTask) task);
                    }
                }
            }
        }
    }

    public List<PluggableArtifactConfig> getExternalArtifactConfigs() {
        if (externalArtifactConfigs == null) {
            cachePublishAndFetchExternalConfig();
        }

        return externalArtifactConfigs;
    }

    public List<FetchPluggableArtifactTask> getFetchExternalArtifactTasks() {
        if (fetchExternalArtifactTasks == null) {
            cachePublishAndFetchExternalConfig();
        }

        return fetchExternalArtifactTasks;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateLabelTemplate();
        validatePipelineName();
        validateStageNameUniqueness();
        validateLockBehaviorValues();
        if (!hasTemplate() && isEmpty()) {
            addError("pipeline", format("Pipeline '%s' does not have any stages configured. A pipeline must have at least one stage.", name()));
        }
    }

    public void validateTemplate(PipelineTemplateConfig templateConfig) {
        if (hasTemplate()) {
            if (new NameTypeValidator().isNameInvalid(templateName.toString())) {
                errors().add(TEMPLATE_NAME, NameTypeValidator.errorMessage("template", templateName));
            }
            if (hasStages() && !hasTemplateApplied()) {
                addError("stages", format("Cannot add stages to pipeline '%s' which already references template '%s'", this.name(), this.getTemplateName()));
                addError("template", format("Cannot set template '%s' on pipeline '%s' because it already has stages defined", this.getTemplateName(), this.name()));
            }
            if (templateConfig == null) {
                addError("pipeline", format("Pipeline '%s' refers to non-existent template '%s'.", name(), templateName));
            }
        }
    }

    private void validatePipelineName() {
        if (!new NameTypeValidator().isNameValid(name)) {
            errors().add(NAME, NameTypeValidator.errorMessage("pipeline", name));
        }
    }

    private void validateStageNameUniqueness() {
        Map<String, StageConfig> stageNameMap = new HashMap<>();
        for (StageConfig stageConfig : this) {
            stageConfig.validateNameUniqueness(stageNameMap);
        }
    }

    private void validateLabelTemplate() {
        if (StringUtils.isBlank(labelTemplate)) {
            addError("labelTemplate", BLANK_LABEL_TEMPLATE_ERROR_MESSAGE);
            return;
        }

        String[] allTokens = substringsBetween(labelTemplate, "${", "}");

        if (allTokens == null) {
            addError("labelTemplate", String.format(LABEL_TEMPLATE_ERROR_MESSAGE, labelTemplate));
            return;
        }

        for (String token : allTokens) {
            if (!isValidToken(token)) {
                break;
            }
        }
    }

    private boolean isValidToken(String token) {
        if (StringUtils.isBlank(token)) {
            addError("labelTemplate", "Label template variable cannot be blank.");
            return false;
        }

        if (token.equalsIgnoreCase(COUNT)) {
            return true;
        }

        if (token.equalsIgnoreCase(ENV_VAR_PREFIX)) {
            addError("labelTemplate", "Missing environment variable name.");
            return false;
        }

        if (token.toLowerCase().startsWith(ENV_VAR_PREFIX)) {
            return true;
        }

        Matcher matcher = LABEL_TEMPLATE_TOKEN_PATTERN.matcher(token);

        if (matcher.matches()) {
            String materialName = matcher.group("groupName");
            String truncationLength = matcher.group("truncationLength");

            if (isNotBlank(truncationLength) && truncationLength.startsWith("0")) {
                addError("labelTemplate", format("Length of zero not allowed on label %s defined on pipeline %s.", labelTemplate, name));
                return false;
            }

            if (!materialConfigs.materialNames().contains(new CaseInsensitiveString(materialName))) {
                addError("labelTemplate", format("You have defined a label template in pipeline '%s' that refers to a material called '%s', but no material with this name is defined.", name(), materialName));
                return false;
            }

            return true;
        }

        addError("labelTemplate", String.format(LABEL_TEMPLATE_ERROR_MESSAGE, labelTemplate));
        return false;
    }

    private void validateLockBehaviorValues() {
        if (lockBehavior != null && !VALID_LOCK_VALUES.contains(lockBehavior)) {
            addError(LOCK_BEHAVIOR, MessageFormat.format("Lock behavior has an invalid value ({0}). Valid values are: {1}",
                    lockBehavior, VALID_LOCK_VALUES));
        }
    }

    private boolean hasStages() {
        return !isEmpty();
    }

    @Override
    public void addError(String fieldName, String msg) {
        errors.add(fieldName, msg);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

    public PipelineConfig getStages() {
        return this;
    }

    public StageConfig getStage(final CaseInsensitiveString stageName) {
        return findBy(stageName);
    }

    public StageConfig getStage(String stageName) {
        return getStage(new CaseInsensitiveString(stageName));
    }

    public StageConfig findBy(final CaseInsensitiveString stageName) {
        for (StageConfig stageConfig : this) {
            if (stageConfig.name().equals(stageName)) {
                return stageConfig;
            }
        }
        return null;
    }

    public StageConfig nextStage(final CaseInsensitiveString lastStageName) {
        for (int i = 0; i < this.size(); i++) {
            StageConfig stageConfig = this.get(i);
            boolean hasNextStage = i + 1 < this.size();
            if (hasNextStage && stageConfig.name().equals(lastStageName)) {
                return this.get(i + 1);
            }
        }
        return null;
    }

    public StageConfig getFirstStageConfig() {
        return this.first();
    }

    @Override
    public StageConfig set(int index, StageConfig stageConfig) {
        verifyUniqueName(stageConfig, index);
        return super.set(index, stageConfig);
    }

    @Override
    public boolean add(StageConfig stageConfig) {
        ensureNoTemplateDefined(stageConfig.name());
        verifyUniqueName(stageConfig);
        return addStageWithoutValidityAssertion(stageConfig);
    }

    public boolean addStageWithoutValidityAssertion(StageConfig stageConfig) {
        return super.add(stageConfig);
    }

    private void verifyUniqueName(StageConfig stageConfig) {
        if (alreadyContains(stageConfig)) {
            throw bomb("You have defined multiple stages called '" + stageConfig.name() + "'. Stage names are case-insensitive and must be unique.");
        }
    }

    private void verifyUniqueName(StageConfig stageConfig, int index) {
        if (stageConfig.name().equals(super.get(index).name())) {
            return;
        }
        verifyUniqueName(stageConfig);
    }

    private boolean alreadyContains(StageConfig stageConfig) {
        return findBy(stageConfig.name()) != null;
    }

    @Override
    public CaseInsensitiveString name() {
        return name;
    }

    public CaseInsensitiveString getName() {
        return name;
    }

    public StageConfig previousStage(final CaseInsensitiveString stageName) {
        StageConfig lastStageConfig = null;
        for (StageConfig currentStageConfig : this) {
            if (currentStageConfig.name().equals(stageName)) {
                return lastStageConfig;
            }
            lastStageConfig = currentStageConfig;
        }
        return null;
    }

    public boolean isConfigOriginSameAsOneOfMaterials() {
        if (!(isConfigDefinedRemotely()))
            return false;

        RepoConfigOrigin repoConfigOrigin = (RepoConfigOrigin) this.origin;
        MaterialConfig configMaterial = repoConfigOrigin.getMaterial();

        for (MaterialConfig material : this.materialConfigs()) {
            if (material.getFingerprint().equals(configMaterial.getFingerprint()))
                return true;
        }
        return false;
    }

    public boolean isConfigDefinedRemotely() {
        return this.origin instanceof RepoConfigOrigin;
    }

    public boolean hasSameConfigOrigin(PipelineConfig other) {
        if (!(isConfigDefinedRemotely()))
            return false;

        return this.origin.equals(other.getOrigin());
    }

    public boolean isConfigOriginFromRevision(String revision) {
        if (!(isConfigDefinedRemotely()))
            return false;

        RepoConfigOrigin repoConfigOrigin = (RepoConfigOrigin) this.origin;
        return repoConfigOrigin.isFromRevision(revision);
    }

    public MaterialConfigs materialConfigs() {
        return materialConfigs;
    }

    public void setMaterialConfigs(MaterialConfigs newMaterialConfigs) {
        this.materialConfigs = newMaterialConfigs;
        if (newMaterialConfigs == null || newMaterialConfigs.isEmpty()) {
            this.materialConfigs = new MaterialConfigs();
        }
    }

    public Node getDependenciesAsNode() {
        List<Node.DependencyNode> pipelineDeps = new ArrayList<>();
        for (MaterialConfig material : materialConfigs) {
            if (material instanceof DependencyMaterialConfig) {
                DependencyMaterialConfig dependencyMaterialConfig = (DependencyMaterialConfig) material;
                pipelineDeps.add(new Node.DependencyNode(dependencyMaterialConfig.getPipelineName(), dependencyMaterialConfig.getStageName()));
            }
        }
        return new Node(pipelineDeps);
    }

    public String getLabelTemplate() {
        return labelTemplate;
    }

    public void setLabelTemplate(String labelFormat) {
        this.labelTemplate = labelFormat;
    }

    public boolean hasNextStage(final CaseInsensitiveString lastStageName) {
        if (this.isEmpty()) {
            return false;
        }
        return nextStage(lastStageName) != null;
    }

    public TrackingTool getTrackingTool() {
        return trackingTool;
    }

    public TrackingTool trackingTool() {
        return trackingTool == null ? new TrackingTool() : trackingTool;
    }

    public void setTrackingTool(TrackingTool trackingTool) {
        this.trackingTool = trackingTool;
    }

    public void addMaterialConfig(MaterialConfig materialConfig) {
        this.materialConfigs.add(materialConfig);
    }

    public void removeMaterialConfig(MaterialConfig materialConfig) {
        this.materialConfigs.remove(materialConfig);
    }

    public PipelineConfig duplicate() {
        PipelineConfig clone = CLONER.deepClone(this);
        clone.name = new CaseInsensitiveString("");
        clearSelfPipelineNameInFetchTask(clone);
        return clone;
    }

    private void clearSelfPipelineNameInFetchTask(PipelineConfig clone) {
        for (StageConfig stage : clone) {
            for (JobConfig job : stage.getJobs()) {
                for (Task task : job.getTasks()) {
                    if (task instanceof FetchTask) {
                        FetchTask fetchTask = (FetchTask) task;
                        if (this.name().equals(fetchTask.getTargetPipelineName())) {
                            fetchTask.setPipelineName(new CaseInsensitiveString(""));
                        }
                    }
                }
            }
        }
    }

    public boolean isFirstStageManualApproval() {
        if (isEmpty()) {
            throw new IllegalStateException(format("Pipeline [%s] doesn't have any stage", name));
        }
        return getFirstStageConfig().getApproval().isManual();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PipelineConfig that = (PipelineConfig) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(labelTemplate, that.labelTemplate) &&
                Objects.equals(params, that.params) &&
                Objects.equals(trackingTool, that.trackingTool) &&
                Objects.equals(timer, that.timer) &&
                Objects.equals(variables, that.variables) &&
                Objects.equals(materialConfigs, that.materialConfigs) &&
                Objects.equals(lockBehavior, that.lockBehavior) &&
                Objects.equals(templateName, that.templateName);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (labelTemplate != null ? labelTemplate.hashCode() : 0);
        result = 31 * result + (trackingTool != null ? trackingTool.hashCode() : 0);
        result = 31 * result + (materialConfigs != null ? materialConfigs.hashCode() : 0);
        result = 31 * result + (timer != null ? timer.hashCode() : 0);
        result = 31 * result + (params != null ? params.hashCode() : 0);
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        result = 31 * result + (lockBehavior != null ? lockBehavior.hashCode() : 0);
        result = 31 * result + (templateName != null ? templateName.hashCode() : 0);
        return result;
    }

    public TimerConfig getTimer() {
        return timer;
    }

    public void setTimer(TimerConfig timer) {
        this.timer = timer;
    }

    public boolean requiresApproval() {
        if (isEmpty()) {
            return false;
        }
        return first().requiresApproval();
    }

    public void lockExplicitly() {
        this.lockBehavior = LOCK_VALUE_LOCK_ON_FAILURE;
    }

    public void unlockExplicitly() {
        lockBehavior = LOCK_VALUE_NONE;
    }

    public boolean hasExplicitLock() {
        return lockBehavior != null;
    }

    public Boolean explicitLock() {
        if (!hasExplicitLock()) {
            throw new RuntimeException(format("There is no explicit lock on the pipeline '%s'.", name));
        }

        return isLockable();
    }

    public boolean isLockable() {
        return isLockableOnFailure() || isPipelineUnlockableWhenFinished();
    }

    public boolean isLockableOnFailure() {
        return LOCK_VALUE_LOCK_ON_FAILURE.equals(lockBehavior);
    }

    public boolean isPipelineUnlockableWhenFinished() {
        return LOCK_VALUE_UNLOCK_WHEN_FINISHED.equals(lockBehavior);
    }

    public String getLockBehavior() {
        return lockBehavior == null ? LOCK_VALUE_NONE : lockBehavior;
    }

    public void setLockBehaviorIfNecessary(String newLockBehavior) {
        boolean oldBehaviorWasEmpty = !hasExplicitLock();
        boolean newBehaviorIsNone = LOCK_VALUE_NONE.equals(newLockBehavior);
        boolean doNotSet = oldBehaviorWasEmpty && newBehaviorIsNone;
        if (!doNotSet) {
            lockBehavior = newLockBehavior;
        }
    }

    public void setVariables(EnvironmentVariablesConfig variables) {
        this.variables = variables;
    }

    public EnvironmentVariablesConfig getVariables() {
        return variables;
    }

    public EnvironmentVariablesConfig getPlainTextVariables() {
        return variables.getPlainTextVariables();
    }

    public EnvironmentVariablesConfig getSecureVariables() {
        return variables.getSecureVariables();
    }

    public void addEnvironmentVariable(String name, String value) {
        variables.add(new EnvironmentVariableConfig(name.trim(), value));
    }

    public boolean hasTemplate() {
        return templateName != null && !StringUtils.isBlank(templateName.toString());
    }

    public CaseInsensitiveString getTemplateName() {
        return templateName;
    }

    public void usingTemplate(PipelineTemplateConfig pipelineTemplate) {
        this.addAll(CLONER.deepClone(pipelineTemplate));
        this.templateApplied = true;
    }

    private void ensureNoTemplateDefined(CaseInsensitiveString stageName) {
        if (hasTemplate()) {
            throw new IllegalStateException(format("Cannot add stage '%s' to pipeline '%s', which already references template '%s'.", stageName, name, templateName));
        }
    }

    public boolean hasTemplateApplied() {
        return templateApplied;
    }

    public void setTemplateName(CaseInsensitiveString templateName) {
        ensureNoStagesDefined(templateName);
        this.templateName = templateName;
    }

    public void setTemplateName(String templateName) {
        setTemplateName(new CaseInsensitiveString(templateName));
    }

    private void ensureNoStagesDefined(CaseInsensitiveString newTemplateName) {
        bombIf(!isEmpty(), format("Cannot set template '%s' on pipeline '%s' because it already has stages defined", newTemplateName, name));
    }

    public PipelineConfig getCopyForEditing() {
        PipelineConfig pipelineConfig = (PipelineConfig) clone();
        if (pipelineConfig.hasTemplate()) {
            pipelineConfig.clear();
        }
        return pipelineConfig;
    }

    public boolean dependsOn(final CaseInsensitiveString pipelineName) {
        for (MaterialConfig material : materialConfigs) {
            if (material instanceof DependencyMaterialConfig && ((DependencyMaterialConfig) material).getPipelineName().equals(pipelineName)) {
                return true;
            }
        }
        return false;
    }

    public List<DependencyMaterialConfig> dependencyMaterialConfigs() {
        List<DependencyMaterialConfig> materialConfigs = new ArrayList<>();
        for (MaterialConfig material : this.materialConfigs) {
            if (material instanceof DependencyMaterialConfig) {
                materialConfigs.add((DependencyMaterialConfig) material);
            }
        }
        return materialConfigs;
    }

    public boolean hasVariableInScope(String variableName) {
        if (variables.hasVariable(variableName)) {
            return true;
        }
        for (StageConfig stageConfig : this) {
            if (stageConfig.hasVariableInScope(variableName)) {
                return true;
            }
        }
        return false;
    }

    public List<CaseInsensitiveString> upstreamPipelines() {
        return materialConfigs.getDependentPipelineNames();
    }

    public boolean hasMaterial(MaterialConfig material) {
        return materialConfigs.hasMaterialWithFingerprint(material);
    }

    public void addParam(ParamConfig paramConfig) {
        this.params.add(paramConfig);
    }

    public void setParams(ParamsConfig paramsConfig) {
        this.params = paramsConfig;
    }

    public void setParams(List<ParamConfig> paramsConfig) {
        setParams(new ParamsConfig(paramsConfig));
    }

    @Override
    public ParamResolver applyOver(ParamResolver enclosingScope) {
        return enclosingScope.override(CLONER.deepClone(params));
    }

    public ParamsConfig getParams() {
        return params;
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        setConfigAttributes(attributes, null);
    }

    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        if (attributes == null) {
            return;
        }
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(NAME)) {
            setName((String) attributeMap.get(NAME));
        }
        if (attributeMap.containsKey(MATERIALS)) {
            materialConfigs.setConfigAttributes(attributeMap.get(MATERIALS));
        }
        if (attributeMap.containsKey(STAGE)) {
            clear();
            StageConfig stageConfig = new StageConfig();
            stageConfig.setConfigAttributes(attributeMap.get(STAGE), taskFactory);
            add(stageConfig);
        }
        if (attributeMap.containsKey(LABEL_TEMPLATE)) {
            labelTemplate = (String) attributeMap.get(LABEL_TEMPLATE);
            if (StringUtils.isBlank(labelTemplate)) {
                labelTemplate = PipelineLabel.COUNT_TEMPLATE;
            }
        }
        if (attributeMap.containsKey(TIMER_CONFIG)) {
            timer = TimerConfig.createTimer(attributeMap.get(TIMER_CONFIG));
        }
        if (attributeMap.containsKey(LOCK_BEHAVIOR)) {
            setLockBehaviorIfNecessary((String) attributeMap.get(LOCK_BEHAVIOR));
        }
        if (attributeMap.containsKey(TRACKING_TOOL)) {
            setIntegrationType(attributeMap);
        }

        if (attributeMap.containsKey(CONFIGURATION_TYPE)) {
            setConfigurationType(attributeMap);
        }

        if (attributeMap.containsKey(ENVIRONMENT_VARIABLES)) {
            variables.setConfigAttributes(attributeMap.get(ENVIRONMENT_VARIABLES));
        }
        if (!attributeMap.containsKey(CONFIGURATION_TYPE) || (attributeMap.containsKey(CONFIGURATION_TYPE) && getConfigurationType().equals(CONFIGURATION_TYPE_TEMPLATE))) {
            if (attributeMap.containsKey(PARAMS)) {
                params.setConfigAttributes(attributeMap.get(PARAMS));
            }
        }
        if (attributeMap.containsKey(StageConfig.APPROVAL)) {
            StageConfig firstStage = first();
            firstStage.setConfigAttributes(attributeMap);
        }
    }

    public void setName(String name) {
        this.name = new CaseInsensitiveString(name);
    }

    public void setName(CaseInsensitiveString name) {
        this.name = name;
    }

    private void setConfigurationType(Map attributeMap) {
        String configurationType = (String) attributeMap.get(CONFIGURATION_TYPE);
        if (configurationType.equals(CONFIGURATION_TYPE_STAGES)) {
            return;
        }
        if (configurationType.equals(CONFIGURATION_TYPE_TEMPLATE)) {
            String templateName = (String) attributeMap.get(TEMPLATE_NAME);
            this.clear();
            this.setTemplateName(new CaseInsensitiveString(templateName));
        }
    }

    private void setIntegrationType(Map attributeMap) {
        trackingTool = TrackingTool.createTrackingTool((Map) attributeMap.get(TRACKING_TOOL));
    }

    public String getConfigurationType() {
        if (hasTemplate()) {
            return CONFIGURATION_TYPE_TEMPLATE;
        }
        return CONFIGURATION_TYPE_STAGES;
    }

    public void incrementIndex(StageConfig stageToBeMoved) {
        moveStage(stageToBeMoved, 1);
    }

    public void decrementIndex(StageConfig stageToBeMoved) {
        moveStage(stageToBeMoved, -1);
    }

    private void moveStage(StageConfig moveMeStage, int moveBy) {
        int current = this.indexOf(moveMeStage);
        if (current == -1) {
            throw new RuntimeException(format("Cannot find the stage '%s' in pipeline '%s'", moveMeStage.name(), name()));
        }
        this.remove(moveMeStage);
        this.add(current + moveBy, moveMeStage);
    }

    public List<StageConfig> allStagesBefore(CaseInsensitiveString stage) {
        List<StageConfig> stages = new ArrayList<>();
        for (StageConfig stageConfig : this) {
            if (stage.equals(stageConfig.name())) {
                break;
            }
            stages.add(stageConfig);
        }
        return stages;
    }

    public List<StageConfig> validStagesForFetchArtifact(PipelineConfig downstreamPipeline, CaseInsensitiveString currentDownstreamStage) {
        for (DependencyMaterialConfig dependencyMaterial : downstreamPipeline.dependencyMaterialConfigs()) {
            if (dependencyMaterial.getPipelineName().equals(name)) {
                List<StageConfig> stageConfigs = allStagesBefore(dependencyMaterial.getStageName());
                stageConfigs.add(getStage(dependencyMaterial.getStageName())); // add this stage itself
                return stageConfigs;
            }
        }
        if (this.equals(downstreamPipeline)) {
            return allStagesBefore(currentDownstreamStage);
        }
        return null;
    }

    public List<PipelineConfig> allFirstLevelUpstreamPipelines(CruiseConfig cruiseConfig) {
        List<PipelineConfig> pipelinesForFetchArtifact = new ArrayList<>();
        for (DependencyMaterialConfig dependencyMaterial : dependencyMaterialConfigs()) {
            pipelinesForFetchArtifact.add(cruiseConfig.pipelineConfigByName(dependencyMaterial.getPipelineName()));
        }
        return pipelinesForFetchArtifact;
    }

    public CommentRenderer getCommentRenderer() {
        return trackingTool();
    }

    public void validateNameUniqueness(Map<CaseInsensitiveString, PipelineConfig> pipelineNameMap) {
        PipelineConfig pipelineWithSameName = pipelineNameMap.get(name);
        if (pipelineWithSameName == null) {
            pipelineNameMap.put(name, this);
        } else {
            pipelineWithSameName.nameConflictError();
            this.nameConflictError();
        }
    }

    private void nameConflictError() {
        errors.add(NAME, format("You have defined multiple pipelines called '%s'. Pipeline names are case-insensitive and must be unique.", name));
    }

    public List<FetchTask> getFetchTasks() {
        List<FetchTask> fetchTasks = new ArrayList<>();
        for (StageConfig stage : this) {
            for (JobConfig job : stage.getJobs()) {
                for (Task task : job.tasks()) {
                    if (task instanceof FetchTask) {
                        fetchTasks.add((FetchTask) task);
                    }
                }
            }
        }
        return fetchTasks;

    }

    public void addEnvironmentVariable(EnvironmentVariableConfig environmentVariableConfig) {
        variables.add(environmentVariableConfig);
    }

    public void cleanupAllUsagesOfRole(Role roleToDelete) {
        for (StageConfig stageConfig : this) {
            stageConfig.cleanupAllUsagesOfRole(roleToDelete);
        }
    }

    public void templatize(CaseInsensitiveString templateName) {
        clear();
        this.templateName = templateName;
        this.templateApplied = false;
    }

    public boolean hasMaterialWithFingerprint(MaterialConfig materialConfig) {
        return this.materialConfigs.hasMaterialWithFingerprint(materialConfig);
    }

    public List<PackageMaterialConfig> packageMaterialConfigs() {
        return materialConfigs().stream()
                .filter(materialConfig -> materialConfig instanceof PackageMaterialConfig)
                .map(x -> (PackageMaterialConfig) x)
                .collect(Collectors.toList());
    }

    public List<PluggableSCMMaterialConfig> pluggableSCMMaterialConfigs() {
        return materialConfigs().stream()
                .filter(materialConfig -> materialConfig instanceof PluggableSCMMaterialConfig)
                .map(x -> (PluggableSCMMaterialConfig) x)
                .collect(Collectors.toList());
    }

    @Override
    public ConfigOrigin getOrigin() {
        return origin;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        this.origin = origins;
    }

    public void setOrigin(ConfigOrigin origin) {
        this.origin = origin;
    }

    public boolean isLocal() {
        return origin == null || this.origin.isLocal();
    }

    public void setLock(boolean lock) {
        if (lock)
            this.lockExplicitly();
        else
            this.unlockExplicitly();
    }

    public String getOriginDisplayName() {
        return getOrigin() != null ? getOrigin().displayName() : new FileConfigOrigin().displayName();
    }

    public void setDisplayOrderWeight(int displayOrderWeight) {
        this.displayOrderWeight = displayOrderWeight;
    }

    public int getDisplayOrderWeight() {
        return displayOrderWeight;
    }
}
