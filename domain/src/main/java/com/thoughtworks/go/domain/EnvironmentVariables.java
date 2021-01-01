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
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.List;

public class EnvironmentVariables extends BaseCollection<EnvironmentVariable> implements InsecureEnvironmentVariables {

    public EnvironmentVariables() {
    }

    public EnvironmentVariables(List<EnvironmentVariable> environmentVariables) {
        super(environmentVariables);
    }

    public EnvironmentVariables(EnvironmentVariable... environmentVariables) {
        super(environmentVariables);
    }

    public void addTo(EnvironmentVariableContext variableContext) {
        for (EnvironmentVariable variable : this) {
            variable.addTo(variableContext);
        }
    }

    public void addToIfExists(EnvironmentVariableContext variableContext) {
        for (EnvironmentVariable variable : this) {
            variable.addToIfExists(variableContext);
        }
    }

    public void overrideWith(EnvironmentVariables other) {
        for (EnvironmentVariable variable : other) {
            this.removeIf(v -> v.getName().equalsIgnoreCase(variable.getName()));
            this.add(variable);
        }
    }

    public void add(String name, String value) {
        add(new EnvironmentVariable(name, value, false));
    }

    public static EnvironmentVariables toEnvironmentVariables(EnvironmentVariablesConfig environmentVariableConfigs) {
        final EnvironmentVariables environmentVariables = new EnvironmentVariables();
        for (EnvironmentVariableConfig environmentVariableConfig : environmentVariableConfigs) {
            environmentVariables.add(new EnvironmentVariable(environmentVariableConfig));
        }
        return environmentVariables;
    }

    @Override
    public String getInsecureEnvironmentVariableOrDefault(String key, String defaultValue) {
        for (EnvironmentVariable variable : this) {
            if (!variable.isSecure() && variable.getName().equalsIgnoreCase(key)) {
                return variable.getValue();
            }
        }
        return defaultValue;
    }
}
