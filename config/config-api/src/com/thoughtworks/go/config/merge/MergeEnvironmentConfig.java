package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Composite of many EnvironmentConfig instances. Hides elementary environment configurations.
 */
public class MergeEnvironmentConfig extends BaseCollection<EnvironmentConfig>  implements EnvironmentConfig {

    private final ConfigErrors configErrors = new ConfigErrors();

    public static final String CONSISTENT_KV = "ConsistentEnvVariables";

    public MergeEnvironmentConfig(EnvironmentConfig... configs)
    {
        CaseInsensitiveString name = configs[0].name();
        for(EnvironmentConfig part : configs) {
            if(!part.name().equals(name))
                throw new IllegalArgumentException(
                        "partial environment configs must all have the same name");
            this.add(part);
        }
    }
    public MergeEnvironmentConfig(List<EnvironmentConfig> configs)
    {
        CaseInsensitiveString name = configs.get(0).name();
        for(EnvironmentConfig part : configs) {
            if(!part.name().equals(name))
                throw new IllegalArgumentException(
                        "partial environment configs must all have the same name");
            this.add(part);
        }
    }

    @Override
    public void validate(ValidationContext validationContext) {
        EnvironmentVariablesConfig allVariables = new EnvironmentVariablesConfig();
        for(EnvironmentConfig part : this)
        {
            for(EnvironmentVariableConfig partVariable : part.getVariables())
            {
                if(!allVariables.hasVariable(partVariable.getName()))
                {
                    allVariables.add(partVariable);
                }
                else
                {
                    //then it must be equal
                    if(!allVariables.contains(partVariable))
                        configErrors.add(CONSISTENT_KV, String.format(
                                "Environment variable '%s' is defined more than once with different values",
                                partVariable.getName()));
                }
            }
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public EnvironmentPipelineMatcher createMatcher() {
        return new EnvironmentPipelineMatcher(this.name(), this.getAgents().getUuids(), this.getPipelines());
    }

    @Override
    public boolean hasAgent(String uuid) {
        for(EnvironmentConfig part : this)
        {
            if(part.hasAgent(uuid))
                return true;
        }
        return false;
    }

    @Override
    public void validateContainsOnlyUuids(Set<String> uuids) {
        for (EnvironmentAgentConfig agent : this.getAgents()) {
            agent.validateUuidPresent(this.name(), uuids);
        }
    }

    @Override
    public void validateContainsOnlyPipelines(List<CaseInsensitiveString> pipelineNames) {
        this.getPipelines().validateContainsOnlyPipelines(this.name(), pipelineNames);
    }

    @Override
    public boolean containsPipeline(CaseInsensitiveString pipelineName) {
        for(EnvironmentConfig part : this)
        {
            if(part.containsPipeline(pipelineName))
                return true;
        }
        return false;
    }


    @Override
    public void setConfigAttributes(Object attributes) {

    }
    @Override
    public void addEnvironmentVariable(String name, String value) {

    }

    @Override
    public void addAgent(String uuid) {

    }

    @Override
    public void addAgentIfNew(String uuid) {

    }

    @Override
    public void addPipeline(CaseInsensitiveString pipelineName) {

    }

    @Override
    public void removeAgent(String uuid) {

    }


    @Override
    public boolean hasName(CaseInsensitiveString environmentName) {
        return this.name().equals(environmentName);
    }

    @Override
    public boolean hasVariable(String variableName) {
        for(EnvironmentConfig part : this)
        {
            if(part.hasVariable(variableName))
                return true;
        }
        return  false;
    }

    @Override
    public boolean hasSamePipelinesAs(EnvironmentConfig other) {
        return false;
    }

    @Override
    public boolean contains(String pipelineName) {
        for(EnvironmentConfig part : this)
        {
            if(part.contains(pipelineName))
                return true;
        }
        return  false;
    }

    @Override
    public CaseInsensitiveString name() {
        return this.first().name();
    }

    @Override
    public EnvironmentAgentsConfig getAgents() {
        EnvironmentAgentsConfig allAgents = new EnvironmentAgentsConfig();
        for(EnvironmentConfig part : this)
        {
            for(EnvironmentAgentConfig partAgent : part.getAgents())
            {
                if(!allAgents.contains(partAgent))
                    allAgents.add(partAgent);
            }
        }
        return allAgents;
    }


    @Override
    public EnvironmentVariableContext createEnvironmentContext() {
        EnvironmentVariableContext context = new EnvironmentVariableContext(
                EnvironmentVariableContext.GO_ENVIRONMENT_NAME, CaseInsensitiveString.str(this.name()));
        this.getVariables().addTo(context);
        return context;
    }

    @Override
    public List<CaseInsensitiveString> getPipelineNames() {
        List<CaseInsensitiveString> allNames = new ArrayList<CaseInsensitiveString>();
        for(EnvironmentConfig part : this)
        {
            for (CaseInsensitiveString pipe : part.getPipelineNames())
            {
                if(!allNames.contains(pipe))
                    allNames.add(pipe);
            }
        }
        return allNames;
    }

    @Override
    public EnvironmentPipelinesConfig getPipelines() {
        EnvironmentPipelinesConfig allPipelines = new EnvironmentPipelinesConfig();
        for(EnvironmentConfig part : this)
        {
            EnvironmentPipelinesConfig partPipes = part.getPipelines();
            for(EnvironmentPipelineConfig partPipe : partPipes)
            {
                if(!allPipelines.containsPipelineNamed(partPipe.getName()))
                    allPipelines.add(partPipe);
            }
        }
        return  allPipelines;
    }

    @Override
    public EnvironmentVariablesConfig getVariables() {
        EnvironmentVariablesConfig allVariables = new EnvironmentVariablesConfig();
        for(EnvironmentConfig part : this)
        {
            for(EnvironmentVariableConfig partVariable : part.getVariables())
            {
                if(!allVariables.contains(partVariable))
                    allVariables.add(partVariable);
            }
        }
        return allVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        EnvironmentConfig that = as(EnvironmentConfig.class,o);
        if(that == null)
            return  false;

        if (this.getAgents() != null ? !this.getAgents().equals(that.getAgents()) : that.getAgents() != null) {
            return false;
        }
        if (this.name() != null ? !this.name().equals(that.name()) : that.name() != null) {
            return false;
        }
        if (this.getPipelines() != null ? !this.getPipelines().equals(that.getPipelines()) : that.getPipelines() != null) {
            return false;
        }
        if (this.getVariables() != null ? !this.getVariables().equals(that.getVariables()) : that.getVariables() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (this.name() != null ? this.name().hashCode() : 0);
        result = 31 * result + (this.getAgents() != null ? this.getAgents().hashCode() : 0);
        result = 31 * result + (this.getPipelines() != null ? this.getPipelines().hashCode() : 0);
        result = 31 * result + (this.getVariables() != null ? this.getVariables().hashCode() : 0);
        return result;
    }

    private static <T> T as(Class<T> clazz, Object o){
        if(clazz.isInstance(o)){
            return clazz.cast(o);
        }
        return null;
    }

    @Override
    public ConfigOrigin getOrigin() {
        return null;
    }
}
