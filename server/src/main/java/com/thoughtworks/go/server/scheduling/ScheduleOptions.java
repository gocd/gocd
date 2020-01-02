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
package com.thoughtworks.go.server.scheduling;

import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.security.GoCipher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @understands parameter object that contains information like revisions and variables with which the pipeline is triggered
 */
public class ScheduleOptions {
    private final Map<String, String> specifiedRevisions;
    private final EnvironmentVariablesConfig variables;
    private Boolean shouldPerformMDUBeforeScheduling = true;

    public ScheduleOptions() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public ScheduleOptions(Map<String, String> specifiedRevisions, Map<String, String> variables, Map<String, String> secureVariables) {
        this(new GoCipher(), specifiedRevisions, variables, secureVariables);
    }

    public ScheduleOptions(GoCipher goCipher, Map<String, String> specifiedRevisions, Map<String, String> variables, Map<String, String> secureVariables) {
        this.specifiedRevisions = specifiedRevisions;
        this.variables = new EnvironmentVariablesConfig();
        for (Map.Entry<String, String> nameValue : variables.entrySet()) {
            this.variables.add(new EnvironmentVariableConfig(nameValue.getKey(), nameValue.getValue()));
        }
        for (Map.Entry<String, String> nameValue : secureVariables.entrySet()) {
            this.variables.add(new EnvironmentVariableConfig(goCipher,nameValue.getKey(), nameValue.getValue(), true));
        }
    }

    public Map<String, String> getSpecifiedRevisions() {
        return specifiedRevisions;
    }

    public EnvironmentVariablesConfig getVariables() {
        return variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScheduleOptions that = (ScheduleOptions) o;

        if (specifiedRevisions != null ? !specifiedRevisions.equals(that.specifiedRevisions) : that.specifiedRevisions != null) {
            return false;
        }
        // `Set` because we explicitly want to ignore ordering while comparing, for tests
        if (variables != null ? !new HashSet<>(variables).equals(new HashSet<>(that.variables)) : that.variables != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = specifiedRevisions != null ? specifiedRevisions.hashCode() : 0;
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        return result;
    }

    public Boolean shouldPerformMDUBeforeScheduling() {
        return shouldPerformMDUBeforeScheduling;
    }
    public void shouldPerformMDUBeforeScheduling(Boolean shouldPerformMDUBeforeScheduling) {
        this.shouldPerformMDUBeforeScheduling = shouldPerformMDUBeforeScheduling;
    }
}
