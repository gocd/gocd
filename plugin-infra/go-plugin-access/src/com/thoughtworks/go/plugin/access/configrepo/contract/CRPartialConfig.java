package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.ArrayList;
import java.util.Collection;

public class CRPartialConfig {
    private Collection<CREnvironment> environments;
    private Collection<CRPipelineGroup> groups;

    public CRPartialConfig()
    {
        environments = new ArrayList<CREnvironment>();
        groups = new ArrayList<CRPipelineGroup>();
    }

    public Collection<CREnvironment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Collection<CREnvironment> environments) {
        this.environments = environments;
    }

    public void addEnvironment(CREnvironment environment) {
        this.environments.add(environment);
    }
}
