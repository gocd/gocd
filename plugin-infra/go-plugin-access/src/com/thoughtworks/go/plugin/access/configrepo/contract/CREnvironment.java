package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class CREnvironment extends CRBase {
    private String name;
    private Collection<CREnvironmentVariable> environment_variables;
    private Collection<String> agents;
    private Collection<String> pipelines;

    public CREnvironment(String name)
    {
        this();
        this.name = name;
    }
    public CREnvironment()
    {
        environment_variables = new ArrayList<>();
        agents = new ArrayList<>();
        pipelines = new ArrayList<>();
    }
    public CREnvironment(String name, Collection<CREnvironmentVariable> environmentVariables, Collection<String> agents, Collection<String> pipelines) {
        this.name = name;
        this.environment_variables = environmentVariables;
        this.agents = agents;
        this.pipelines = pipelines;
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

    @Override
    public void getErrors(ErrorCollection errors,String parentLocation) {
        String location = this.getLocation(parentLocation);
        validateEnvironmentVariableUniqueness(errors,location);
        validateAgentUniqueness(errors,location);
        validatePipelineUniqueness(errors,location);
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
    private void validateAgentUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(String agent : agents)
        {
            String lowerCase = agent.toLowerCase();
            if(keys.contains(lowerCase))
                errors.addError(location,String.format(
                        "Agent %s is defined more than once",agent));
            else
                keys.add(lowerCase);
        }
    }
    private void validatePipelineUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(String pipeline : pipelines)
        {
            String lowerCase = pipeline.toLowerCase();
            if(keys.contains(lowerCase))
                errors.addError(location,String.format(
                        "Pipeline %s is defined more than once",pipeline));
            else
                keys.add(lowerCase);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CREnvironment that = (CREnvironment)o;
        if(that == null)
            return  false;

        if (name != null ? !name.equals(that.getName()) : that.getName() != null) {
            return false;
        }

        if (agents != null ? !CollectionUtils.isEqualCollection(this.getAgents(), that.getAgents()) : that.getAgents() != null) {
            return false;
        }

        if (pipelines != null ? !CollectionUtils.isEqualCollection(this.getPipelines(), that.getPipelines()) : that.getPipelines() != null) {
            return false;
        }
        if (environment_variables != null ?
                !CollectionUtils.isEqualCollection(this.getEnvironmentVariables(),that.getEnvironmentVariables()) :
                that.getEnvironmentVariables() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (agents != null ? agents.size() : 0);
        result = 31 * result + (pipelines != null ? pipelines.size() : 0);
        result = 31 * result + (environment_variables != null ? environment_variables.size() : 0);
        return result;
    }

    public void addAgent(String agentUuid) {
        this.agents.add(agentUuid);
    }

    public void addPipeline(String pipeline1) {
        this.pipelines.add(pipeline1);
    }

    public String validateNameUniqueness(HashSet<String> keys) {
        if(keys.contains(this.getName()))
            return String.format("Environment %s is defined more than once",this.getName());
        else
            keys.add(this.getName());
        return null;
    }

    @Override
    public String getLocation(String parent) {
        return StringUtil.isBlank(location) ?
                StringUtil.isBlank(name) ? String.format("Environment in %s",parent) :
                        String.format("Environment %s",name) : String.format("%s; Environment %s",location,name);
    }
}
