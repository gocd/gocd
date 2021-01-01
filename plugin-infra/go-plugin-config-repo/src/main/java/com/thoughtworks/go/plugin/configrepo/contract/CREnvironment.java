/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CREnvironment extends CRBase {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("environment_variables")
    @Expose
    private Collection<CREnvironmentVariable> environmentVariables = new ArrayList<>();
    @SerializedName("agents")
    @Expose
    private Collection<String> agents = new ArrayList<>();
    @SerializedName("pipelines")
    @Expose
    private Collection<String> pipelines = new ArrayList<>();

    public CREnvironment() {
        this(null);
    }

    public CREnvironment(String name) {
        this.name = name;
    }

    public void addEnvironmentVariable(String key, String value) {
        CREnvironmentVariable variable = new CREnvironmentVariable(key);
        variable.setValue(value);
        this.environmentVariables.add(variable);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        validateEnvironmentVariableUniqueness(errors, location);
        validateAgentUniqueness(errors, location);
        validatePipelineUniqueness(errors, location);
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for (CREnvironmentVariable var : environmentVariables) {
            String error = var.validateNameUniqueness(keys);
            if (error != null)
                errors.addError(location, error);
        }
    }

    private void validateAgentUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for (String agent : agents) {
            String lowerCase = agent.toLowerCase();
            if (keys.contains(lowerCase))
                errors.addError(location, String.format(
                        "Agent %s is defined more than once", agent));
            else
                keys.add(lowerCase);
        }
    }

    private void validatePipelineUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for (String pipeline : pipelines) {
            String lowerCase = pipeline.toLowerCase();
            if (keys.contains(lowerCase))
                errors.addError(location, String.format(
                        "Pipeline %s is defined more than once", pipeline));
            else
                keys.add(lowerCase);
        }
    }

    public void addAgent(String agentUuid) {
        this.agents.add(agentUuid);
    }

    public void addPipeline(String pipeline1) {
        this.pipelines.add(pipeline1);
    }

    public String validateNameUniqueness(HashSet<String> keys) {
        if (keys.contains(this.getName()))
            return String.format("Environment %s is defined more than once", this.getName());
        else
            keys.add(this.getName());
        return null;
    }

    @Override
    public String getLocation(String parent) {
        return StringUtils.isBlank(location) ?
                StringUtils.isBlank(name) ? String.format("Environment in %s", parent) :
                        String.format("Environment %s", name) : String.format("%s; Environment %s", location, name);
    }
}
