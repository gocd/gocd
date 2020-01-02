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
package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Composite of many EnvironmentConfig instances. Hides elementary environment configurations.
 */
public class MergeEnvironmentConfig extends BaseCollection<EnvironmentConfig> implements EnvironmentConfig {
    public static final String CONSISTENT_KV = "ConsistentEnvVariables";
    private final ConfigErrors configErrors = new ConfigErrors();

    public MergeEnvironmentConfig(EnvironmentConfig... configs) {
        this(asList(configs));
    }

    public MergeEnvironmentConfig(List<EnvironmentConfig> configs) {
        boolean allPartsDoesNotHaveSameName = configs.stream()
                .peek(this::add)
                .map(EnvironmentConfig::name)
                .distinct()
                .count() > 1;

        if(allPartsDoesNotHaveSameName) {
            throw new IllegalArgumentException("partial environment configs must all have the same name");
        }
    }

    public EnvironmentConfig getFirstEditablePartOrNull() {
        return this.stream().filter(this::isEditable).findFirst().orElse(null);
    }

    private boolean isEditable(EnvironmentConfig part) {
        return part.getOrigin() == null || part.getOrigin().canEdit();
    }

    public EnvironmentConfig getFirstEditablePart() {
        EnvironmentConfig found = getFirstEditablePartOrNull();
        if (found == null)
            throw bomb("No editable configuration part");

        return found;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateDuplicateEnvironmentVariables();
        validateDuplicatePipelines();
        validateDuplicateAgents();
    }

    private void validateDuplicateAgents() {
        Set<String> uuids = new HashSet<>();
        this.stream().flatMap(part -> part.getAgents().stream())
                .map(EnvironmentAgentConfig::getUuid)
                .filter(uuid -> !uuids.add(uuid))
                .findFirst()
                .ifPresent(uuid -> configErrors.add("agent", format("Environment agent '%s' is defined more than once.", uuid)));
    }

    private void validateDuplicateEnvironmentVariables() {
        Set<String> envVariables = new HashSet<>();
        this.stream().flatMap(part -> part.getVariables().stream())
                .map(EnvironmentVariableConfig::getName)
                .filter(varName -> !envVariables.add(varName))
                .findFirst()
                .ifPresent(varName -> configErrors.add(CONSISTENT_KV, format("Environment variable '%s' is defined more than once with different values", varName)));
    }

    private void validateDuplicatePipelines() {
        Set<CaseInsensitiveString> pipelines = new HashSet<>();
        this.stream().flatMap(part -> part.getPipelineNames().stream())
                .filter(pipeline -> !pipelines.add(pipeline))
                .findFirst()
                .ifPresent(pipelineName -> configErrors.add(CONSISTENT_KV, format("Environment pipeline '%s' is defined more than once.", pipelineName)));
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
        return this.stream().anyMatch(part -> part.hasAgent(uuid));
    }

    @Override
    public boolean validateContainsAgentUUIDsFrom(Set<String> uuids) {
        return this.getAgents().stream().allMatch(envAgentConfig -> envAgentConfig.validateUuidPresent(this.name(), uuids));
    }

    @Override
    public void validateContainsOnlyPipelines(List<CaseInsensitiveString> pipelineNames) {
        this.getPipelines().validateContainsOnlyPipelines(this.name(), pipelineNames);
    }

    @Override
    public boolean containsPipeline(CaseInsensitiveString pipelineName) {
        return this.stream().anyMatch(part -> part.containsPipeline(pipelineName));
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        this.getFirstEditablePart().setConfigAttributes(attributes);
    }

    @Override
    public void addEnvironmentVariable(String name, String value) {
        this.getFirstEditablePart().addEnvironmentVariable(name, value);
    }

    @Override
    public void addEnvironmentVariable(EnvironmentVariableConfig variableConfig) {
        this.getFirstEditablePart().addEnvironmentVariable(variableConfig);
    }

    @Override
    public void addAgent(String uuid) {
        EnvironmentConfig editablePart = this.getFirstEditablePartOrNull();
        if (editablePart != null) {
            editablePart.addAgent(uuid);
        }
    }

    @Override
    public void addAgentIfNew(String uuid) {
        boolean uuidExists = this.stream().anyMatch(part -> part.hasAgent(uuid));
        if(!uuidExists){
            this.stream().filter(this::isEditable).findFirst().ifPresent(envConfig -> envConfig.addAgentIfNew(uuid));
        }
    }

    @Override
    public void addPipeline(CaseInsensitiveString pipelineName) {
        this.getFirstEditablePart().addPipeline(pipelineName);
    }

    @Override
    public void removePipeline(CaseInsensitiveString pipelineName) {
        this.getFirstEditablePart().removePipeline(pipelineName);
    }

    @Override
    public void removeAgent(String uuid) {
        for (EnvironmentConfig part : this) {
            if (part.hasAgent(uuid)) {
                if (isEditable(part))
                    part.removeAgent(uuid);
                else
                    throw bomb("cannot remove agent defined in non-editable source");
            }
        }
    }


    @Override
    public boolean hasName(CaseInsensitiveString environmentName) {
        return this.name().equals(environmentName);
    }

    @Override
    public boolean hasVariable(String variableName) {
        for (EnvironmentConfig part : this) {
            if (part.hasVariable(variableName))
                return true;
        }
        return false;
    }

    @Override
    public boolean contains(String pipelineName) {
        for (EnvironmentConfig part : this) {
            if (part.contains(pipelineName))
                return true;
        }
        return false;
    }

    @Override
    public CaseInsensitiveString name() {
        return this.first().name();
    }

    @Override
    public EnvironmentAgentsConfig getAgents() {
        EnvironmentAgentsConfig allAgents = new EnvironmentAgentsConfig();
        for (EnvironmentConfig part : this) {
            for (EnvironmentAgentConfig partAgent : part.getAgents()) {
                if (!allAgents.contains(partAgent))
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
        List<CaseInsensitiveString> allNames = new ArrayList<>();
        for (EnvironmentConfig part : this) {
            for (CaseInsensitiveString pipe : part.getPipelineNames()) {
                if (!allNames.contains(pipe))
                    allNames.add(pipe);
            }
        }
        return allNames;
    }

    @Override
    public EnvironmentPipelinesConfig getPipelines() {
        EnvironmentPipelinesConfig allPipelines = new EnvironmentPipelinesConfig();
        for (EnvironmentConfig part : this) {
            EnvironmentPipelinesConfig partPipes = part.getPipelines();
            for (EnvironmentPipelineConfig partPipe : partPipes) {
                if (!allPipelines.containsPipelineNamed(partPipe.getName()))
                    allPipelines.add(partPipe);
            }
        }
        return allPipelines;
    }

    @Override
    public EnvironmentVariablesConfig getVariables() {
        EnvironmentVariablesConfig allVariables = new EnvironmentVariablesConfig();
        for (EnvironmentConfig part : this) {
            for (EnvironmentVariableConfig partVariable : part.getVariables()) {
                if (!allVariables.contains(partVariable))
                    allVariables.add(partVariable);
            }
        }
        return allVariables;
    }

    @Override
    public EnvironmentVariablesConfig getPlainTextVariables() {
        EnvironmentVariablesConfig allVariables = new EnvironmentVariablesConfig();
        for (EnvironmentConfig part : this) {
            for (EnvironmentVariableConfig partVariable : part.getPlainTextVariables()) {
                if (!allVariables.contains(partVariable))
                    allVariables.add(partVariable);
            }
        }
        return allVariables;
    }

    @Override
    public EnvironmentVariablesConfig getSecureVariables() {
        EnvironmentVariablesConfig allVariables = new EnvironmentVariablesConfig();
        for (EnvironmentConfig part : this) {
            for (EnvironmentVariableConfig partVariable : part.getSecureVariables()) {
                if (!allVariables.contains(partVariable))
                    allVariables.add(partVariable);
            }
        }
        return allVariables;
    }

    @Override
    public EnvironmentConfig getLocal() {
        for (EnvironmentConfig part : this) {
            if (part.isLocal())
                return part;
        }
        return null;
    }

    @Override
    public boolean isLocal() {
        for (EnvironmentConfig part : this) {
            if (!part.isLocal())
                return false;
        }
        return true;
    }

    @Override
    public boolean isEnvironmentEmpty() {
        for (EnvironmentConfig part : this) {
            if (!part.isEnvironmentEmpty())
                return false;
        }
        return true;
    }

    @Override
    public EnvironmentPipelinesConfig getRemotePipelines() {
        EnvironmentPipelinesConfig remotes = new EnvironmentPipelinesConfig();
        for (EnvironmentConfig part : this) {
            remotes.addAll(part.getRemotePipelines());
        }
        return remotes;
    }

    @Override
    public EnvironmentAgentsConfig getLocalAgents() {
        EnvironmentAgentsConfig locals = new EnvironmentAgentsConfig();
        for (EnvironmentConfig part : this) {
            locals.addAll(part.getLocalAgents());
        }
        return locals;
    }

    @Override
    public boolean containsPipelineRemotely(CaseInsensitiveString pipelineName) {
        for (EnvironmentConfig part : this) {
            if (part.containsPipelineRemotely(pipelineName))
                return true;
        }
        return false;
    }

    @Override
    public boolean containsAgentRemotely(String uuid) {
        for (EnvironmentConfig part : this) {
            if (part.containsAgentRemotely(uuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsEnvironmentVariableRemotely(String variableName) {
        for (EnvironmentConfig part : this) {
            if (part.containsEnvironmentVariableRemotely(variableName)) {
                return true;
            }
        }
        return false;
    }

    public ConfigOrigin getOriginForPipeline(CaseInsensitiveString pipelineName) {
        for (EnvironmentConfig part : this) {
            if (part.containsPipeline(pipelineName)) {
                return part.getOrigin();
            }
        }
        return null;
    }

    @Deprecated //To be merged with originForAgent. Use that instead
    public ConfigOrigin getOriginForAgent(String agentUUID) {
        for (EnvironmentConfig part : this) {
            if (part.hasAgent(agentUUID)) {
                return part.getOrigin();
            }
        }
        return null;
    }

    @Override
    public Optional<ConfigOrigin> originForAgent(String agentUuid) {
        return Optional.ofNullable(getOriginForAgent(agentUuid));
    }

    public ConfigOrigin getOriginForEnvironmentVariable(String variableName) {
        for (EnvironmentConfig part : this) {
            if (part.getVariables().hasVariable(variableName)) {
                return part.getOrigin();
            }
        }
        return null;
    }

    @Override
    public boolean validateTree(ConfigSaveValidationContext validationContext, CruiseConfig preprocessedConfig) {
        validate(validationContext);
        boolean isValid = ErrorCollector.getAllErrors(this).isEmpty();
        for (EnvironmentConfig part : this) {
            isValid = isValid && part.validateTree(validationContext, preprocessedConfig);
        }
        return isValid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        EnvironmentConfig that = as(EnvironmentConfig.class, o);
        if (that == null)
            return false;

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

    private static <T> T as(Class<T> clazz, Object o) {
        if (clazz.isInstance(o)) {
            return clazz.cast(o);
        }
        return null;
    }

    @Override
    public ConfigOrigin getOrigin() {
        MergeConfigOrigin mergeConfigOrigin = new MergeConfigOrigin();
        for (EnvironmentConfig part : this) {
            mergeConfigOrigin.add(part.getOrigin());
        }
        return mergeConfigOrigin;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        throw bomb("Cannot set origins on merged config");
    }
}
