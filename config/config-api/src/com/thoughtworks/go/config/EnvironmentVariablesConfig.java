/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

/**
 * @understands environment variables that are passed to a running job
 */
@ConfigTag("environmentvariables")
@ConfigCollection(EnvironmentVariableConfig.class)
public class EnvironmentVariablesConfig extends BaseCollection<EnvironmentVariableConfig> implements Serializable, ParamsAttributeAware, Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public EnvironmentVariablesConfig() {
    }

    public EnvironmentVariablesConfig(List<EnvironmentVariableConfig> elements) {
        super(elements);
    }
    public EnvironmentVariablesConfig(EnvironmentVariableConfig[] elements) {
        super(elements);
    }

    public void validate(ValidationContext validationContext) {
        Map<String, EnvironmentVariableConfig> map = new HashMap<>();
        for (EnvironmentVariableConfig config : this) {
            config.validateName(map, validationContext);
        }
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();
        for (EnvironmentVariableConfig config : this) {
            isValid = config.validateTree(validationContext) && isValid;
        }
        return isValid;
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void add(String name, String value) {
        add(new EnvironmentVariableConfig(name.trim(), value));
    }

    public void addTo(EnvironmentVariableContext variableContext) {
        for (EnvironmentVariableConfig variable : this) {
            variable.addTo(variableContext);
        }
    }

    public void addToIfExists(EnvironmentVariableContext variableContext) {
        for (EnvironmentVariableConfig variable : this) {
            variable.addToIfExists(variableContext);
        }
    }

    public EnvironmentVariablesConfig overrideWith(EnvironmentVariablesConfig environmentVariablesConfig) {
        EnvironmentVariablesConfig variablesConfig = new EnvironmentVariablesConfig();
        variablesConfig.addAll(this);
        for (EnvironmentVariableConfig environmentVariableConfig : environmentVariablesConfig) {
            variablesConfig.removeIfExists(environmentVariableConfig.getName());
            variablesConfig.add(environmentVariableConfig);
        }

        return variablesConfig;
    }

    private void removeIfExists(String name) {
        EnvironmentVariableConfig configToRemove = null;
        for (EnvironmentVariableConfig config : this) {
            if (config.getName().equals(name)) {
                configToRemove = config;
                break;
            }
        }
        if (configToRemove != null) {
            this.remove(configToRemove);
        }
    }

    public boolean hasVariable(String variableName) {
        for (EnvironmentVariableConfig variableConfig : this) {
            if (variableConfig.hasName(variableName)) {
                return true;
            }
        }
        return false;
    }

    public void setConfigAttributes(Object attributes) {
        this.clear();
        if (attributes != null) {
            for (Map attributeMap : (List<Map>) attributes) {
                EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(new GoCipher());
                try {
                    environmentVariableConfig.setConfigAttributes(attributeMap);
                    this.add(environmentVariableConfig);
                } catch (IllegalArgumentException e) {
                    continue;
                }
            }
        }
    }

    public EnvironmentVariablesConfig getSecureVariables() {
        EnvironmentVariablesConfig result = new EnvironmentVariablesConfig();
        for (EnvironmentVariableConfig environmentVariableConfig : this) {
            if (environmentVariableConfig.isSecure()) {
                result.add(environmentVariableConfig);
            }
        }
        return result;
    }

    public EnvironmentVariablesConfig getPlainTextVariables() {
        EnvironmentVariablesConfig result = new EnvironmentVariablesConfig();
        for (EnvironmentVariableConfig environmentVariableConfig : this) {
            if (environmentVariableConfig.isPlain()) {
                result.add(environmentVariableConfig);
            }
        }
        return result;

    }

}
