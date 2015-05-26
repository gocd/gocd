/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.lang.builder.ToStringBuilder;

import static com.thoughtworks.go.util.command.EnvironmentVariableContext.GO_ENVIRONMENT_NAME;

/**
 * @understands the current persistent information related to a logical grouping of machines
 */
@ConfigTag("environment")
public class EnvironmentConfig implements ParamsAttributeAware, Validatable, EnvironmentVariableScope {
    @ConfigAttribute(value = NAME_FIELD, optional = false) private CaseInsensitiveString name;
    @ConfigSubtag private EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
    @ConfigSubtag private EnvironmentAgentsConfig agents = new EnvironmentAgentsConfig();
    @ConfigSubtag private EnvironmentPipelinesConfig pipelines = new EnvironmentPipelinesConfig();

    static final String NAME_FIELD = "name";
    static final String PIPELINES_FIELD = "pipelines";
    static final String AGENTS_FIELD = "agents";
    static final String VARIABLES_FIELD = "variables";
    private final ConfigErrors configErrors = new ConfigErrors();

    public EnvironmentConfig() {
    }

    public EnvironmentConfig(final CaseInsensitiveString name) {
        this.name = name;
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public EnvironmentPipelineMatcher createMatcher() {
        return new EnvironmentPipelineMatcher(name, agents.getUuids(), pipelines);
    }

    public boolean hasAgent(String uuid) {
        for (EnvironmentAgentConfig agent : agents) {
            if (agent.hasUuid(uuid)) {
                return true;
            }
        }
        return false;
    }

    public void validateContainsOnlyUuids(Set<String> uuids) {
        for (EnvironmentAgentConfig agent : agents) {
            agent.validateUuidPresent(name, uuids);
        }
    }

    public boolean containsPipeline(final CaseInsensitiveString pipelineName) {
        return pipelines.containsPipelineNamed(pipelineName);
    }

    public void addAgent(String uuid) {
        agents.add(new EnvironmentAgentConfig(uuid));
    }

    public void addAgentIfNew(String uuid) {
        EnvironmentAgentConfig agentConfig = new EnvironmentAgentConfig(uuid);
        if (!agents.contains(agentConfig)) {
            agents.add(agentConfig);
        }
    }

    public void removeAgent(String uuid) {
        agents.remove(new EnvironmentAgentConfig(uuid));
    }

    public boolean hasName(final CaseInsensitiveString environmentName) {
        return name.equals(environmentName);
    }

    public void addPipeline(final CaseInsensitiveString pipelineName) {
        pipelines.add(new EnvironmentPipelineConfig(pipelineName));
    }

    public boolean contains(String pipelineName) {
        return pipelines.containsPipelineNamed(new CaseInsensitiveString(pipelineName));
    }

    public void validateContainsOnlyPipelines(List<CaseInsensitiveString> pipelineNames) {
        pipelines.validateContainsOnlyPipelines(name, pipelineNames);
    }

    public boolean hasSamePipelinesAs(EnvironmentConfig other) {
        for (EnvironmentPipelineConfig pipeline : pipelines) {
            if (other.pipelines.containsPipelineNamed(pipeline.getName())) {
                return true;
            }
        }
        return false;
    }

    public CaseInsensitiveString name() {
        return name;
    }

    public EnvironmentAgentsConfig getAgents() {
        return agents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnvironmentConfig that = (EnvironmentConfig) o;

        if (agents != null ? !agents.equals(that.agents) : that.agents != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (pipelines != null ? !pipelines.equals(that.pipelines) : that.pipelines != null) {
            return false;
        }
        if (variables != null ? !variables.equals(that.variables) : that.variables != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (agents != null ? agents.hashCode() : 0);
        result = 31 * result + (pipelines != null ? pipelines.hashCode() : 0);
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public void addEnvironmentVariable(String name, String value) {
        variables.add(new EnvironmentVariableConfig(name.trim(), value));
    }

    public EnvironmentVariableContext createEnvironmentContext() {
        EnvironmentVariableContext context = new EnvironmentVariableContext(GO_ENVIRONMENT_NAME, CaseInsensitiveString.str(name));
        variables.addTo(context);
        return context;

    }

    public List<CaseInsensitiveString> getPipelineNames() {
        ArrayList<CaseInsensitiveString> pipelineNames = new ArrayList<CaseInsensitiveString>();
        for (EnvironmentPipelineConfig pipeline : pipelines) {
            pipelineNames.add(pipeline.getName());
        }
        return pipelineNames;
    }

    public EnvironmentPipelinesConfig getPipelines() {
        return pipelines;
    }

    public boolean hasVariable(String variableName) {
        return variables.hasVariable(variableName);
    }

    public EnvironmentVariablesConfig getVariables() {
        return variables;
    }

    public EnvironmentVariablesConfig getPlainTextVariables() {
        return variables.getPlainTextVariables();
    }

    public EnvironmentVariablesConfig getSecureVariables() {
        return variables.getSecureVariables();
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(NAME_FIELD)) {
            name = new CaseInsensitiveString((String) attributeMap.get(NAME_FIELD));
        }
        if (attributeMap.containsKey(PIPELINES_FIELD)) {
            pipelines.setConfigAttributes(attributeMap.get(PIPELINES_FIELD));
        }
        if (attributeMap.containsKey(AGENTS_FIELD)) {
            agents.setConfigAttributes(attributeMap.get(AGENTS_FIELD));
        }
        if (attributeMap.containsKey(VARIABLES_FIELD)) {
            variables.setConfigAttributes(attributeMap.get(VARIABLES_FIELD));
        }
    }
}
