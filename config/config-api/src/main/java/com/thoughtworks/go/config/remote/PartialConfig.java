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
package com.thoughtworks.go.config.remote;


import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;

/**
 * Part of cruise configuration that can be stored outside of main cruise-config.xml.
 * It can be merged with others and main configuration.
 */
@ConfigTag("cruise")
public class PartialConfig implements Validatable, ConfigOriginTraceable {
    // consider to include source of this part.

    @ConfigSubtag(label = "groups") private PipelineGroups pipelines = new PipelineGroups();
    @ConfigSubtag @SkipParameterResolution private EnvironmentsConfig environments = new EnvironmentsConfig();
    @ConfigSubtag(label = "scms") private SCMs scms = new SCMs();

    private ConfigOrigin origin;

    public PartialConfig(){
    }
    public PartialConfig(PipelineGroups pipelines){
        this.pipelines = pipelines;
    }
    public PartialConfig(EnvironmentsConfig environments,PipelineGroups pipelines){
        this.environments = environments;
        this.pipelines = pipelines;
    }

    public PartialConfig(EnvironmentsConfig environments,PipelineGroups pipelines, SCMs scms){
        this.environments = environments;
        this.pipelines = pipelines;
        this.scms = scms;
    }

    @Override
    public String toString() {
        return String.format("ConfigPartial: %s pipes, %s environments, %s scms; From %s",pipelines.size(),environments.size(), scms.size(),origin);
    }

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public ConfigErrors errors() {
        return new ConfigErrors();
    }

    @Override
    public void addError(String fieldName, String message) {

    }


    @Override
    public ConfigOrigin getOrigin() {
        return origin;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        this.origin = origins;
        for(EnvironmentConfig env : this.environments)
        {
            env.setOrigins(origins);
        }
        for(PipelineConfigs pipes : this.pipelines)
        {
            pipes.setOrigins(origins);
        }
        for(SCM scm: this.scms)
        {
           scm.setOrigins(origins);
        }
    }

    public void setOrigin(ConfigOrigin origin) {
        this.origin = origin;
    }

    public EnvironmentsConfig getEnvironments() {
        return environments;
    }

    public void setEnvironments(EnvironmentsConfig environments) {
        this.environments = environments;
    }

    public PipelineGroups getGroups() {
        return pipelines;
    }

    public void setPipelines(PipelineGroups pipelines) {
        this.pipelines = pipelines;
    }

    /**
     * Validate this part within its own scope.
     */
    public void validatePart() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PartialConfig that = (PartialConfig) o;

        if (!pipelines.equals(that.pipelines)) return false;
        if (!getEnvironments().equals(that.getEnvironments())) return false;
        return getOrigin() != null ? getOrigin().equals(that.getOrigin()) : that.getOrigin() == null;

    }

    @Override
    public int hashCode() {
        int result = pipelines.hashCode();
        result = 31 * result + getEnvironments().hashCode();
        result = 31 * result + (getOrigin() != null ? getOrigin().hashCode() : 0);
        return result;
    }

    public SCMs getScms() {
        return scms;
    }

    public void setScms(SCMs scms) {
        this.scms = scms;
    }
}
