package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.List;
import java.util.Set;

/**
 * Created by tomzo on 6/10/15.
 */
public interface EnvironmentConfig extends ParamsAttributeAware, Validatable, EnvironmentVariableScope {
    void validate(ValidationContext validationContext);

    ConfigErrors errors();

    void addError(String fieldName, String message);

    EnvironmentPipelineMatcher createMatcher();

    boolean hasAgent(String uuid);

    void validateContainsOnlyUuids(Set<String> uuids);

    boolean containsPipeline(CaseInsensitiveString pipelineName);

    void addAgent(String uuid);

    void addAgentIfNew(String uuid);

    void removeAgent(String uuid);

    boolean hasName(CaseInsensitiveString environmentName);

    void addPipeline(CaseInsensitiveString pipelineName);

    boolean contains(String pipelineName);

    void validateContainsOnlyPipelines(List<CaseInsensitiveString> pipelineNames);

    boolean hasSamePipelinesAs(EnvironmentConfig other);

    CaseInsensitiveString name();

    EnvironmentAgentsConfig getAgents();

    void addEnvironmentVariable(String name, String value);

    EnvironmentVariableContext createEnvironmentContext();

    List<CaseInsensitiveString> getPipelineNames();

    EnvironmentPipelinesConfig getPipelines();

    boolean hasVariable(String variableName);

    EnvironmentVariablesConfig getVariables();

    void setConfigAttributes(Object attributes);

}
