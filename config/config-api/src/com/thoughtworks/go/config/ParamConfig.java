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

import java.util.Map;

import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.StringUtil;

@ConfigTag("param")
public class ParamConfig implements Validatable {
    @ConfigAttribute(value = "name", optional = false) private String name;
    @ConfigValue private String value;
    private final ConfigErrors configErrors = new ConfigErrors();

    public static final String NAME = "name";
    public static final String VALUE = "valueForDisplay";

    public ParamConfig() {
        //Required for MagicalLoader
    }

    public ParamConfig(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    public void validate(ValidationContext validationContext) {
        if (new NameTypeValidator().isNameInvalid(name)) {
            errors().add(NAME, NameTypeValidator.errorMessage("parameter", name));
        }
    }


    public void validateName(Map<String, ParamConfig> paramConfigMap, ValidationContext validationContext) {
        CaseInsensitiveString parentName = validationContext.getPipeline().name();

        if (StringUtil.isBlank(name)) {
            configErrors.add("name", String.format("Parameter cannot have an empty name for pipeline '%s'.", parentName));
            return;
        }
        String currentParamName = name.toLowerCase();

        ParamConfig paramWithSameName = paramConfigMap.get(currentParamName);
        if (paramWithSameName != null) {
            paramWithSameName.addNameConflictError(name, parentName);
            addNameConflictError(name, parentName);
            return;
        }
        paramConfigMap.put(currentParamName, this);
    }

    private void addNameConflictError(String paramName, Object parentName) {
        configErrors.add("name", String.format("Param name '%s' is not unique for pipeline '%s'.", paramName, parentName));
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParamConfig that = (ParamConfig) o;
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return !(value != null ? !value.equals(that.value) : that.value != null);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "ParamConfig{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public String getValueForDisplay(){
        return getValue();
    }
}
