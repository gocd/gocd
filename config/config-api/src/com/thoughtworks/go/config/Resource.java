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

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PersistentObject;

import java.io.Serializable;

@ConfigTag("resource")
public class Resource extends PersistentObject implements Serializable, Comparable<Resource>, Validatable {
    @ConfigValue @ValidationErrorKey(value = JobConfig.RESOURCES) private String name=new String();
    private static final String VALID_CHARACTER_CLASS = "[-\\w\\s|.]";
    public static final String VALID_REGEX = "^" + VALID_CHARACTER_CLASS + "*$";
    public static final String VALID_REGEX_WHEN_IN_TEMPLATES = "^" + "[-\\w\\s|.#{}]" + "*$";
    private long buildId;

    ConfigErrors configErrors = new ConfigErrors();
    public static final String NAME = "name";

    public Resource() {
    }

    public Resource(String name) {
        setName(name);
    }

    public Resource(Resource resource) {
        this(resource.name);
        this.configErrors = resource.configErrors;
    }

    public String getName() {
        return name.trim(); //For case that direct field access e.g. MagicalCruiseConfigLoader
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String toString() {
        return getName();
    }

    public void setBuildId(long id) {
        this.buildId = id;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Resource resource = (Resource) o;

        if (name != null ? !getName().equalsIgnoreCase(resource.getName()) : resource.name != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (name != null ? name.toLowerCase().hashCode() : 0);
    }

    public int compareTo(Resource other) {
        return name.compareTo(other.name);
    }

    public static Resources resources(String... names) {
        Resources resources = new Resources();
        for (String name : names) {
            resources.add(new Resource(name));
        }
        return resources;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    public void validate(ValidationContext validationContext) {
        if (validationContext.isWithinTemplates()) {
            if (!name.matches(VALID_REGEX_WHEN_IN_TEMPLATES)) {
                configErrors.add(JobConfig.RESOURCES,
                        String.format("Resource name '%s' is not valid. Valid names can contain valid parameter syntax or valid alphanumeric with hyphens,dots or pipes", getName()));
            }
            return;
        }
        if (!name.matches(VALID_REGEX)) {
            configErrors.add(JobConfig.RESOURCES, String.format("Resource name '%s' is not valid. Valid names much match '%s'", getName(), Resource.VALID_REGEX));
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

}
