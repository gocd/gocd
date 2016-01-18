package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.plugin.configrepo.material.CRMaterial_1;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

public class CRPipeline_1 extends CRBase {
    private String group;
    private String name;
    private String labelTemplate;
    private boolean isLocked;
    private CRTrackingTool_1 trackingTool;
    private CRMingle_1 mingle;
    private CRTimer_1 timer;
    private Collection<CREnvironmentVariable_1> environmentVariables = new ArrayList<>();
    private Collection<CRMaterial_1> materials = new ArrayList<>();
    private List<CRStage_1> stages = new ArrayList<>();

    public CRPipeline_1(){}
    public CRPipeline_1(String name,String groupName,CRMaterial_1 material,CRStage_1... stages)
    {
        this.name = name;
        this.group = groupName;
        this.materials.add(material);
        this.stages = Arrays.asList(stages);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabelTemplate() {
        return labelTemplate;
    }

    public void setLabelTemplate(String labelTemplate) {
        this.labelTemplate = labelTemplate;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setIsLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    public CRTrackingTool_1 getTrackingTool() {
        return trackingTool;
    }

    public void setTrackingTool(CRTrackingTool_1 trackingTool) {
        this.trackingTool = trackingTool;
    }

    public CRMingle_1 getMingle() {
        return mingle;
    }

    public void setMingle(CRMingle_1 mingle) {
        this.mingle = mingle;
    }

    public CRTimer_1 getTimer() {
        return timer;
    }

    public void setTimer(CRTimer_1 timer) {
        this.timer = timer;
    }

    public Collection<CREnvironmentVariable_1> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Collection<CREnvironmentVariable_1> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public Collection<CRMaterial_1> getMaterials() {
        return materials;
    }
    public CRMaterial_1 getMaterialByName(String name)
    {
        if(this.materials == null)
            return  null;
        for(CRMaterial_1 m : this.materials)
        {
            if(m.getName().equals(name))
                return m;
        }
        return null;
    }

    public void setMaterials(Collection<CRMaterial_1> materials) {
        this.materials = materials;
    }

    public List<CRStage_1> getStages() {
        return stages;
    }

    public void setStages(List<CRStage_1> stages) {
        this.stages = stages;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateName(errors);
        validateMaterials(errors);
        validateAtLeastOneStage(errors);
    }

    private void validateMaterials(ErrorCollection errors) {
        if(this.materials == null || this.materials.isEmpty()) {
            errors.add(this, "Pipeline has no materials");
            return;
        }
        if(materials.size() > 1)
        {
            validateMaterialNameUniqueness(errors);
        }
    }

    private void validateMaterialNameUniqueness(ErrorCollection errors) {
        HashSet<String> keys = new HashSet<>();
        for(CRMaterial_1 material1 : materials)
        {
            String error = material1.validateNameUniqueness(keys);
            if(error != null)
                errors.add(this,error);
        }
    }

    private void validateAtLeastOneStage(ErrorCollection errors) {
        if(this.stages == null || this.stages.isEmpty())
            errors.add(this,"Pipeline has no stages");
    }

    private void validateName(ErrorCollection errors) {
        if (StringUtil.isBlank(name)) {
            errors.add(this, "Pipeline has no name");
        }
    }

    public void addMaterial(CRMaterial_1 material) {
        this.materials.add(material);
    }

    public void addStage(CRStage_1 stage) {
        this.stages.add(stage);
    }

    public void addEnvironmentVariable(String key,String value){
        CREnvironmentVariable_1 variable = new CREnvironmentVariable_1(key);
        variable.setValue(value);
        this.environmentVariables.add(variable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRPipeline_1 that = (CRPipeline_1) o;

        if (labelTemplate != null ? !labelTemplate.equals(that.labelTemplate) : that.labelTemplate != null) {
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
        if (trackingTool != null ? !trackingTool.equals(that.trackingTool) : that.trackingTool != null) {
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
        if (environmentVariables != null ? !CollectionUtils.isEqualCollection(environmentVariables,that.environmentVariables) : that.environmentVariables != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (labelTemplate != null ? labelTemplate.hashCode() : 0);
        result = 31 * result + (mingle != null ? mingle.hashCode() : 0);
        result = 31 * result + (trackingTool != null ? trackingTool.hashCode() : 0);
        result = 31 * result + (materials != null ? materials.size() : 0);
        result = 31 * result + (stages != null ? stages.size() : 0);
        result = 31 * result + (environmentVariables != null ? environmentVariables.size() : 0);
        result = 31 * result + (timer != null ? timer.hashCode() : 0);
        return result;
    }

    public String getGroupName() {
        return group;
    }

    public void setGroupName(String groupName) {
        this.group = groupName;
    }
}
