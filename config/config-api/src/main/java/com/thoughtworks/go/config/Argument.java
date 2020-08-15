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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;

import static com.thoughtworks.go.config.ExecTask.ARG_LIST_STRING;

@ConfigTag(value = "arg", label = "arg")
public class Argument implements Validatable{
    @ConfigValue @ValidationErrorKey(value = ARG_LIST_STRING) private String value;
    private final ConfigErrors configErrors = new ConfigErrors();

    public Argument() {
        this("");
    }

    public Argument(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Argument arg = (Argument) o;

        if (!value.equals(arg.value)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "Arg[value=" + value + "]";
    }

    public String getValue() {
        return value;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (null == value) {
            configErrors.add("arg", "Invalid argument, cannot be null.");
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
}
