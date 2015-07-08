package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.plugin.configrepo.tasks.CRTask_1;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

public class CRJob_1 extends CRBase {
    private String name;
    private Collection<CREnvironmentVariable_1> environmentVariables = new ArrayList<>();
    private Collection<CRTab_1> tabs = new ArrayList<>();
    private Collection<String> resources = new ArrayList<>();
    private Collection<CRArtifact_1> artifacts = new ArrayList<>();
    private Collection<CRPropertyGenerator_1> artifactPropertiesGenerators = new ArrayList<>();

    private boolean runOnAllAgents;
    private int runInstanceCount;
    private int timeout;

    private List<CRTask_1> tasks = new ArrayList<>();

    public CRJob_1()
    {
    }
    public CRJob_1(String name, CRTask_1... tasks)
    {
        this.name = name;
        this.tasks = Arrays.asList(tasks);
    }
    @Override
    public void getErrors(ErrorCollection errors) {
        validateName(errors);
        validateEnvironmentVariableUniqueness(errors);
        validateTabs(errors);
        validateArtifacts(errors);
        validateProperties(errors);
        validateTasks(errors);
    }

    private void validateTasks(ErrorCollection errors) {
        if(tasks == null)
            return;
        for(CRTask_1 task : tasks)
        {
            task.getErrors(errors);
        }
    }

    private void validateProperties(ErrorCollection errors) {
        if(artifactPropertiesGenerators == null)
            return;
        for(CRPropertyGenerator_1 gen : artifactPropertiesGenerators)
        {
            gen.getErrors(errors);
        }
    }

    private void validateArtifacts(ErrorCollection errors) {
        if(artifacts == null)
            return;
        for(CRArtifact_1 artifact : artifacts)
        {
            artifact.getErrors(errors);
        }
    }

    private void validateTabs(ErrorCollection errors) {
        if(tabs == null)
            return;
        for(CRTab_1 tab : tabs)
        {
            tab.getErrors(errors);
        }
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors) {
        if(environmentVariables == null)
            return;
        HashSet<String> keys = new HashSet<>();
        for(CREnvironmentVariable_1 var : environmentVariables)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.add(this,error);
        }
    }

    private void validateName(ErrorCollection errors) {
        if (StringUtil.isBlank(name)) {
            errors.add(this, "Job name not set");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRJob_1 that = (CRJob_1)o;
        if(that == null)
            return  false;

        if (name != null ? !name.equals(that.getName()) : that.getName() != null) {
            return false;
        }

        if (environmentVariables != null ? !CollectionUtils.isEqualCollection(this.environmentVariables, that.environmentVariables) : that.environmentVariables != null) {
            return false;
        }
        if (tabs != null ? !CollectionUtils.isEqualCollection(this.tabs, that.tabs) : that.tabs != null) {
            return false;
        }
        if (resources != null ? !CollectionUtils.isEqualCollection(this.resources, that.resources) : that.resources != null) {
            return false;
        }
        if (artifacts != null ? !CollectionUtils.isEqualCollection(this.artifacts, that.artifacts) : that.artifacts != null) {
            return false;
        }
        if (artifactPropertiesGenerators != null ? !CollectionUtils.isEqualCollection(this.artifactPropertiesGenerators, that.artifactPropertiesGenerators) : that.artifactPropertiesGenerators != null) {
            return false;
        }
        if (tasks != null ? this.tasks.size() != that.tasks.size() : that.tasks != null) {
            return false;
        }
        for(int i = 0 ; i< this.tasks.size(); i++)
        {
            if(!tasks.get(i).equals(that.tasks.get(i)))
                return false;
        }
        if(this.runOnAllAgents != that.runOnAllAgents)
            return false;
        if(this.runInstanceCount != that.runInstanceCount)
            return false;
        if(this.timeout != that.timeout)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (tabs != null ? tabs.size() : 0);
        result = 31 * result + (resources != null ? resources.size() : 0);
        result = 31 * result + (environmentVariables != null ? environmentVariables.size() : 0);
        result = 31 * result + (resources != null ? resources.size() : 0);
        result = 31 * result + (artifacts != null ? artifacts.size() : 0);
        result = 31 * result + (artifactPropertiesGenerators != null ? artifactPropertiesGenerators.size() : 0);
        return result;
    }

    public void addTask(CRTask_1 task)
    {
        tasks.add(task);
    }

    public void addEnvironmentVariable(String key,String value){
        CREnvironmentVariable_1 variable = new CREnvironmentVariable_1(key);
        variable.setValue(value);
        this.environmentVariables.add(variable);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<CREnvironmentVariable_1> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Collection<CREnvironmentVariable_1> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public Collection<CRTab_1> getTabs() {
        return tabs;
    }

    public void setTabs(Collection<CRTab_1> tabs) {
        this.tabs = tabs;
    }

    public Collection<String> getResources() {
        return resources;
    }

    public void setResources(Collection<String> resources) {
        this.resources = resources;
    }

    public Collection<CRArtifact_1> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Collection<CRArtifact_1> artifacts) {
        this.artifacts = artifacts;
    }

    public Collection<CRPropertyGenerator_1> getArtifactPropertiesGenerators() {
        return artifactPropertiesGenerators;
    }

    public void setArtifactPropertiesGenerators(Collection<CRPropertyGenerator_1> artifactPropertiesGenerators) {
        this.artifactPropertiesGenerators = artifactPropertiesGenerators;
    }

    public boolean isRunOnAllAgents() {
        return runOnAllAgents;
    }

    public void setRunOnAllAgents(boolean runOnAllAgents) {
        this.runOnAllAgents = runOnAllAgents;
    }

    public int getRunInstanceCount() {
        return runInstanceCount;
    }

    public void setRunInstanceCount(int runInstanceCount) {
        this.runInstanceCount = runInstanceCount;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public List<CRTask_1> getTasks() {
        return tasks;
    }

    public void setTasks(List<CRTask_1> tasks) {
        this.tasks = tasks;
    }



    public void addResource(String resource) {
        this.resources.add(resource);
    }

    public void addTab(CRTab_1 tab) {
        this.tabs.add(tab);
    }

    public void addProperty(CRPropertyGenerator_1 property) {
        this.artifactPropertiesGenerators.add(property);
    }

    public String validateNameUniqueness(HashSet<String> names) {
        if(names.contains(this.getName()))
            return String.format("Job %s is defined more than once",this.getName());
        else
            names.add(this.getName());
        return null;
    }
}
