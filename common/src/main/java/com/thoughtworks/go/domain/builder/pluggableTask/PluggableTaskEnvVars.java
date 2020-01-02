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
package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.plugin.api.task.EnvironmentVariables;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.Collections;
import java.util.Map;

public class PluggableTaskEnvVars implements EnvironmentVariables {
    private final Console.SecureEnvVarSpecifier secureEnvVarSpecifier;
    private final Map<String, String> envVarMap;

    public PluggableTaskEnvVars(final EnvironmentVariableContext variableContext) {
        envVarMap = Collections.unmodifiableMap(variableContext.getProperties());
        secureEnvVarSpecifier = variableContext::isPropertySecure;
    }

    @Override
    public Map<String, String> asMap() {
        return envVarMap;
    }

    @Override
    public void writeTo(Console console) {
        console.printEnvironment(envVarMap, secureEnvVarSpecifier);
    }

    @Override
    public Console.SecureEnvVarSpecifier secureEnvSpecifier() {
        return secureEnvVarSpecifier;
    }
}
