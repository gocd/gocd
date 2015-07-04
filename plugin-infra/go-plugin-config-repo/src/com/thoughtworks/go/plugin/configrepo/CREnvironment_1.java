package com.thoughtworks.go.plugin.configrepo;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CREnvironment_1 extends CRBase {
    private String name;
    private Collection<CREnvironmentVariable_1> environmentVariables;
    private Collection<String> agents;
    private Collection<String> pipelines;

    public CREnvironment_1(String name)
    {
        this();
        this.name = name;
    }
    public CREnvironment_1()
    {
        environmentVariables = new ArrayList<>();
        agents = new ArrayList<>();
        pipelines = new ArrayList<>();
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
    public void getErrors(ErrorCollection errors) {
        validateEnvironmentVariableUniqueness(errors);
        validateAgentUniqueness(errors);
        validatePipelineUniqueness(errors);
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors) {
        HashSet<String> keys = new HashSet<>();
        for(CREnvironmentVariable_1 var : environmentVariables)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.add(this,error);
        }
    }
    private void validateAgentUniqueness(ErrorCollection errors) {
        HashSet<String> keys = new HashSet<>();
        for(String agent : agents)
        {
            String lowerCase = agent.toLowerCase();
            if(keys.contains(lowerCase))
                errors.add(this,String.format(
                        "Agent %s is defined more than once",agent));
            else
                keys.add(lowerCase);
        }
    }
    private void validatePipelineUniqueness(ErrorCollection errors) {
        HashSet<String> keys = new HashSet<>();
        for(String pipeline : pipelines)
        {
            String lowerCase = pipeline.toLowerCase();
            if(keys.contains(lowerCase))
                errors.add(this,String.format(
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

        CREnvironment_1 that = (CREnvironment_1)o;
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
        if (environmentVariables != null ?
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
        result = 31 * result + (environmentVariables != null ? environmentVariables.size() : 0);
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
}
