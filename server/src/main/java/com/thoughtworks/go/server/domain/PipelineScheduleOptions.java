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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.EnvironmentVariablesConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PipelineScheduleOptions {
    private List<MaterialForScheduling> materials = new ArrayList<>();
    private EnvironmentVariablesConfig environmentVariables = new EnvironmentVariablesConfig();
    private Boolean shouldPerformMDUBeforeScheduling = true;

    public List<MaterialForScheduling> getMaterials() {
        return materials;
    }

    public EnvironmentVariablesConfig getAllEnvironmentVariables() {
        return environmentVariables;
    }

    public EnvironmentVariablesConfig getPlainTextEnvironmentVariables() {
        return environmentVariables.getPlainTextVariables();
    }

    public EnvironmentVariablesConfig getSecureEnvironmentVariables() {
        return environmentVariables.getSecureVariables();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineScheduleOptions that = (PipelineScheduleOptions) o;
        return Objects.equals(materials, that.materials) &&
            Objects.equals(shouldPerformMDUBeforeScheduling, that.shouldPerformMDUBeforeScheduling) &&
            Objects.equals(environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materials, environmentVariables, shouldPerformMDUBeforeScheduling);
    }

    public void setMaterials(List<MaterialForScheduling> materials) {
        this.materials = materials;
    }

    public void setEnvironmentVariables(EnvironmentVariablesConfig environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public void shouldPerformMDUBeforeScheduling(boolean shouldPerformMDUBeforeScheduling) {
        this.shouldPerformMDUBeforeScheduling = shouldPerformMDUBeforeScheduling;
    }

    public Boolean shouldPerformMDUBeforeScheduling() {
        return shouldPerformMDUBeforeScheduling;
    }

}




