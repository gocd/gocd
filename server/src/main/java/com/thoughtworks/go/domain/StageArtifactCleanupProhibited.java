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
package com.thoughtworks.go.domain;

public class StageArtifactCleanupProhibited extends PersistentObject {
    private String pipelineName;
    private String stageName;
    private boolean prohibited;

    public StageArtifactCleanupProhibited() {
    }

    public StageArtifactCleanupProhibited(String pipelineName, String stageName, boolean prohibited) {
        this();
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.prohibited = prohibited;
    }

    public StageArtifactCleanupProhibited(String pipelineName, String stageName) {
        this(pipelineName, stageName, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StageArtifactCleanupProhibited)) {
            return false;
        }

        StageArtifactCleanupProhibited that = (StageArtifactCleanupProhibited) o;

        if (prohibited != that.prohibited) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = pipelineName != null ? pipelineName.hashCode() : 0;
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        result = 31 * result + (prohibited ? 1 : 0);
        return result;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStageName() {
        return stageName;
    }

    public void setProhibited(boolean prohibited) {
        this.prohibited = prohibited;
    }

    @Override public String toString() {
        return "StageArtifactCleanupProhibited{" +
                "pipelineName='" + pipelineName + '\'' +
                ", stageName='" + stageName + '\'' +
                ", prohibited=" + prohibited +
                '}';
    }
}
