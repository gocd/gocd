package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class CREnvironment {
    private String name;
    private Collection<CREnvironmentVariable> environmentVariables;
    private Collection<String> agents;
    private Collection<String> pipelines;

    public CREnvironment() {
        environmentVariables = new ArrayList<>();
        agents = new HashSet<>();
        pipelines = new HashSet<>();
    }
    public CREnvironment(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<CREnvironmentVariable>  getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Collection<CREnvironmentVariable> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public Collection<String> getAgents() {
        return agents;
    }

    public void setAgents(Collection<String> agents) {
        this.agents = agents;
    }

    public Collection<String> getPipelines() {
        return pipelines;
    }

    public void setPipelines(Collection<String> pipelines) {
        this.pipelines = pipelines;
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
