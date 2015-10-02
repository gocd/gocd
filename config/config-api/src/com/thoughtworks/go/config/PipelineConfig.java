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
import com.thoughtworks.go.util.Pair;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.XmlUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static com.thoughtworks.go.util.Pair.pair;
import static org.apache.commons.collections.CollectionUtils.select;

/**
 * @understands how a cruise pipeline is configured by the user
 */
@ConfigTag("pipeline")
@ConfigCollection(value = StageConfig.class, asFieldName = "Stages")
public class PipelineConfig extends BaseCollection<StageConfig> implements ParamScope, ParamsAttributeAware,
        Validatable, EnvironmentVariableScope, ConfigOriginTraceable {
    private static final Cloner CLONER = new Cloner();

    private static final String ERR_TEMPLATE = "You have defined a label template in pipeline %s that refers to a material called %s, but no material with this name is defined.";
    public static final String LABEL_TEMPLATE = "labelTemplate";
    public static final String MINGLE_CONFIG = "mingleConfig";
    public static final String TRACKING_TOOL = "trackingTool";
    public static final String TIMER_CONFIG = "timer";
    public static final String ENVIRONMENT_VARIABLES = "variables";
    public static final String PARAMS = "params";
    private static final String LABEL_TEMPLATE_ZERO_TRUNC_BLOCK = "(\\[:0+\\])";
    private static final String LABEL_TEMPLATE_TRUNC_BLOCK = "(\\[:\\d+\\])?";
    private static final String LABEL_TEMPLATE_CHARACTERS = "[a-zA-Z0-9_\\-.!~*'()#:]"; // why a '#'?
    private static final String LABEL_TEMPLATE_VARIABLE_REGEX = "[$]\\{(" + LABEL_TEMPLATE_CHARACTERS + "+)" + LABEL_TEMPLATE_TRUNC_BLOCK + "\\}";
    public static final String LABEL_TEMPLATE_FORMAT = "((" + LABEL_TEMPLATE_CHARACTERS + ")*[$]"
            + "\\{" + LABEL_TEMPLATE_CHARACTERS + "+" + LABEL_TEMPLATE_TRUNC_BLOCK + "\\}(" + LABEL_TEMPLATE_CHARACTERS + ")*)+";
    private static final Pattern LABEL_TEMPLATE_FORMAT_REGEX = Pattern.compile(String.format("^(%s)$", LABEL_TEMPLATE_FORMAT));
    public static final Pattern LABEL_TEMPATE_ZERO_TRUNC_BLOCK_PATTERN = Pattern.compile(LABEL_TEMPLATE_ZERO_TRUNC_BLOCK);
    public static final String TEMPLATE_NAME = "templateName";
    public static final String LOCK = "lock";
    public static final String CONFIGURATION_TYPE = "configurationType";
    public static final String CONFIGURATION_TYPE_STAGES = "configurationType_stages";
    public static final String CONFIGURATION_TYPE_TEMPLATE = "configurationType_template";
    public static final String LABEL_TEMPLATE_ERROR_MESSAGE =
            "Invalid label. Label should be composed of alphanumeric text, it should contain the builder number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as #{<param-name>}.";

    @SkipParameterResolution
    @ConfigAttribute(value = "name", optional = false)
    private CaseInsensitiveString name;

    @ConfigAttribute(value = "labeltemplate", optional = true)
    private String labelTemplate = PipelineLabel.COUNT_TEMPLATE;

    @ConfigSubtag @SkipParameterResolution
    private ParamsConfig params = new ParamsConfig();

    @ConfigSubtag
    private TrackingTool trackingTool;

    @ConfigSubtag
    private MingleConfig mingleConfig = new MingleConfig();

    @ConfigSubtag(optional = true)
    private TimerConfig timer;

    @ConfigSubtag
    private EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();

    @ConfigSubtag(optional = false)
    private MaterialConfigs materialConfigs = new MaterialConfigs();

    @ConfigAttribute(value = "isLocked", optional = true, allowNull = true)
    private String lock;

    @SkipParameterResolution
    @ConfigAttribute(value = "template", optional = true, allowNull = true)
    private CaseInsensitiveString templateName;

    private ConfigOrigin origin;

    private boolean templateApplied;

    private ConfigErrors errors = new ConfigErrors();
    public static final String NAME = "name";
    public static final String INTEGRATION_TYPE = "integrationType";
    public static final String INTEGRATION_TYPE_NONE = "none";
    public static final String INTEGRATION_TYPE_MINGLE = "mingle";
    public static final String INTEGRATION_TYPE_TRACKING_TOOL = "trackingTool";
    public static final String MATERIALS = "materials";
    public static final String STAGE = "stage";

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



    public void validate(ValidationContext validationContext) {
        validateLabelTemplate();
        validatePipelineName();
        validateStageNameUniqueness();
        validateTemplateName();
    }

    private void validatePipelineName() {
        if (!new NameTypeValidator().isNameValid(name)) {
            errors().add(NAME, NameTypeValidator.errorMessage("pipeline", name));
        }
    }

    private void validateTemplateName() {
        if (templateName != null && !new NameTypeValidator().isNameValid(templateName)) {
            errors().add(TEMPLATE_NAME, NameTypeValidator.errorMessage("template", templateName));
        }
    }

    private void validateStageNameUniqueness() {
        Map<String, StageConfig> stageNameMap = new HashMap<String, StageConfig>();
        for (StageConfig stageConfig : this) {
            stageConfig.validateNameUniqueness(stageNameMap);
        }
    }

    private void validateLabelTemplate() {
        if (XmlUtils.doesNotMatchUsingXsdRegex(LABEL_TEMPLATE_FORMAT_REGEX, labelTemplate)) {
            addError("labelTemplate", LABEL_TEMPLATE_ERROR_MESSAGE);
            return;
        }

        if(validateLabelTemplateTruncation(labelTemplate)){
            addError("labelTemplate", String.format("Length of zero not allowed on label %s defined on pipeline %s.",labelTemplate,name));
            return;
        }

        Set<String> templateVariables = getTemplateVariables();
        List<String> materialNames = allowedTemplateVariables();
        for (final String templateVariable : templateVariables) {
            if (!CollectionUtils.exists(materialNames, withNameSameAs(templateVariable))) {
                addError("labelTemplate", String.format(ERR_TEMPLATE, name(), templateVariable));
            }
        }
    }

    private boolean validateLabelTemplateTruncation(String labelTemplate) {
        return LABEL_TEMPATE_ZERO_TRUNC_BLOCK_PATTERN.matcher(labelTemplate).find();
    }

    private Predicate withNameSameAs(final String templateVariable) {
        return new Predicate() {
            public boolean evaluate(Object materialName) {
                return StringUtils.equalsIgnoreCase(materialName.toString(), templateVariable);
            }
        };
    }

    public void addError(String fieldName, String msg) {
        errors.add(fieldName, msg);
    }

    public ConfigErrors errors() {
        return errors;
    }

	public List<StageConfig> getStages() {
		return this;
	}

    public StageConfig getStage(final CaseInsensitiveString stageName) {
        return findBy(stageName);
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

    public StageConfig set(int index, StageConfig stageConfig) {
        verifyUniqueName(stageConfig, index);
        return super.set(index, stageConfig);
    }

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

    public CaseInsensitiveString name() {
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

    public boolean isConfigOriginSameAsOneOfMaterials()
    {
        if(!(this.origin instanceof RepoConfigOrigin))
            return false;

        RepoConfigOrigin repoConfigOrigin = (RepoConfigOrigin)this.origin;
        MaterialConfig configMaterial = repoConfigOrigin.getMaterial();

        for(MaterialConfig material : this.materialConfigs())
        {
            if(material.equals(configMaterial))
                return true;
        }
        return false;
    }
    public boolean isConfigOriginFromRevision(String revision)
    {
        if(!(this.origin instanceof RepoConfigOrigin))
            return false;

        RepoConfigOrigin repoConfigOrigin = (RepoConfigOrigin)this.origin;
        return repoConfigOrigin.isFromRevision(revision);
    }

    private static <T> T as(Class<T> clazz, Object o){
        if(clazz.isInstance(o)){
            return clazz.cast(o);
        }
        return null;
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
        List<CaseInsensitiveString> pipelineDeps = new ArrayList<CaseInsensitiveString>();
        for (MaterialConfig material : materialConfigs) {
            if (material instanceof DependencyMaterialConfig) {
                pipelineDeps.add(((DependencyMaterialConfig) material).getPipelineName());
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
            throw new IllegalStateException(String.format("Pipeline [%s] doesn't have any stage", name));
        }
        return getFirstStageConfig().getApproval().isManual();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PipelineConfig that = (PipelineConfig) o;

        if (labelTemplate != null ? !labelTemplate.equals(that.labelTemplate) : that.labelTemplate != null) {
            return false;
        }
        if (materialConfigs != null ? !materialConfigs.equals(that.materialConfigs) : that.materialConfigs != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (timer != null ? !timer.equals(that.timer) : that.timer != null) {
            return false;
        }
        if (trackingTool != null ? !trackingTool.equals(that.trackingTool) : that.trackingTool != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (labelTemplate != null ? labelTemplate.hashCode() : 0);
        result = 31 * result + (trackingTool != null ? trackingTool.hashCode() : 0);
        result = 31 * result + (materialConfigs != null ? materialConfigs.hashCode() : 0);
        result = 31 * result + (timer != null ? timer.hashCode() : 0);
        return result;
    }

    public Set<String> getTemplateVariables() {
        Pattern pattern = Pattern.compile(LABEL_TEMPLATE_VARIABLE_REGEX);
        Matcher matcher = pattern.matcher(this.labelTemplate);
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    public List<String> allowedTemplateVariables() {
        List<String> names = new ArrayList<String>();
        for (MaterialConfig material : materialConfigs) {
            if (!CaseInsensitiveString.isBlank(material.getName())) {
                names.add(CaseInsensitiveString.str(material.getName()));
            }
        }
        names.add("COUNT");
        return names;
    }

    public TimerConfig getTimer() {
        return timer;
    }

    public boolean requiresApproval() {
        if (isEmpty()) {
            return false;
        }
        return first().requiresApproval();
    }

    public void lockExplicitly() {
        this.lock = Boolean.TRUE.toString();
    }

    public void unlockExplicitly() {
        lock = Boolean.FALSE.toString();
    }

    public boolean hasExplicitLock() {
        return lock != null;
    }

    public void removeExplicitLocks() {
        this.lock = null;
    }

    public Boolean explicitLock() {
        if (!hasExplicitLock()) {
            throw new RuntimeException(String.format("There is no explicit lock on the pipeline '%s'.", name));
        }

        return isLock();
    }

    public boolean isLock() {
        return Boolean.parseBoolean(lock);
    }

    // only called from tests

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
        return templateName != null;
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
            throw new IllegalStateException(String.format("Cannot add stage '%s' to pipeline '%s', which already references template '%s'.", stageName, name, templateName));
        }
    }

    public boolean hasTemplateApplied() {
        return templateApplied;
    }

    public void setTemplateName(CaseInsensitiveString templateName) {
        ensureNoStagesDefined(templateName);
        this.templateName = templateName;
    }

    public void setMingleConfig(MingleConfig mingleConfig) {
        this.mingleConfig = mingleConfig;
    }

    public MingleConfig getMingleConfig() {
        return mingleConfig;
    }

    private void ensureNoStagesDefined(CaseInsensitiveString newTemplateName) {
        bombIf(!isEmpty(), String.format("Cannot set template '%s' on pipeline '%s' because it already has stages defined", newTemplateName, name));
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
        List<DependencyMaterialConfig> materialConfigs = new ArrayList<DependencyMaterialConfig>();
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

    public ParamResolver applyOver(ParamResolver enclosingScope) {
        return enclosingScope.override(CLONER.deepClone(params));
    }

    public ParamsConfig getParams() {
        return params;
    }


    public void setConfigAttributes(Object attributes) {
        setConfigAttributes(attributes, null);
    }

    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        if (attributes == null) {
            return;
        }
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(NAME)) {
            name = new CaseInsensitiveString((String) attributeMap.get(NAME));
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
            if (StringUtil.isBlank(labelTemplate)) {
                labelTemplate = PipelineLabel.COUNT_TEMPLATE;
            }
        }
        if (attributeMap.containsKey(TIMER_CONFIG)) {
            timer = TimerConfig.createTimer(attributeMap.get(TIMER_CONFIG));
        }
        if (attributeMap.containsKey(LOCK)) {
            lock = "1".equals(attributeMap.get(LOCK)) ? "true" : "false";
        }
        if (attributeMap.containsKey(INTEGRATION_TYPE)) {
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
        String integrationType = (String) attributeMap.get(INTEGRATION_TYPE);
        if (integrationType.equals(INTEGRATION_TYPE_NONE)) {
            mingleConfig = new MingleConfig();
            trackingTool = null;
        } else if (integrationType.equals(INTEGRATION_TYPE_MINGLE)) {
            mingleConfig = MingleConfig.create(attributeMap.get(MINGLE_CONFIG));
            trackingTool = null;
        } else if (integrationType.equals(INTEGRATION_TYPE_TRACKING_TOOL)) {
            mingleConfig = new MingleConfig();
            trackingTool = TrackingTool.createTrackingTool((Map) attributeMap.get(TRACKING_TOOL));
        }
    }

    public String getIntegrationType() {
        boolean isMingleConfigEmpty = (mingleConfig.equals(new MingleConfig()) && mingleConfig.errors().isEmpty());
        boolean isTrackingToolEmpty = (trackingTool == null || (trackingTool.equals(new TrackingTool()) && trackingTool.errors().isEmpty()));

        if (isMingleConfigEmpty && isTrackingToolEmpty) {
            return INTEGRATION_TYPE_NONE;
        }
        if (!isTrackingToolEmpty && isMingleConfigEmpty) {
            return INTEGRATION_TYPE_TRACKING_TOOL;
        }
        if (!isMingleConfigEmpty && isTrackingToolEmpty) {
            return INTEGRATION_TYPE_MINGLE;
        }
        throw new RuntimeException("Cannot have both tracking tool and mingle config specified");
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
            throw new RuntimeException(String.format("Cannot find the stage '%s' in pipeline '%s'", moveMeStage.name(), name()));
        }
        this.remove(moveMeStage);
        this.add(current + moveBy, moveMeStage);
    }

    public List<StageConfig> allStagesBefore(CaseInsensitiveString stage) {
        List<StageConfig> stages = new ArrayList<StageConfig>();
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
        List<PipelineConfig> pipelinesForFetchArtifact = new ArrayList<PipelineConfig>();
        for (DependencyMaterialConfig dependencyMaterial : dependencyMaterialConfigs()) {
            pipelinesForFetchArtifact.add(cruiseConfig.pipelineConfigByName(dependencyMaterial.getPipelineName()));
        }
        return pipelinesForFetchArtifact;
    }

    public CommentRenderer getCommentRenderer() {
        if (getIntegrationType().equals(INTEGRATION_TYPE_MINGLE)) {
            return mingleConfig;
        } else {
            return trackingTool();
        }
    }

    public void validateNameUniqueness(Map<String, PipelineConfig> pipelineNameMap) {
        String currentName = name.toLower();
        PipelineConfig pipelineWithSameName = pipelineNameMap.get(currentName);
        if (pipelineWithSameName == null) {
            pipelineNameMap.put(currentName, this);
        } else {
            pipelineWithSameName.nameConflictError();
            this.nameConflictError();
        }
    }

    private void nameConflictError() {
        errors.add(NAME, String.format("You have defined multiple pipelines called '%s'. Pipeline names are case-insensitive and must be unique.", name));
    }

    public List<FetchTask> getFetchTasks() {
        List<FetchTask> fetchTasks = new ArrayList<FetchTask>();
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

    public Set<Pair<PipelineConfig, StageConfig>> stagesWithPermissionForRole(CaseInsensitiveString roleName) {
        Set<Pair<PipelineConfig, StageConfig>> result = new HashSet<Pair<PipelineConfig, StageConfig>>();
        for (StageConfig stageConfig : this) {
            if (stageConfig.canBeOperatedBy(new Role(roleName))) {
                result.add(pair(this, stageConfig));
            }
        }
        return result;
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
        return new ArrayList<PackageMaterialConfig>(select(materialConfigs(), new Predicate() {
            @Override
            public boolean evaluate(Object materialConfig) {
                return materialConfig instanceof PackageMaterialConfig;
            }
        }));
    }

    public List<PluggableSCMMaterialConfig> pluggableSCMMaterialConfigs() {
        return new ArrayList<PluggableSCMMaterialConfig>(select(materialConfigs(), new Predicate() {
            @Override
            public boolean evaluate(Object materialConfig) {
                return materialConfig instanceof PluggableSCMMaterialConfig;
            }
        }));
    }

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
}