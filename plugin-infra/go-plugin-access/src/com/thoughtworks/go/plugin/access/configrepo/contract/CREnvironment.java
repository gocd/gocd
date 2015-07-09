package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class CREnvironment {
    private final String name;
    private final Collection<CREnvironmentVariable> environmentVariables;
    private final Collection<String> agents;
    private final Collection<String> pipelines;

    public CREnvironment(String name, Collection<CREnvironmentVariable> environmentVariables, Collection<String> agents, Collection<String> pipelines) {
        this.name = name;
        this.environmentVariables = environmentVariables;
        this.agents = agents;
        this.pipelines = pipelines;
    }


    public String getName() {
        return name;
    }

    public Collection<CREnvironmentVariable>  getEnvironmentVariables() {
        return environmentVariables;
    }

    public Collection<String> getAgents() {
        return agents;
    }

    public Collection<String> getPipelines() {
        return pipelines;
    }

    public void addVariable(String name, String value, String encryptedValue) {
        environmentVariables.add(new CREnvironmentVariable(name,value,encryptedValue));
    }

    public void addAgent(String agent) {
        this.agents.add(agent);
    }

    public void addPipeline(String pipeline) {
        this.pipelines.add(pipeline);
    }
}
