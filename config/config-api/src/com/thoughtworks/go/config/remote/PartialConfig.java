package com.thoughtworks.go.config.remote;


import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PipelineGroups;

/**
 * Part of cruise configuration that can be stored outside of main cruise-config.xml.
 * It can be merged with others and main configuration.
 */
public class PartialConfig implements Validatable, ConfigOriginTraceable {
    // consider to include source of this part.

    private ConfigOrigin origin;

    private EnvironmentsConfig environments = new EnvironmentsConfig();
    private PipelineGroups pipelines = new PipelineGroups();

    public PartialConfig(){
    }
    public PartialConfig(PipelineGroups pipelines){
        this.pipelines = pipelines;
    }
    public PartialConfig(EnvironmentsConfig environments,PipelineGroups pipelines){
        this.environments = environments;
        this.pipelines = pipelines;
    }

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public ConfigErrors errors() {
        return null;
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
}
