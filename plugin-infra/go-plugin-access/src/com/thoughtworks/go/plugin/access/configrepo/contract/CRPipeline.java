package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRMaterial;

import java.util.Collection;
import java.util.List;

public class CRPipeline {
    private final String name;
    private final String labelTemplate;
    private final boolean isLocked;
    private final CRTrackingTool trackingTool;
    private final CRMingle mingle;
    private final CRTimer timer;
    private final Collection<CREnvironmentVariable> environmentVariables;
    private final Collection<CRMaterial> materials;
    private final List<CRStage> stages;

    public CRPipeline(String name, String labelTemplate, boolean isLocked, CRTrackingTool trackingTool,
                      CRMingle mingle, CRTimer timer, Collection<CREnvironmentVariable> environmentVariables,
                      Collection<CRMaterial> materials, List<CRStage> stages) {
        this.name = name;
        this.labelTemplate = labelTemplate;
        this.isLocked = isLocked;
        this.trackingTool = trackingTool;
        this.mingle = mingle;
        this.timer = timer;
        this.environmentVariables = environmentVariables;
        this.materials = materials;
        this.stages = stages;
    }

    public String getName() {
        return name;
    }

    public String getLabelTemplate() {
        return labelTemplate;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public CRTrackingTool getTrackingTool() {
        return trackingTool;
    }

    public CRMingle getMingle() {
        return mingle;
    }

    public CRTimer getTimer() {
        return timer;
    }

    public Collection<CREnvironmentVariable> getEnvironmentVariables() {
        return environmentVariables;
    }

    public Collection<CRMaterial> getMaterials() {
        return materials;
    }

    public List<CRStage> getStages() {
        return stages;
    }
}
