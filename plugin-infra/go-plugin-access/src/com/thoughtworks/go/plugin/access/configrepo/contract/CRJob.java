package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRTask;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

public class CRJob extends CRBase {
    private String name;
    private Collection<CREnvironmentVariable> environment_variables = new ArrayList<>();
    private Collection<CRTab> tabs = new ArrayList<>();
    private Collection<String> resources = new ArrayList<>();
    private Collection<CRArtifact> artifacts = new ArrayList<>();
    private Collection<CRPropertyGenerator> properties = new ArrayList<>();

    private String run_instance_count;
    private Integer timeout;

    private List<CRTask> tasks = new ArrayList<>();

    public CRJob()
    {
    }
    public CRJob(String name, CRTask... tasks)
    {
        this.name = name;
        this.tasks = Arrays.asList(tasks);
    }
    public CRJob(String name, Collection<CREnvironmentVariable> environmentVariables, Collection<CRTab> tabs,
                 Collection<String> resources, Collection<CRArtifact> artifacts,
                 Collection<CRPropertyGenerator> artifactPropertiesGenerators,
                 String runInstanceCount, int timeout, List<CRTask> tasks) {
        this.name = name;
        this.environment_variables = environmentVariables;
        this.tabs = tabs;
        this.resources = resources;
        this.artifacts = artifacts;
        this.properties = artifactPropertiesGenerators;
        this.run_instance_count = runInstanceCount;
        this.timeout = timeout;
        this.tasks = tasks;
    }
    public CRJob(String name, Collection<CREnvironmentVariable> environmentVariables, Collection<CRTab> tabs,
                 Collection<String> resources, Collection<CRArtifact> artifacts,
                 Collection<CRPropertyGenerator> artifactPropertiesGenerators,
                 boolean runOnAllAgents, int runInstanceCount, int timeout, List<CRTask> tasks) {
        this.name = name;
        this.environment_variables = environmentVariables;
        this.tabs = tabs;
        this.resources = resources;
        this.artifacts = artifacts;
        this.properties = artifactPropertiesGenerators;
        this.run_instance_count = Integer.toString(runInstanceCount);
        this.timeout = timeout;
        this.tasks = tasks;
        if(runOnAllAgents)
            this.setRunOnAllAgents(runOnAllAgents);
    }

    @Override
    public void getErrors(ErrorCollection errors,String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location,"name",name);
        validateEnvironmentVariableUniqueness(errors,location);
        validateTabs(errors,location);
        validateArtifacts(errors,location);
        validateProperties(errors,location);
        validateTasks(errors,location);
    }

    private void validateTasks(ErrorCollection errors, String location) {
        errors.checkMissing(location,"tasks",tasks);
        if(tasks != null)
            for(CRTask task : tasks)
            {
                task.getErrors(errors,location);
            }
    }

    private void validateProperties(ErrorCollection errors, String location) {
        if(properties != null)
            for(CRPropertyGenerator gen : properties)
            {
                gen.getErrors(errors,location);
            }
    }

    private void validateArtifacts(ErrorCollection errors, String location) {
        if(artifacts == null)
            return;
        for(CRArtifact artifact : artifacts)
        {
            artifact.getErrors(errors,location);
        }
    }

    private void validateTabs(ErrorCollection errors, String location) {
        if(tabs == null)
            return;
        for(CRTab tab : tabs)
        {
            tab.getErrors(errors,location);
        }
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(CREnvironmentVariable var : environment_variables)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRJob that = (CRJob)o;
        if(that == null)
            return  false;

        if (name != null ? !name.equals(that.getName()) : that.getName() != null) {
            return false;
        }

        if (environment_variables != null ? !CollectionUtils.isEqualCollection(this.environment_variables, that.environment_variables) : that.environment_variables != null) {
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
        if (properties != null ? !CollectionUtils.isEqualCollection(this.properties, that.properties) : that.properties != null) {
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
        if (run_instance_count != null ? !run_instance_count.equals(that.run_instance_count) : that.run_instance_count != null) {
            return false;
        }
        if(this.timeout != that.timeout)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (tabs != null ? tabs.size() : 0);
        result = 31 * result + (resources != null ? resources.size() : 0);
        result = 31 * result + (environment_variables != null ? environment_variables.size() : 0);
        result = 31 * result + (resources != null ? resources.size() : 0);
        result = 31 * result + (artifacts != null ? artifacts.size() : 0);
        result = 31 * result + (properties != null ? properties.size() : 0);
        return result;
    }

    public void addTask(CRTask task)
    {
        tasks.add(task);
    }

    public void addEnvironmentVariable(String key,String value){
        CREnvironmentVariable variable = new CREnvironmentVariable(key);
        variable.setValue(value);
        this.environment_variables.add(variable);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<CREnvironmentVariable> getEnvironmentVariables() {
        return environment_variables;
    }

    public void setEnvironmentVariables(Collection<CREnvironmentVariable> environmentVariables) {
        this.environment_variables = environmentVariables;
    }

    public Collection<CRTab> getTabs() {
        return tabs;
    }

    public void setTabs(Collection<CRTab> tabs) {
        this.tabs = tabs;
    }

    public Collection<String> getResources() {
        return resources;
    }

    public void setResources(Collection<String> resources) {
        this.resources = resources;
    }

    public Collection<CRArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Collection<CRArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    public Collection<CRPropertyGenerator> getArtifactPropertiesGenerators() {
        return properties;
    }

    public void setArtifactPropertiesGenerators(Collection<CRPropertyGenerator> artifactPropertiesGenerators) {
        this.properties = artifactPropertiesGenerators;
    }

    public boolean isRunOnAllAgents() {
        return run_instance_count != null && run_instance_count.equalsIgnoreCase("all");
    }

    public void setRunOnAllAgents(boolean runOnAllAgents) {
        if(runOnAllAgents)
            this.run_instance_count = "all";
        else
            this.run_instance_count = null;
    }

    public Integer getRunInstanceCount() {
        if(run_instance_count == null)
            return null;
        return Integer.parseInt(run_instance_count);
    }

    public void setRunInstanceCount(int runInstanceCount) {
        this.run_instance_count = Integer.toString(runInstanceCount);
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public List<CRTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<CRTask> tasks) {
        this.tasks = tasks;
    }

    public void addResource(String resource) {
        this.resources.add(resource);
    }

    public void addTab(CRTab tab) {
        this.tabs.add(tab);
    }

    public void addProperty(CRPropertyGenerator property) {
        this.properties.add(property);
    }

    public String validateNameUniqueness(HashSet<String> names) {
        if(names.contains(this.getName()))
            return String.format("Job %s is defined more than once",this.getName());
        else
            names.add(this.getName());
        return null;
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String stage = getName() == null ? "unknown name" : getName();
        return String.format("%s; Job (%s)",myLocation,stage);
    }
}
