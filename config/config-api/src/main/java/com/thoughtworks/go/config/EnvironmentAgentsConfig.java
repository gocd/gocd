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

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
* @understands references to existing agents that are associated to an Environment
 */
public class EnvironmentAgentsConfig extends BaseCollection<EnvironmentAgentConfig> implements ParamsAttributeAware, Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

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

    public List<String> getUuids() {
        return this.stream().map(EnvironmentAgentConfig::getUuid).collect(toList());
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        if (attributes != null) {
            this.clear();
            ((List<Map<String, String>>)attributes).forEach(attributeMap -> this.add(new EnvironmentAgentConfig(attributeMap.get("uuid"))));
        }
    }
}
