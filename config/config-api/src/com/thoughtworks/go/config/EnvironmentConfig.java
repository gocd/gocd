/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigOriginTraceable;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.List;
import java.util.Set;

/**
 * @understands the current persistent information related to a logical grouping of machines
 */
public interface EnvironmentConfig extends ParamsAttributeAware, Validatable, EnvironmentVariableScope, ConfigOriginTraceable {

    static final String NAME_FIELD = "name";
    static final String PIPELINES_FIELD = "pipelines";
    static final String AGENTS_FIELD = "agents";
    static final String VARIABLES_FIELD = "variables";

    void validate(ValidationContext validationContext);

    ConfigErrors errors();

    void addError(String fieldName, String message);

    EnvironmentPipelineMatcher createMatcher();

    boolean hasAgent(String uuid);

    boolean validateContainsOnlyUuids(Set<String> uuids);

    boolean containsPipeline(CaseInsensitiveString pipelineName);

    void addAgent(String uuid);

    void addAgentIfNew(String uuid);

    void removeAgent(String uuid);

    boolean hasName(CaseInsensitiveString environmentName);

    void addPipeline(CaseInsensitiveString pipelineName);

    void removePipeline(CaseInsensitiveString pipelineName);

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

    EnvironmentVariablesConfig getPlainTextVariables();

    EnvironmentVariablesConfig getSecureVariables();

    EnvironmentConfig getLocal();

    boolean isLocal();

    boolean isEnvironmentEmpty();

    EnvironmentPipelinesConfig getRemotePipelines();

    EnvironmentAgentsConfig getLocalAgents();

    boolean containsPipelineRemotely(CaseInsensitiveString pipelineName);
}
