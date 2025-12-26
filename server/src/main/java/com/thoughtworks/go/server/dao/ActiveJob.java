/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.dao;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

public class ActiveJob {
    private long id;
    private String pipelineName;
    private String buildName;
    private String stageName;
    private int pipelineCounter;
    private String pipelineLabel;

    public ActiveJob() {
    }

    public ActiveJob(long id, String pipelineName, int pipelineCounter, String pipelineLabel, String stageName, String buildName) {
        this();
        this.id = id;
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.pipelineLabel = pipelineLabel;
        this.stageName = stageName;
        this.buildName = buildName;
    }

    public long getId() {
        return id;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    public void setPipelineLabel(String pipelineLabel) {
        this.pipelineLabel = pipelineLabel;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public int getPipelineCounter() {
        return pipelineCounter;
    }

    public void setPipelineCounter(int pipelineCounter) {
        this.pipelineCounter = pipelineCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ActiveJob activeJob = (ActiveJob) o;

        return id == activeJob.id &&
            Objects.equals(buildName, activeJob.buildName) &&
            pipelineCounter == activeJob.pipelineCounter &&
            Objects.equals(pipelineLabel, activeJob.pipelineLabel) &&
            Objects.equals(pipelineName, activeJob.pipelineName) &&
            Objects.equals(stageName, activeJob.stageName);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(id);
        result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (buildName != null ? buildName.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        result = 31 * result + pipelineCounter;
        result = 31 * result + (pipelineLabel != null ? pipelineLabel.hashCode() : 0);
        return result;
    }
}
