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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.lang.builder.ToStringBuilder;

import static com.thoughtworks.go.util.command.EnvironmentVariableContext.GO_ENVIRONMENT_NAME;

/**
 * @understands the current persistent information related to a logical grouping of machines
 */
@ConfigTag("environment")
public class BasicEnvironmentConfig implements EnvironmentConfig {
    @ConfigAttribute(value = NAME_FIELD, optional = false) private CaseInsensitiveString name;
    @ConfigSubtag private EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
    @ConfigSubtag private EnvironmentAgentsConfig agents = new EnvironmentAgentsConfig();
    @ConfigSubtag private EnvironmentPipelinesConfig pipelines = new EnvironmentPipelinesConfig();


    private final ConfigErrors configErrors = new ConfigErrors();
    private ConfigOrigin origin;

    public BasicEnvironmentConfig() {
    }

    public BasicEnvironmentConfig(final CaseInsensitiveString name) {
        this.name = name;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        // each of these references is defined in this.origin
        for (EnvironmentPipelineConfig pipelineRefConfig : this.pipelines) {
            ConfigReposConfig configRepos = validationContext.getConfigRepos();
            PipelineConfig pipelineConfig = validationContext.getPipelineConfigByName(pipelineRefConfig.getName());
            if (pipelineConfig == null) {
                continue;//other rule will error that we reference unknown pipeline
            }
            if (validationContext.shouldCheckConfigRepo()) {
                if (!configRepos.isReferenceAllowed(this.origin, pipelineConfig.getOrigin()))
                    pipelineRefConfig.addError(EnvironmentPipelineConfig.ORIGIN,
                            String.format("Environment defined in %s cannot reference a pipeline in %s",
                                    this.origin, displayNameFor(pipelineConfig.getOrigin())));
            }
        }
    }

    private String displayNameFor(ConfigOrigin origin) {
        return origin != null ? origin.displayName() : "cruise-config.xml";
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
        return new EnvironmentPipelineMatcher(name, agents.getUuids(), pipelines);
    }

    @Override
    public boolean hasAgent(String uuid) {
        for (EnvironmentAgentConfig agent : agents) {
            if (agent.hasUuid(uuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean validateContainsOnlyUuids(Set<String> uuids) {
        boolean isValid = true;
        for (EnvironmentAgentConfig agent : agents) {
            isValid = agent.validateUuidPresent(name, uuids) && isValid;
        }
        return isValid;
    }

    @Override
    public boolean containsPipeline(final CaseInsensitiveString pipelineName) {
        return pipelines.containsPipelineNamed(pipelineName);
    }

    @Override
    public void addAgent(String uuid) {
        agents.add(new EnvironmentAgentConfig(uuid));
    }

    @Override
    public void addAgentIfNew(String uuid) {
        EnvironmentAgentConfig agentConfig = new EnvironmentAgentConfig(uuid);
        if (!agents.contains(agentConfig)) {
            agents.add(agentConfig);
        }
    }

    @Override
    public void removeAgent(String uuid) {
        agents.remove(new EnvironmentAgentConfig(uuid));
    }

    @Override
    public boolean hasName(final CaseInsensitiveString environmentName) {
        return name.equals(environmentName);
    }

    @Override
    public void addPipeline(final CaseInsensitiveString pipelineName) {
        pipelines.add(new EnvironmentPipelineConfig(pipelineName));
    }

    @Override
    public void removePipeline(final CaseInsensitiveString pipelineName) {
        pipelines.remove(new EnvironmentPipelineConfig(pipelineName));
    }

    @Override
    public boolean contains(String pipelineName) {
        return pipelines.containsPipelineNamed(new CaseInsensitiveString(pipelineName));
    }

    @Override
    public void validateContainsOnlyPipelines(List<CaseInsensitiveString> pipelineNames) {
        pipelines.validateContainsOnlyPipelines(name, pipelineNames);
    }

    @Override
    public boolean hasSamePipelinesAs(EnvironmentConfig other) {
        for (EnvironmentPipelineConfig pipeline : pipelines) {
            for(CaseInsensitiveString name : other.getPipelineNames())
            {
                if(name.equals(pipeline.getName()))
                    return  true;
            }
        }
        return false;
    }

    @Override
    public CaseInsensitiveString name() {
        return name;
    }

    public void setName(CaseInsensitiveString name) {
        this.name = name;
    }

    @Override
    public EnvironmentAgentsConfig getAgents() {
        return agents;
    }

    public void setAgents(List<EnvironmentAgentConfig> agents) {
        this.agents.clear();
        this.agents.addAll(agents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        EnvironmentConfig that = as(EnvironmentConfig.class,o);
        if(that == null)
            return  false;

        if (agents != null ? !agents.equals(that.getAgents()) : that.getAgents() != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name()) : that.name() != null) {
            return false;
        }
        if (pipelines != null ? !pipelines.equals(that.getPipelines()) : that.getPipelines() != null) {
            return false;
        }
        if (variables != null ? !variables.equals(that.getVariables()) : that.getVariables() != null) {
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

    private static <T> T as(Class<T> clazz, Object o){
        if(clazz.isInstance(o)){
            return clazz.cast(o);
        }
        return null;
    }

    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public void addEnvironmentVariable(String name, String value) {
        variables.add(new EnvironmentVariableConfig(name.trim(), value));
    }

    @Override
    public EnvironmentVariableContext createEnvironmentContext() {
        EnvironmentVariableContext context = new EnvironmentVariableContext(
                GO_ENVIRONMENT_NAME, CaseInsensitiveString.str(name));
        variables.addTo(context);
        return context;

    }

    @Override
    public List<CaseInsensitiveString> getPipelineNames() {
        ArrayList<CaseInsensitiveString> pipelineNames = new ArrayList<>();
        for (EnvironmentPipelineConfig pipeline : pipelines) {
            pipelineNames.add(pipeline.getName());
        }
        return pipelineNames;
    }

    @Override
    public EnvironmentPipelinesConfig getPipelines() {
        return pipelines;
    }

    public void setPipelines(List<EnvironmentPipelineConfig> pipelines) {
        this.pipelines.clear();
        this.pipelines.addAll(pipelines);
    }

    @Override
    public boolean hasVariable(String variableName) {
        return variables.hasVariable(variableName);
    }

    @Override
    public EnvironmentVariablesConfig getVariables() {
        return variables;
    }

    public void setVariables(EnvironmentVariablesConfig environmentVariables) {
        this.variables = environmentVariables;
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
    @Override
    public EnvironmentVariablesConfig getPlainTextVariables() {
        return variables.getPlainTextVariables();
    }
    @Override
    public EnvironmentVariablesConfig getSecureVariables() {
        return variables.getSecureVariables();
    }

    @Override
    public EnvironmentConfig getLocal() {
        if(this.isLocal())
            return this;
        else
            return null;
    }

    @Override
    public ConfigOrigin getOrigin() {
        return origin;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        this.origin = origins;
        for(EnvironmentVariableConfig environmentVariableConfig : this.variables)
        {
            environmentVariableConfig.setOrigins(origins);
        }
    }

    @Override
    public EnvironmentPipelinesConfig getRemotePipelines() {
        if(this.isLocal())
            return new EnvironmentPipelinesConfig();
        else
            return this.pipelines;
    }

    @Override
    public EnvironmentAgentsConfig getLocalAgents() {
        if(this.isLocal())
            return this.agents;
        else
            return new EnvironmentAgentsConfig();
    }

    public boolean isLocal() {
        return this.origin == null || this.origin.isLocal();
    }

    @Override
    public boolean isEnvironmentEmpty() {
        return this.variables.isEmpty() && this.agents.isEmpty() && this.pipelines.isEmpty();
    }

    @Override
    public boolean containsPipelineRemotely(CaseInsensitiveString pipelineName) {
        if(this.isLocal())
            return false;
        if(!this.containsPipeline(pipelineName))
            return false;

        return true;
    }

}
