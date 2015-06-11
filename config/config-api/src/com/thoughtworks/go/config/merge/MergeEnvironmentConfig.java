package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
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
        for(EnvironmentConfig part : configs) {
            this.add(part);
        }
    }

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public ConfigErrors errors() {
        return null;
    }

    @Override
    public void addError(String fieldName, String message) {

    }

    @Override
    public EnvironmentPipelineMatcher createMatcher() {
        return null;
    }

    @Override
    public boolean hasAgent(String uuid) {
        /*for(EnvironmentConfig part : parts)
        {
            if(part.hasAgent(uuid))
                return true;
        }
        return false;*/
        return false;
    }

    @Override
    public void validateContainsOnlyUuids(Set<String> uuids) {

    }

    @Override
    public void validateContainsOnlyPipelines(List<CaseInsensitiveString> pipelineNames) {

    }


    @Override
    public boolean containsPipeline(CaseInsensitiveString pipelineName) {
        return false;
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
        return false;
    }

    @Override
    public boolean hasVariable(String variableName) {
        return false;
    }

    @Override
    public boolean hasSamePipelinesAs(EnvironmentConfig other) {
        return false;
    }

    @Override
    public boolean contains(String pipelineName) {
        return false;
    }

    @Override
    public CaseInsensitiveString name() {
        return null;
    }

    @Override
    public EnvironmentAgentsConfig getAgents() {
        return null;
    }


    @Override
    public EnvironmentVariableContext createEnvironmentContext() {
        return null;
    }

    @Override
    public List<CaseInsensitiveString> getPipelineNames() {
        return null;
    }

    @Override
    public EnvironmentPipelinesConfig getPipelines() {
        return null;
    }

    @Override
    public EnvironmentVariablesConfig getVariables() {
        return null;
    }

    @Override
    public void setConfigAttributes(Object attributes) {

    }
}
