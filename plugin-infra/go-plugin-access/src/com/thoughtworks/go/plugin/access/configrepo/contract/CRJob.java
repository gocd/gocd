package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRTask;

import java.util.Collection;
import java.util.List;

public class CRJob {
    private final String name;
    private final Collection<CREnvironmentVariable> environmentVariables ;
    private final Collection<CRTab> tabs ;
    private final Collection<String> resources;
    private final Collection<CRArtifact> artifacts;
    private final Collection<CRPropertyGenerator> artifactPropertiesGenerators;

    private final boolean runOnAllAgents;
    private final int runInstanceCount;
    private final int timeout;

    private final List<CRTask> tasks ;

    public CRJob(String name, Collection<CREnvironmentVariable> environmentVariables, Collection<CRTab> tabs,
                 Collection<String> resources, Collection<CRArtifact> artifacts,
                 Collection<CRPropertyGenerator> artifactPropertiesGenerators,
                 boolean runOnAllAgents, int runInstanceCount, int timeout, List<CRTask> tasks) {
        this.name = name;
        this.environmentVariables = environmentVariables;
        this.tabs = tabs;
        this.resources = resources;
        this.artifacts = artifacts;
        this.artifactPropertiesGenerators = artifactPropertiesGenerators;
        this.runOnAllAgents = runOnAllAgents;
        this.runInstanceCount = runInstanceCount;
        this.timeout = timeout;
        this.tasks = tasks;
    }

    public String getName() {
        return name;
    }

    public Collection<CREnvironmentVariable> getEnvironmentVariables() {
        return environmentVariables;
    }

    public Collection<CRTab> getTabs() {
        return tabs;
    }

    public Collection<String> getResources() {
        return resources;
    }

    public Collection<CRArtifact> getArtifacts() {
        return artifacts;
    }

    public Collection<CRPropertyGenerator> getArtifactPropertiesGenerators() {
        return artifactPropertiesGenerators;
    }

    public boolean isRunOnAllAgents() {
        return runOnAllAgents;
    }

    public int getRunInstanceCount() {
        return runInstanceCount;
    }

    public int getTimeout() {
        return timeout;
    }

    public List<CRTask> getTasks() {
        return tasks;
    }

    public CREnvironmentVariable getEnvironmentVariable(String name) {
        for(CREnvironmentVariable var : environmentVariables)
        {
            if(var.getName().equals(name))
                return var;
        }
        return  null;
    }

    public CRTab getTab(String tabName) {
        for(CRTab var : tabs)
        {
            if(var.getName().equals(tabName))
                return var;
        }
        return  null;
    }

    public CRPropertyGenerator getPropertyGenerator(String name) {
        for(CRPropertyGenerator var : artifactPropertiesGenerators)
        {
            if(var.getName().equals(name))
                return var;
        }
        return  null;
    }
}
