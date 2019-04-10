/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public abstract class AbstractDirective implements Directive {
    @ConfigAttribute(value = "action", optional = false)
    protected String action;

    @ConfigAttribute(value = "type", optional = false)
    protected String type;

    @ConfigValue
    private String resource;

    private final ConfigErrors configErrors = new ConfigErrors();

    public AbstractDirective() {
    }

    public AbstractDirective(String action, String type, String resource) {
        this.action = action;
        this.type = type;
        this.resource = resource;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        RulesValidationContext rulesValidationContext = validationContext.getRulesValidationContext();

        if (isInvalid(action, rulesValidationContext.getAllowedActions())) {
            this.addError("action", format("Invalid action, must be one of %s.", rulesValidationContext.getAllowedActions()));
        }

        if (isInvalid(type, rulesValidationContext.getAllowedTypes())) {
            this.addError("type", format("Invalid type, must be one of %s.", rulesValidationContext.getAllowedTypes()));
        }
    }

    private boolean isInvalid(String actionOrType, List<String> allowedActions) {
        if ("*".equals(actionOrType)) {
            return false;
        }

        return allowedActions.stream().noneMatch(it -> equalsIgnoreCase(it, actionOrType));
    }

    @Override
    public ConfigErrors errors() {
        return this.configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        this.configErrors.add(fieldName, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractDirective directive = (AbstractDirective) o;
        return Objects.equals(action, directive.action) &&
                Objects.equals(type, directive.type) &&
                Objects.equals(resource, directive.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, type, resource);
    }

    @Override
    public boolean hasErrors() {
        return !this.configErrors.isEmpty();
    }
}
