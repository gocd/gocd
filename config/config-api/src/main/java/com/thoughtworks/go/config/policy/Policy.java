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
package com.thoughtworks.go.config.policy;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

@ConfigTag("policy")
@ConfigCollection(Directive.class)
public class Policy extends BaseCollection<Directive> implements Validatable {
    public Policy(Directive... items) {
        super(items);
    }

    public Policy() {
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    public void validateTree(ValidationContext validationContext) {
        this.forEach(directive -> directive.validate(validationContext));
    }

    @Override
    public ConfigErrors errors() {
        return new ConfigErrors();
    }

    public boolean hasErrors() {
        return this.stream().anyMatch(Directive::hasErrors);
    }

    @Override
    public void addError(String fieldName, String message) {
    }
}
