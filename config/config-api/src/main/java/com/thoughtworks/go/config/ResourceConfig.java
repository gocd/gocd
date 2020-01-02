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
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@ConfigTag("resource")
public class ResourceConfig implements Serializable, Comparable<ResourceConfig>, Validatable {
    private static final String VALID_CHARACTER_CLASS = "[-\\w\\s|.]";
    public static final String VALID_REGEX = "^" + VALID_CHARACTER_CLASS + "*$";
    public static final String VALID_REGEX_WHEN_IN_TEMPLATES = "^" + "[-\\w\\s|.#{}]" + "*$";
    public static final String NAME = "name";

    @ConfigValue
    @ValidationErrorKey(value = JobConfig.RESOURCES)
    private String name;
    private ConfigErrors configErrors = new ConfigErrors();


    public ResourceConfig() {
    }

    public ResourceConfig(String name) {
        setName(name);
    }

    public ResourceConfig(ResourceConfig resourceConfig) {
        this(resourceConfig.name);
        this.configErrors = resourceConfig.configErrors;
    }

    public boolean hasErrors(){
        return !this.errors().isEmpty();
    }

    public String getName() {
        return StringUtils.trimToNull(name);
    }

    public void setName(String name) {
        this.name = StringUtils.trimToNull(name);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceConfig that = (ResourceConfig) o;

        return getName() != null ? getName().equalsIgnoreCase(that.getName()) : that.getName() == null;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().toLowerCase().hashCode() : 0;
    }

    @Override
    public int compareTo(ResourceConfig other) {
        return name.compareTo(other.name);
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (validationContext != null && validationContext.isWithinTemplates()) {
            if (!name.matches(VALID_REGEX_WHEN_IN_TEMPLATES)) {
                configErrors.add(JobConfig.RESOURCES,
                        String.format("Resource name '%s' is not valid. Valid names can contain valid parameter syntax or valid alphanumeric with hyphens,dots or pipes", getName()));
            }
            return;
        }
        if (!name.matches(VALID_REGEX)) {
            configErrors.add(JobConfig.RESOURCES, String.format("Resource name '%s' is not valid. Valid names much match '%s'", getName(), ResourceConfig.VALID_REGEX));
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
