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
package com.thoughtworks.go.server.ui;

/**
 * @understands identifying a pipeline uniquely in both config and db
 */
public class PipelineId {
    private final String pipelineName;
    private final Long pipelineId;

    public PipelineId(final String pipelineName, final Long pipelineId) {
        this.pipelineName = pipelineName;
        this.pipelineId = pipelineId;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public Long getPipelineId() {
        return pipelineId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineId that = (PipelineId) o;

        if (pipelineId != null ? !pipelineId.equals(that.pipelineId) : that.pipelineId != null) {
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
        result = 31 * result + (pipelineId != null ? pipelineId.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "PipelineId{" +
                "pipelineName='" + pipelineName + '\'' +
                ", pipelineId=" + pipelineId +
                '}';
    }
}
