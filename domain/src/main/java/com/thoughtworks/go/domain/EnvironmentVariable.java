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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

public class EnvironmentVariable extends PersistentObject {
    private String name;
    private boolean isSecure = false;
    private String value;
    private long entityId;
    private String entityType;

    public EnvironmentVariable() {
    }

    public EnvironmentVariable(String name, String value) {
        this(name, value, false);
    }

    public EnvironmentVariable(String name, String value, boolean isSecure) {
        this.name = name;
        this.value = value;
        this.isSecure = isSecure;
    }

    public EnvironmentVariable(EnvironmentVariableConfig environmentVariableConfig) {
        this(environmentVariableConfig.getName(), environmentVariableConfig.getValue(), environmentVariableConfig.isSecure());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public void setSecure(boolean secure) {
        isSecure = secure;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void addTo(EnvironmentVariableContext environmentVariableContext) {
        environmentVariableContext.setProperty(getName(), getValue(), isSecure());
    }

    public void addToIfExists(EnvironmentVariableContext context) {
        if (context.hasProperty(getName())) {
            addTo(context);
        }
    }

    public String getDisplayValue() {
        if (isSecure()) {
            return "****";
        }
        return getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnvironmentVariable)) return false;

        EnvironmentVariable that = (EnvironmentVariable) o;

        if (isSecure != that.isSecure) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (isSecure ? 1 : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
