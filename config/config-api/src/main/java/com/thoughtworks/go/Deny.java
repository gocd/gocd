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

package com.thoughtworks.go;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.Objects;

@ConfigTag("deny")
public class Deny implements Directive {
    @ConfigAttribute(value = "action", optional = false)
    protected String action;

    @ConfigAttribute(value = "type", optional = false)
    protected String type;

    @ConfigValue
    private String resource;

    public Deny() {
    }

    public Deny (String action, String type, String resource) {
        this.action = action;
        this.type = type;
        this.resource = resource;
    }

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public ConfigErrors errors() {
        return new ConfigErrors();
    }

    @Override
    public void addError(String fieldName, String message) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Deny deny = (Deny) o;
        return Objects.equals(action, deny.action) &&
                Objects.equals(type, deny.type) &&
                Objects.equals(resource, deny.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, type, resource);
    }
}