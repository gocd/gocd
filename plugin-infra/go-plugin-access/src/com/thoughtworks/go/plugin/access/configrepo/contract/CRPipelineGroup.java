package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Collection;
import java.util.List;

public class CRPipelineGroup {
    private final String name;
    private final List<CRPipeline> pipelines;

    public CRPipelineGroup(String name, List<CRPipeline> pipelines) {
        this.name = name;
        this.pipelines = pipelines;
    }

    public String getName() {
        return name;
    }

    public List<CRPipeline> getPipelines() {
        return pipelines;
    }
}
