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
package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

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


    public EnvironmentConfig getFirstEditablePartOrNull()
    {
        for(EnvironmentConfig part : this)
        {
            if(isEditable(part))
                return  part;
        }
        return  null;
    }

    private boolean isEditable(EnvironmentConfig part) {
        return part.getOrigin() == null || part.getOrigin().canEdit();
    }

    public EnvironmentConfig getFirstEditablePart()
    {
        EnvironmentConfig found = getFirstEditablePartOrNull();
        if(found == null)
            throw bomb("No editable configuration part");

        return found;
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
        if (attributes == null) {
            return;
        }
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(NAME_FIELD)) {
            CaseInsensitiveString newName = new CaseInsensitiveString((String) attributeMap.get(NAME_FIELD));
            if(!newName.equals(this.name()))
                throw bomb("Cannot update name of environment defined in multiple sources");
        }
        if (attributeMap.containsKey(PIPELINES_FIELD)) {
            Object pipelinesAttributes = attributeMap.get(PIPELINES_FIELD);
            this.setPipelineAttributes(pipelinesAttributes);
        }
        if (attributeMap.containsKey(AGENTS_FIELD)) {
            Object agentAttributes = attributeMap.get(AGENTS_FIELD);
            this.setAgentAttributes(agentAttributes);
        }
        if (attributeMap.containsKey(VARIABLES_FIELD)) {
            Object variablesAttributes = attributeMap.get(VARIABLES_FIELD);
            this.setVariablesAttributes(variablesAttributes);
        }
    }

    private void setVariablesAttributes(Object variablesAttributes) {
        if (variablesAttributes != null) {
            // these are all k=v that user wants to have set
            List<Map> variableAttributes = (List) variablesAttributes;
            List<EnvironmentVariableConfig> newProposed = new ArrayList<EnvironmentVariableConfig>();
            for (Map attributeMap : variableAttributes) {
                EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(new GoCipher());
                try {
                    environmentVariableConfig.setConfigAttributes(attributeMap);
                    newProposed.add(environmentVariableConfig);
                } catch (IllegalArgumentException e) {
                    continue;
                }
            }
            // but we cannot remove or change assignment of any variable from non-editable sources

            List<EnvironmentVariableConfig> removals = new ArrayList<EnvironmentVariableConfig>();
            List<EnvironmentVariableConfig> changes = new ArrayList<EnvironmentVariableConfig>();
            List<EnvironmentVariableConfig> nochanges = new ArrayList<EnvironmentVariableConfig>();
            for(EnvironmentConfig part : this) {
                for (EnvironmentVariableConfig existingVariable : part.getVariables()) {
                    // lets check if user is trying to remove or change something unmodifiable
                    boolean found = false;
                    for(EnvironmentVariableConfig var : newProposed)
                    {
                        if(var.getName().equals(existingVariable.getName()))
                        {
                            // trying to set variable which is already set in this part
                            if(!var.getValue().equals(existingVariable.getValue()))
                            {
                                // and it is trying to change current assignment
                                if(!isEditable(part))
                                    throw bomb(String.format("Cannot change variable %s in environment %s because it is defined in non-editable source %s",
                                            existingVariable.getName(), this.name(), part.getOrigin()));
                                //otherwise it can be changed
                                if(!changes.contains(var)) {
                                    changes.add(var);
                                    // existing assignment must be removed
                                    removals.add(existingVariable);
                                }
                            }
                            else
                            {
                                // assignment did not change so forget this change
                                if(!nochanges.contains(var))
                                    nochanges.add(var);
                            }
                            found = true;
                        }
                    }
                    if(!found)
                    {
                        // the new proposed did not contain existingVariable
                        // therefore intent is to remove it
                        if(!isEditable(part))
                            throw bomb(String.format("Cannot remove variable %s from environment %s because it is defined in non-editable source %s",
                                    existingVariable.getName(), this.name(), part.getOrigin()));
                        removals.add(existingVariable);
                    }
                    // otherwise already handled
                }
            }
            for(EnvironmentVariableConfig noChange : nochanges)
            {
                newProposed.remove(noChange);
            }
            // remove modifications from newProposed so that only new variables are there
            for(EnvironmentVariableConfig noChange : changes)
            {
                newProposed.remove(noChange);
            }
            // removes what user wanted to remove
            // and removes existing variables to be replaced
            for(EnvironmentVariableConfig toRemove : removals)
            {
                for(EnvironmentConfig part : this) {
                    part.getVariables().remove(toRemove);
                }
            }
            // add variables which are to be modified
            this.getFirstEditablePart().getVariables().addAll(changes);
            // add new variables
            this.getFirstEditablePart().getVariables().addAll(newProposed);
        }
    }

    private void setAgentAttributes(Object agentsAttributes) {
        if (agentsAttributes != null) {
            // these are all agents that user wants to have set
            List<Map> agentAttributes = (List) agentsAttributes;
            List<EnvironmentAgentConfig> newProposed = new ArrayList<EnvironmentAgentConfig>();
            for (Map attributeMap : agentAttributes) {
                EnvironmentAgentConfig agentInEnv = new EnvironmentAgentConfig((String) attributeMap.get("uuid"));
                newProposed.add(agentInEnv);
            }
            // but we cannot remove any agent from non-editable sources

            List<EnvironmentAgentConfig> removals = new ArrayList<EnvironmentAgentConfig>();
            for(EnvironmentConfig part : this) {
                for (EnvironmentAgentConfig existingAgent : part.getAgents()) {
                    // lets check if user is trying to remove something unmodifiable
                    if(!newProposed.contains(existingAgent))
                    {
                        if(!isEditable(part))
                            throw bomb(String.format("Cannot remove agent %s from environment %s because it is defined in non-editable source %s",
                                    existingAgent.getUuid(), this.name(), part.getOrigin()));
                        // otherwise it can just be removed
                        removals.add(existingAgent);
                    }
                    else
                    {
                        // trying to set something already set in one of the parts
                        // remove the attempt
                        newProposed.remove(existingAgent);
                    }
                }
            }
            for(EnvironmentAgentConfig toRemove : removals)
            {
                for(EnvironmentConfig part : this) {
                    part.getAgents().remove(toRemove);
                }
            }
            // all we have left now are new additions
            // let's just add them to first editable part
            this.getFirstEditablePart().getAgents().addAll(newProposed);
        }
    }

    private void setPipelineAttributes(Object pipelinesAttributes) {
        if (pipelinesAttributes != null) {
            // these are all pipelines that user wants to have set
            List<Map> pipelineAttributes = (List) pipelinesAttributes;
            List<EnvironmentPipelineConfig> newProposed = new ArrayList<EnvironmentPipelineConfig>();
            for (Map attributeMap : pipelineAttributes) {
                EnvironmentPipelineConfig pipeInEnv = new EnvironmentPipelineConfig(new CaseInsensitiveString((String) attributeMap.get("name")));
                newProposed.add(pipeInEnv);
            }
            // but we cannot remove any pipelines from non-editable sources

            List<EnvironmentPipelineConfig> removals = new ArrayList<EnvironmentPipelineConfig>();
            for(EnvironmentConfig part : this) {
                for (EnvironmentPipelineConfig existingPipeline : part.getPipelines()) {
                    // lets check if user is trying to remove something unmodifiable
                    if(!newProposed.contains(existingPipeline))
                    {
                        if(!isEditable(part))
                            throw bomb(String.format("Cannot remove pipeline %s from environment %s because it is defined in non-editable source %s",
                                    existingPipeline.getName(),this.name(),part.getOrigin()));
                        // otherwise it can just be removed
                        removals.add(existingPipeline);
                    }
                    else
                    {
                        // trying to set something already set in one of the parts
                        // remove the attempt
                        newProposed.remove(existingPipeline);
                    }
                }
            }
            for(EnvironmentPipelineConfig toRemove : removals)
            {
                for(EnvironmentConfig part : this) {
                    part.getPipelines().remove(toRemove);
                }
            }
            // all we have left now are new additions
            // let's just add them to first editable part
            this.getFirstEditablePart().getPipelines().addAll(newProposed);
        }
    }


    @Override
    public void addEnvironmentVariable(String name, String value) {
        this.getFirstEditablePart().addEnvironmentVariable(name,value);
    }

    @Override
    public void addAgent(String uuid) {
        this.getFirstEditablePart().addAgent(uuid);
    }

    @Override
    public void addAgentIfNew(String uuid) {
        for(EnvironmentConfig part : this)
        {
            if (part.hasAgent(uuid)) {
                return;
            }
        }
        this.getFirstEditablePart().addAgentIfNew(uuid);
    }

    @Override
    public void addPipeline(CaseInsensitiveString pipelineName) {
        this.getFirstEditablePart().addPipeline(pipelineName);
    }

    @Override
    public void removeAgent(String uuid) {
        for(EnvironmentConfig part : this)
        {
            if (part.hasAgent(uuid)) {
                if(isEditable(part))
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
        for(EnvironmentConfig part : this)
        {
            if(part.hasVariable(variableName))
                return true;
        }
        return  false;
    }

    @Override
    public boolean hasSamePipelinesAs(EnvironmentConfig other) {
        return false; //TODO: jyoti - is this correct?
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
    public EnvironmentVariablesConfig getPlainTextVariables() {
        EnvironmentVariablesConfig allVariables = new EnvironmentVariablesConfig();
        for(EnvironmentConfig part : this)
        {
            for(EnvironmentVariableConfig partVariable : part.getPlainTextVariables())
            {
                if(!allVariables.contains(partVariable))
                    allVariables.add(partVariable);
            }
        }
        return allVariables;
    }

    @Override
    public EnvironmentVariablesConfig getSecureVariables() {
        EnvironmentVariablesConfig allVariables = new EnvironmentVariablesConfig();
        for(EnvironmentConfig part : this)
        {
            for(EnvironmentVariableConfig partVariable : part.getSecureVariables())
            {
                if(!allVariables.contains(partVariable))
                    allVariables.add(partVariable);
            }
        }
        return allVariables;
    }

    @Override
    public EnvironmentConfig getLocal() {
        for(EnvironmentConfig part : this)
        {
            if(part.isLocal())
                return  part;
        }
        return  null;
    }

    @Override
    public boolean isLocal() {
        for(EnvironmentConfig part : this)
        {
            if(part.isLocal())
                return false;
        }
        return true;
    }

    @Override
    public boolean isEnvironmentEmpty() {
        for(EnvironmentConfig part : this)
        {
            if(!part.isEnvironmentEmpty())
                return false;
        }
        return true;
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
        MergeConfigOrigin mergeConfigOrigin = new MergeConfigOrigin();
        for(EnvironmentConfig part : this)
        {
            mergeConfigOrigin.add(part.getOrigin());
        }
        return mergeConfigOrigin;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        throw bomb("Cannot set origins on merged config");
    }
}
