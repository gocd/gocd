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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.material.SourceCodeMaterial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRPipeline extends CRBase {
    @SerializedName("group")
    @Expose
    private String group;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("display_order_weight")
    @Expose
    private int displayOrderWeight = -1;
    @SerializedName("label_template")
    @Expose
    private String labelTemplate;
    @SerializedName("lock_behavior")
    @Expose
    private String lockBehavior;
    @SerializedName("tracking_tool")
    @Expose
    private CRTrackingTool trackingTool;
    @SerializedName("timer")
    @Expose
    private CRTimer timer;
    @SerializedName("environment_variables")
    @Expose
    private Collection<CREnvironmentVariable> environmentVariables = new ArrayList<>();
    @SerializedName("parameters")
    @Expose
    private Collection<CRParameter> parameters = new ArrayList<>();
    @SerializedName("materials")
    @Expose
    private Collection<CRMaterial> materials = new ArrayList<>();
    @SerializedName("stages")
    @Expose
    private List<CRStage> stages = new ArrayList<>();
    @SerializedName("template")
    @Expose
    private String template;

    public CRPipeline() {
    }

    public CRPipeline(String name, String groupName) {
        this.name = name;
        this.group = groupName;
    }

    public boolean hasEnvironmentVariable(String key) {
        for (CREnvironmentVariable var : environmentVariables) {
            if (var.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public CRMaterial getMaterialByName(String name) {
        if (this.materials == null)
            return null;
        for (CRMaterial m : this.materials) {
            if (m.getName().equals(name))
                return m;
        }
        return null;
    }

    public void addMaterial(CRMaterial material) {
        this.materials.add(material);
    }

    public void addStage(CRStage stage) {
        this.stages.add(stage);
    }

    public void addParameter(CRParameter param) {
        this.parameters.add(param);
    }

    public void addEnvironmentVariable(String key, String value) {
        CREnvironmentVariable variable = new CREnvironmentVariable(key);
        variable.setValue(value);
        this.environmentVariables.add(variable);
    }

    public void addEnvironmentVariable(CREnvironmentVariable variable) {
        this.environmentVariables.add(variable);
    }

    @Override
    public String getLocation(String parent) {
        return StringUtils.isBlank(location) ?
                StringUtils.isBlank(name) ? String.format("Pipeline in %s", parent) :
                        String.format("Pipeline %s", name) : String.format("%s; Pipeline %s", location, name);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location, "name", name);
        errors.checkMissing(location, "group", group);
        errors.checkMissing(location, "materials", materials);
        validateAtLeastOneMaterial(errors, location);
        if (materials != null) {
            for (CRMaterial material : this.materials) {
                material.getErrors(errors, location);
            }
            if (materials.size() > 1) {
                validateMaterialNameUniqueness(errors, location);
                validateScmMaterials(errors, location);
            }
        }
        validateTemplateOrStages(errors, location);
        if (!hasTemplate()) {
            validateAtLeastOneStage(errors, location);
            if (stages != null) {
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
        if (lockBehavior != null && !PipelineConfig.VALID_LOCK_VALUES.contains(lockBehavior)) {
            errors.addError(location, MessageFormat.format("Lock behavior has an invalid value ({0}). Valid values are: {1}",
                    lockBehavior, PipelineConfig.VALID_LOCK_VALUES));
        }
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for (CREnvironmentVariable var : environmentVariables) {
            String error = var.validateNameUniqueness(keys);
            if (error != null)
                errors.addError(location, error);
        }
    }

    private void validateParamNameUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for (CRParameter param : parameters) {
            String error = param.validateNameUniqueness(keys);
            if (error != null)
                errors.addError(location, error);
        }
    }

    private void validateScmMaterials(ErrorCollection errors, String pipelineLocation) {
        List<SourceCodeMaterial> allSCMMaterials = filterScmMaterials();
        if (allSCMMaterials.size() > 1) {
            for (SourceCodeMaterial material : allSCMMaterials) {
                String directory = material.getDestination();
                if (StringUtils.isBlank(directory)) {
                    String location = material.getLocation(pipelineLocation);
                    errors.addError(location, "Material must have destination directory when there are many SCM materials");
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
        for (CRStage stage : stages) {
            String error = stage.validateNameUniqueness(keys);
            if (error != null)
                errors.addError(location, error);
        }
    }

    private void validateMaterialNameUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for (CRMaterial material1 : materials) {
            String error = material1.validateNameUniqueness(keys);
            if (error != null)
                errors.addError(location, error);
        }
    }

    private void validateAtLeastOneStage(ErrorCollection errors, String location) {
        if (!hasStages())
            errors.addError(location, "Pipeline has no stages.");
    }

    private boolean hasStages() {
        return !(this.stages == null || this.stages.isEmpty());
    }

    private void validateTemplateOrStages(ErrorCollection errors, String location) {
        if (!hasTemplate() && !hasStages()) {
            errors.addError(location, "Pipeline has to define stages or template.");
        } else if (hasTemplate() && hasStages()) {
            errors.addError(location, "Pipeline has to either define stages or template. Not both.");
        }
    }

    private void validateAtLeastOneMaterial(ErrorCollection errors, String location) {
        if (this.materials == null || this.materials.isEmpty())
            errors.addError(location, "Pipeline has no materials.");
    }

    public boolean hasTemplate() {
        return template != null && !StringUtils.isBlank(template);
    }

}
