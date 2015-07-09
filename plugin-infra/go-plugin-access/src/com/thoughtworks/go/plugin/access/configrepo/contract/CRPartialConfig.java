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

    public CRPipelineGroup getGroup(String name) {
        for(CRPipelineGroup g : groups)
        {
            if(g.getName().equals(name))
                return g;
        }
        return null;
    }

    public void addGroup(CRPipelineGroup group) {
        this.groups.add(group);
    }
}
