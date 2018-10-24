/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.SourceCodeMaterial;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.*;

public class CRPipeline extends CRBase {
    private String group;
    private String name;
    private String label_template;
    private String lock_behavior;
    private CRTrackingTool tracking_tool;
    private CRMingle mingle;
    private CRTimer timer;
    private Collection<CREnvironmentVariable> environment_variables = new ArrayList<>();
    private Collection<CRParameter> parameters = new ArrayList<>();
    private Collection<CRMaterial> materials = new ArrayList<>();
    private List<CRStage> stages = new ArrayList<>();
    private String template;

    public CRPipeline(){}
    public CRPipeline(String name, String groupName, CRMaterial material, String template, CRStage... stages)
    {
        this.name = name;
        this.group = groupName;
        this.template = template;
        this.materials.add(material);
        this.stages = Arrays.asList(stages);
    }

    public CRPipeline(String name, String groupName, String labelTemplate, String lockBehavior, CRTrackingTool trackingTool,
                      CRMingle mingle, CRTimer timer, Collection<CREnvironmentVariable> environmentVariables,
                      Collection<CRMaterial> materials, List<CRStage> stages, String template, Collection<CRParameter> parameters) {
        this.name = name;
        this.group = groupName;
        this.label_template = labelTemplate;
        this.lock_behavior = lockBehavior;
        this.tracking_tool = trackingTool;
        this.mingle = mingle;
        this.timer = timer;
        this.environment_variables = environmentVariables;
        this.materials = materials;
        this.stages = stages;
        this.template = template;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabelTemplate() {
        return label_template;
    }

    public void setLabelTemplate(String labelTemplate) {
        this.label_template = labelTemplate;
    }

    public String lockBehavior() {
        return lock_behavior;
    }

    public CRTrackingTool getTrackingTool() {
        return tracking_tool;
    }

    public void setTrackingTool(CRTrackingTool trackingTool) {
        this.tracking_tool = trackingTool;
    }

    public CRMingle getMingle() {
        return mingle;
    }

    public void setMingle(CRMingle mingle) {
        this.mingle = mingle;
    }

    public CRTimer getTimer() {
        return timer;
    }

    public void setTimer(CRTimer timer) {
        this.timer = timer;
    }

    public Collection<CREnvironmentVariable> getEnvironmentVariables() {
        return environment_variables;
    }

    public void setEnvironmentVariables(Collection<CREnvironmentVariable> environmentVariables) {
        this.environment_variables = environmentVariables;
    }

    public boolean hasEnvironmentVariable(String key) {
        for (CREnvironmentVariable var: environment_variables) {
            if (var.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public Collection<CRParameter> getParameters() {
        return parameters;
    }

    public void setParameters(Collection<CRParameter> parameters) {
        this.parameters = parameters;
    }

    public Collection<CRMaterial> getMaterials() {
        return materials;
    }
    public CRMaterial getMaterialByName(String name)
    {
        if(this.materials == null)
            return  null;
        for(CRMaterial m : this.materials)
        {
            if(m.getName().equals(name))
                return m;
        }
        return null;
    }

    public void setMaterials(Collection<CRMaterial> materials) {
        this.materials = materials;
    }

    public List<CRStage> getStages() {
        return stages;
    }

    public void setStages(List<CRStage> stages) {
        this.stages = stages;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void addMaterial(CRMaterial material) {
        this.materials.add(material);
    }

    public void addStage(CRStage stage) {
        this.stages.add(stage);
    }

    public void addParameter (CRParameter param) {
        this.parameters.add(param);
    }

    public void addEnvironmentVariable(String key,String value) {
        CREnvironmentVariable variable = new CREnvironmentVariable(key);
        variable.setValue(value);
        this.environment_variables.add(variable);
    }

    public void addEnvironmentVariable(CREnvironmentVariable variable) {
        this.environment_variables.add(variable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRPipeline that = (CRPipeline) o;

        if (label_template != null ? !label_template.equals(that.label_template) : that.label_template != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (group != null ? !group.equals(that.group) : that.group != null) {
            return false;
        }
        if (timer != null ? !timer.equals(that.timer) : that.timer != null) {
            return false;
        }
        if (tracking_tool != null ? !tracking_tool.equals(that.tracking_tool) : that.tracking_tool != null) {
            return false;
        }
        if (mingle != null ? !mingle.equals(that.mingle) : that.mingle != null) {
            return false;
        }
        if (materials != null ? !CollectionUtils.isEqualCollection(materials, that.materials) : that.materials != null) {
            return false;
        }
        if (stages != null ? !CollectionUtils.isEqualCollection(stages, that.stages) : that.stages != null) {
            return false;
        }
        if (environment_variables != null ? !CollectionUtils.isEqualCollection(environment_variables,that.environment_variables) : that.environment_variables != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (label_template != null ? label_template.hashCode() : 0);
        result = 31 * result + (mingle != null ? mingle.hashCode() : 0);
        result = 31 * result + (tracking_tool != null ? tracking_tool.hashCode() : 0);
        result = 31 * result + (materials != null ? materials.size() : 0);
        result = 31 * result + (stages != null ? stages.size() : 0);
        result = 31 * result + (environment_variables != null ? environment_variables.size() : 0);
        result = 31 * result + (timer != null ? timer.hashCode() : 0);
        return result;
    }

    public String getGroupName() {
        return group;
    }

    public void setGroupName(String groupName) {
        this.group = groupName;
    }

    @Override
    public String getLocation(String parent) {
        return StringUtils.isBlank(location) ?
                StringUtils.isBlank(name) ? String.format("Pipeline in %s",parent) :
                  String.format("Pipeline %s",name) : String.format("%s; Pipeline %s",location,name);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location,"name",name);
        errors.checkMissing(location,"group",group);
        errors.checkMissing(location,"materials",materials);
        validateAtLeastOneMaterial(errors,location);
        if(materials != null) {
            for (CRMaterial material : this.materials) {
                material.getErrors(errors, location);
            }
            if (materials.size() > 1) {
                validateMaterialNameUniqueness(errors, location);
                validateScmMaterials(errors,location);
            }
        }
        validateTemplateOrStages(errors, location);
        if (!hasTemplate()) {
            validateAtLeastOneStage(errors,location);
            if(stages != null){
                for (CRStage stage : this.stages) {
                    stage.getErrors(errors, location);
                }
                if (stages.size() > 1) {
                    validateStageNameUniqueness(errors, location);
                }
            }
        }
        validateEnvironmentVariableUniqueness(errors, location);
        validateParamNameUniqueness(errors, location);
        validateLockBehaviorValue(errors, location);
    }

    private void validateLockBehaviorValue(ErrorCollection errors, String location) {
        if (lock_behavior != null && !PipelineConfig.VALID_LOCK_VALUES.contains(lock_behavior)) {
            errors.addError(location, MessageFormat.format("Lock behavior has an invalid value ({0}). Valid values are: {1}",
                    lock_behavior, PipelineConfig.VALID_LOCK_VALUES));
        }
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(CREnvironmentVariable var : environment_variables)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }
    private void validateParamNameUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(CRParameter param : parameters)
        {
            String error = param.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }

    private void validateScmMaterials(ErrorCollection errors, String pipelineLocation) {
        List<SourceCodeMaterial> allSCMMaterials = filterScmMaterials();
        if (allSCMMaterials.size() > 1) {
            for (SourceCodeMaterial material : allSCMMaterials) {
                String directory = material.getDestination();
                if (StringUtils.isBlank(directory)) {
                    String location = material.getLocation(pipelineLocation);
                    errors.addError(location,"Material must have destination directory when there are many SCM materials");
                }
            }
        }
    }

    private List<SourceCodeMaterial> filterScmMaterials() {
        List<SourceCodeMaterial> scmMaterials = new ArrayList<>();
        for (CRMaterial material : this.materials) {
            if (material instanceof SourceCodeMaterial) {
                scmMaterials.add((SourceCodeMaterial) material);
            }
        }
        return scmMaterials;
    }

    private void validateStageNameUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(CRStage stage : stages)
        {
            String error = stage.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }

    private void validateMaterialNameUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(CRMaterial material1 : materials)
        {
            String error = material1.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }

    private void validateAtLeastOneStage(ErrorCollection errors, String location) {
        if(!hasStages())
            errors.addError(location,"Pipeline has no stages");
    }

    private boolean hasStages() {
        return !(this.stages == null || this.stages.isEmpty());
    }

    private void validateTemplateOrStages(ErrorCollection errors, String location) {
        if (!hasTemplate() && !hasStages()) {
            errors.addError(location, "Pipeline has to define stages or template.");
        }
        else if (hasTemplate() && hasStages()) {
            errors.addError(location, "Pipeline has to either define stages or template. Not both.");
        }
    }

    private void validateAtLeastOneMaterial(ErrorCollection errors, String location) {
        if(this.materials == null || this.materials.isEmpty())
            errors.addError(location,"Pipeline has no materials");
    }

    public boolean hasTemplate() {
        return template != null && !StringUtils.isBlank(template);
    }

    public String getLock_behavior() {
        return lock_behavior;
    }

    public void setLock_behavior(String lock_behavior) {
        this.lock_behavior = lock_behavior;
    }
}
