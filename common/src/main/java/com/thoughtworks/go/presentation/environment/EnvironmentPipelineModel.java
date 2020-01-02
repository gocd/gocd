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
package com.thoughtworks.go.presentation.environment;

public class EnvironmentPipelineModel implements Comparable<EnvironmentPipelineModel> {
    private final String pipelineName;
    private final String environmentName;

    public EnvironmentPipelineModel(String pipelineName, String environmentName) {
        this.pipelineName = pipelineName;
        this.environmentName = environmentName;
    }

    public EnvironmentPipelineModel(String pipelineName) {
        this(pipelineName, null);
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnvironmentPipelineModel that = (EnvironmentPipelineModel) o;

        if (environmentName != null ? !environmentName.equals(that.environmentName) : that.environmentName != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = pipelineName != null ? pipelineName.hashCode() : 0;
        result = 31 * result + (environmentName != null ? environmentName.hashCode() : 0);
        return result;
    }

    public boolean hasEnvironmentAssociated() {
        if (environmentName != null) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(EnvironmentPipelineModel other) {
        return this.pipelineName.toLowerCase().compareTo(other.pipelineName.toLowerCase());
    }

    public boolean isAssociatedWithEnvironmentOtherThan(String environmentName) {
        return !(this.environmentName == null || this.environmentName.equals(environmentName));
    }

    public boolean isAssociatedWithEnvironment(String environmentName) {
        return this.environmentName != null && this.environmentName.equals(environmentName);
    }
}
