/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

@ConfigTag("params")
@ConfigCollection(ParamConfig.class)
public class ParamsConfig extends BaseCollection<ParamConfig> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public ParamsConfig() {}

    public ParamsConfig(ParamConfig... items) {
        super(items);
    }

    public ParamsConfig(List<ParamConfig> elements) {
        super(elements);
    }

    public ParamsConfig(ParamsConfig paramsConfig) {
        this();
        addAll(paramsConfig);
    }

    public ParamsConfig addOrReplace(ParamsConfig newParams) {
        ParamsConfig myCopy = new ParamsConfig(this);
        for (ParamConfig newParam : newParams) {
            myCopy.removeParamNamedIfExists(newParam.getName());
            myCopy.add(newParam);
        }
        return myCopy;
    }

    public boolean validateTree(PipelineConfigSaveValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();

        for (ParamConfig paramConfig : this) {
            isValid = paramConfig.validateTree(validationContext) && isValid;
        }
        return isValid;
    }
    @Override
    public void validate(ValidationContext validationContext) {
        Map<String, ParamConfig> paramConfigMap = new HashMap<>();
        for (ParamConfig paramConfig : this) {
            paramConfig.validateName(paramConfigMap, validationContext);
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

    private void removeParamNamedIfExists(String name) {
        removeFirstIf(p -> p.getName().equals(name));
    }

    public boolean hasParamNamed(String name) {
        return stream().anyMatch(c -> c.getName().equals(name));
    }

    public @Nullable ParamConfig getParamNamed(String name) {
        return stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public void setConfigAttributes(Object attributes) {
        if (attributes != null) {
            this.clear();
            for (Map<String, String> attributeMap : (List<Map<String, String>>) attributes) {
                String name = attributeMap.get(ParamConfig.NAME);
                String value = attributeMap.get(ParamConfig.VALUE);
                if (isBlank(name) && isBlank(value)) {
                    continue;
                }
                this.add(new ParamConfig(name, value));
            }
        }
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (ParamConfig paramConfig : this) {
            names.add(paramConfig.getName());
        }
        return names;
    }
}
