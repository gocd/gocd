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
package com.thoughtworks.go.server.web;

public final class PipelineRevisionRange {
    private final String pipelineName;
    private final String fromRevision;
    private final String toRevision;

    public PipelineRevisionRange(String pipelineName, String fromRevision, String toRevision) {
        this.pipelineName = pipelineName;
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getFromRevision() {
        return fromRevision;
    }

    public String getToRevision() {
        return toRevision;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PipelineRevisionRange");
        sb.append("{pipelineName='").append(pipelineName).append('\'');
        sb.append(", fromRevision='").append(fromRevision).append('\'');
        sb.append(", toRevision='").append(toRevision).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineRevisionRange that = (PipelineRevisionRange) o;

        if (fromRevision != null ? !fromRevision.equals(that.fromRevision) : that.fromRevision != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (toRevision != null ? !toRevision.equals(that.toRevision) : that.toRevision != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = pipelineName != null ? pipelineName.hashCode() : 0;
        result = 31 * result + (fromRevision != null ? fromRevision.hashCode() : 0);
        result = 31 * result + (toRevision != null ? toRevision.hashCode() : 0);
        return result;
    }
}
