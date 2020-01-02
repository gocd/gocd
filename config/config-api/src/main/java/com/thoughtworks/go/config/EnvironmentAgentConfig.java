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

import java.util.Set;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * @understands a reference to an existing agent that is associated to an Environment
 */
public class EnvironmentAgentConfig implements Validatable {
    private String uuid;
    private ConfigErrors configErrors = new ConfigErrors();
    public static final String UUID = "uuid";

    public EnvironmentAgentConfig() {
    }

    public EnvironmentAgentConfig(String uuid) {
        this.uuid = uuid;
    }

    public boolean hasUuid(String uuid) {
        return this.uuid.equals(uuid);
    }

    public boolean validateUuidPresent(CaseInsensitiveString name, Set<String> uuids) {
        if (isEmpty(uuids) || !uuids.contains(uuid)) {
            this.addError(UUID, format("Environment '%s' has an invalid agent uuid '%s'", name, uuid));
        }
        return errors().isEmpty();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnvironmentAgentConfig that = (EnvironmentAgentConfig) o;

        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
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
