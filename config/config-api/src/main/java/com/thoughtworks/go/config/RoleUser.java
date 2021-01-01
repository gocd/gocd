/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import java.util.Objects;

@ConfigTag("user")
public class RoleUser implements Validatable {
    private static final String NAME = "name";

    @ConfigValue private CaseInsensitiveString name;

    private final ConfigErrors configErrors = new ConfigErrors();

    public RoleUser() {
    }

    public RoleUser(final CaseInsensitiveString name) {
        this.name = name;
    }

    public RoleUser(String name) {
        this.name = new CaseInsensitiveString(name);
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

    public CaseInsensitiveString getName() {
        return name;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }

        return equals((RoleUser) that);
    }

    private boolean equals(RoleUser that) {
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override public String toString() {
        return "RoleUser{" +
                "name=" + name +
                '}';
    }

    public void addDuplicateError(String roleName) {
        configErrors.add(NAME, "User '" + CaseInsensitiveString.str(name) + "' already exists in '" + roleName + "'.");
    }
}
