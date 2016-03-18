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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;

import java.io.Serializable;

/**
 * @understands Value of env variable
 */
@ConfigTag("value")
public class VariableValueConfig implements Serializable, Validatable{
    @ConfigValue
    private String value;
    private ConfigErrors configErrors = new ConfigErrors();

    public VariableValueConfig(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public VariableValueConfig() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VariableValueConfig)) {
            return false;
        }

        VariableValueConfig that = (VariableValueConfig) o;

        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }
}
