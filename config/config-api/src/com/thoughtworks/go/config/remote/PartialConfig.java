package com.thoughtworks.go.config.remote;


import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;

/**
 * Part of cruise configuration that can be stored outside of main cruise-config.xml.
 * It can be merged with others and main configuration.
 */
public class PartialConfig implements Validatable, ConfigOriginTraceable {
    // consider to include source of this part.

    private ConfigOrigin origin;

    private EnvironmentsConfig environments;
    private PipelineConfigs pipelines;

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
}
