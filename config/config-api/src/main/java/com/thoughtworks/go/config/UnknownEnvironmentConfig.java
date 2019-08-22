/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.remote.UnknownOrigin;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.*;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;
import static com.thoughtworks.go.util.command.EnvironmentVariableContext.GO_ENVIRONMENT_NAME;
import static java.util.Collections.emptyList;

/**
 * represents an environment config which does not exist either in config xml or config repo
 */
public class UnknownEnvironmentConfig implements EnvironmentConfig {
    private CaseInsensitiveString name;
    private ConfigOrigin origin = new UnknownOrigin();
    private EnvironmentAgentsConfig agents = new EnvironmentAgentsConfig();

    public UnknownEnvironmentConfig() {
    }

    public UnknownEnvironmentConfig(final CaseInsensitiveString name) {
        this.name = name;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        throw new UnsupportedOperationException("Cannot validate an UnknownEnvironmentConfig!");
    }

    @Override
    public ConfigErrors errors() {
        return new ConfigErrors();
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
        return agents.stream().anyMatch(agent -> agent.hasUuid(uuid));
    }

    @Override
    public boolean validateContainsAgentUUIDsFrom(Set<String> uuids) {
        return agents.stream().allMatch(agent -> agent.validateUuidPresent(name, uuids));
    }

    @Override
    public boolean containsPipeline(CaseInsensitiveString pipelineName) {
        return false;
    }

    @Override
    public void addAgent(String uuid) {
        agents.add(new EnvironmentAgentConfig(uuid));
    }

    @Override
    public void addAgentIfNew(String uuid) {
        EnvironmentAgentConfig envAgentConfig = new EnvironmentAgentConfig(uuid);
        if (!agents.contains(envAgentConfig)) {
            agents.add(envAgentConfig);
        }
    }

    @Override
    public void removeAgent(String uuid) {
        agents.remove(new EnvironmentAgentConfig(uuid));
    }

    @Override
    public boolean hasName(CaseInsensitiveString environmentName) {
        return name.equals(environmentName);
    }

    @Override
    public void addPipeline(CaseInsensitiveString pipelineName) {
        throw new UnsupportedOperationException("Cannot add pipeline to an UnknownEnvironmentConfig!");
    }

    @Override
    public void removePipeline(CaseInsensitiveString pipelineName) {
//        throw new UnsupportedOperationException("Cannot remove pipeline from an UnknownEnvironmentConfig!");
    }

    @Override
    public boolean contains(String pipelineName) {
        return false;
    }

    @Override
    public void validateContainsOnlyPipelines(List<CaseInsensitiveString> pipelineNames) {

    }

    @Override
    public CaseInsensitiveString name() {
        return name;
    }

    @Override
    public EnvironmentAgentsConfig getAgents() {
        return agents;
    }

    @Override
    public void addEnvironmentVariable(String name, String value) {
        throw new UnsupportedOperationException("Cannot add an environment variable to an UnknownEnvironmentConfig!");
    }

    @Override
    public void addEnvironmentVariable(EnvironmentVariableConfig variableConfig) {
        throw new UnsupportedOperationException("Cannot add an environment variable to an UnknownEnvironmentConfig!");
    }

    @Override
    public EnvironmentVariableContext createEnvironmentContext() {
        return new EnvironmentVariableContext(GO_ENVIRONMENT_NAME, str(name));
    }

    @Override
    public List<CaseInsensitiveString> getPipelineNames() {
        return emptyList();
    }

    @Override
    public EnvironmentPipelinesConfig getPipelines() {
        return new EnvironmentPipelinesConfig();
    }

    @Override
    public boolean hasVariable(String variableName) {
        return false;
    }

    @Override
    public EnvironmentVariablesConfig getVariables() {
        return new EnvironmentVariablesConfig();
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(NAME_FIELD)) {
            name = new CaseInsensitiveString((String) attributeMap.get(NAME_FIELD));
        }
    }

    @Override
    public EnvironmentVariablesConfig getPlainTextVariables() {
        return new EnvironmentVariablesConfig();
    }

    @Override
    public EnvironmentVariablesConfig getSecureVariables() {
        return new EnvironmentVariablesConfig();
    }

    @Override
    public EnvironmentConfig getLocal() {
        return null;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public boolean isEnvironmentEmpty() {
        return agents.isEmpty();
    }

    @Override
    public EnvironmentPipelinesConfig getRemotePipelines() {
        return new EnvironmentPipelinesConfig();
    }

    @Override
    public EnvironmentAgentsConfig getLocalAgents() {
        return new EnvironmentAgentsConfig();
    }

    @Override
    public boolean containsPipelineRemotely(CaseInsensitiveString pipelineName) {
        return false;
    }

    @Override
    public boolean containsAgentRemotely(String uuid) {
        return false;
    }

    @Override
    public boolean containsEnvironmentVariableRemotely(String variableName) {
        return false;
    }

    @Override
    public boolean validateTree(ConfigSaveValidationContext validationContext, CruiseConfig preprocessedConfig) {
        throw new UnsupportedOperationException("Cannot validate an UnknownEnvironmentConfig!");
    }

    @Override
    public Optional<ConfigOrigin> originForAgent(String agentUuid) {
        if (this.hasAgent(agentUuid)) {
            return Optional.of(this.getOrigin());
        }
        return Optional.empty();
    }

    @Override
    public ConfigOrigin getOrigin() {
        return origin;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        throw new UnsupportedOperationException("Cannot set origin on an UnknownEnvironmentConfig!");
    }

    @Override
    public boolean isUnknown() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnknownEnvironmentConfig that = (UnknownEnvironmentConfig) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(origin, that.origin) &&
                Objects.equals(agents, that.agents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, origin, agents);
    }
}
