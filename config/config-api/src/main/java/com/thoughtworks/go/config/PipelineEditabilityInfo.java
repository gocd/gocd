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

public class PipelineEditabilityInfo {
    private final CaseInsensitiveString pipelineName;
    private final boolean canUserEditPipeline;
    private boolean isOriginLocal;

    public PipelineEditabilityInfo(CaseInsensitiveString pipelineName, boolean canUserEditPipeline, boolean isOriginLocal) {
        this.pipelineName = pipelineName;
        this.canUserEditPipeline = canUserEditPipeline;
        this.isOriginLocal = isOriginLocal;
    }

    public CaseInsensitiveString getPipelineName() {
        return pipelineName;
    }

    public boolean canUserEditPipeline() {
        return canUserEditPipeline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PipelineEditabilityInfo that = (PipelineEditabilityInfo) o;

        if (canUserEditPipeline != that.canUserEditPipeline) return false;
        if (isOriginLocal != that.isOriginLocal) return false;
        return pipelineName != null ? pipelineName.equals(that.pipelineName) : that.pipelineName == null;
    }

    @Override
    public int hashCode() {
        int result = pipelineName != null ? pipelineName.hashCode() : 0;
        result = 31 * result + (canUserEditPipeline ? 1 : 0);
        result = 31 * result + (isOriginLocal ? 1 : 0);
        return result;
    }

    public boolean isOriginLocal() {
        return isOriginLocal;
    }
}