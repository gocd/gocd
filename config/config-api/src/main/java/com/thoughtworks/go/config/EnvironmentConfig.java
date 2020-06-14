/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigOriginTraceable;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @understands the current persistent information related to a logical grouping of machines
 */
public interface EnvironmentConfig extends ParamsAttributeAware, Validatable, EnvironmentVariableScope, ConfigOriginTraceable, SecretParamAware {

    String NAME_FIELD = "name";
    String PIPELINES_FIELD = "pipelines";
    String AGENTS_FIELD = "agents";
    String VARIABLES_FIELD = "variables";

    @Override
    void validate(ValidationContext validationContext);

    @Override
    ConfigErrors errors();

    @Override
    void addError(String fieldName, String message);

    EnvironmentPipelineMatcher createMatcher();

    boolean hasAgent(String uuid);

    boolean validateContainsAgentUUIDsFrom(Set<String> uuids);

    boolean containsPipeline(CaseInsensitiveString pipelineName);

    void addAgent(String uuid);

    void addAgentIfNew(String uuid);

    void removeAgent(String uuid);

    boolean hasName(CaseInsensitiveString environmentName);

    void addPipeline(CaseInsensitiveString pipelineName);

    void removePipeline(CaseInsensitiveString pipelineName);

    boolean contains(String pipelineName);

    void validateContainsOnlyPipelines(List<CaseInsensitiveString> pipelineNames);

    @Override
    CaseInsensitiveString name();

    EnvironmentAgentsConfig getAgents();

    void addEnvironmentVariable(String name, String value);

    void addEnvironmentVariable(EnvironmentVariableConfig variableConfig);

    EnvironmentVariableContext createEnvironmentContext();

    List<CaseInsensitiveString> getPipelineNames();

    EnvironmentPipelinesConfig getPipelines();

    boolean hasVariable(String variableName);

    EnvironmentVariablesConfig getVariables();

    @Override
    void setConfigAttributes(Object attributes);

    EnvironmentVariablesConfig getPlainTextVariables();

    EnvironmentVariablesConfig getSecureVariables();

    EnvironmentConfig getLocal();

    boolean isLocal();

    boolean isEnvironmentEmpty();

    EnvironmentPipelinesConfig getRemotePipelines();

    EnvironmentAgentsConfig getLocalAgents();

    boolean containsPipelineRemotely(CaseInsensitiveString pipelineName);

    boolean containsAgentRemotely(String uuid);

    boolean containsEnvironmentVariableRemotely(String variableName);

    boolean validateTree(ConfigSaveValidationContext validationContext, CruiseConfig preprocessedConfig);

    Optional<ConfigOrigin> originForAgent(String agentUuid);

    default List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }


    @Override
    default boolean hasSecretParams() {
        return getVariables().hasSecretParams();
    }

    @Override
    default SecretParams getSecretParams() {
        return getVariables().getSecretParams();
    }
}
