package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRMaterial;

import java.util.Collection;
import java.util.List;

/**
 * Created by tomzo on 7/2/15.
 */
public class CRPipeline {
    private String name;
    private String labelTemplate;
    private boolean isLocked;
    private CRTrackingTool trackingTool;
    private CRMingle mingle;
    private CRTimer timer;
    private Collection<CREnvironmentVariable> environmentVariables;
    private Collection<CRMaterial> materials;
    private List<CRStage> stages;
}
