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
package com.thoughtworks.go.domain.valuestreammap;

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.Stages;

public class PipelineRevision implements Revision {
    private Stages stages = new Stages();
    private PipelineIdentifier pipelineIdentifier;

    public PipelineRevision(String pipelineName, Integer pipelineCounter, String pipelineLabel) {
        this(new PipelineIdentifier(pipelineName, pipelineCounter, pipelineLabel));
    }

    public PipelineRevision(PipelineIdentifier pipelineIdentifier) {
        this.pipelineIdentifier = pipelineIdentifier;
    }

    @Override
    public String getRevisionString() {
        return pipelineIdentifier.pipelineLocator();
    }

    public String getPipelineName() {
        return pipelineIdentifier.getName();
    }

    public Integer getCounter() {
        return pipelineIdentifier.getCounter();
    }

    public String getLabel() {
        return pipelineIdentifier.getLabel();
    }

    public PipelineIdentifier getPipelineIdentifier() {
        return pipelineIdentifier;
    }

    public Stages getStages() {
        return stages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineRevision that = (PipelineRevision) o;

        if (pipelineIdentifier != null ? !pipelineIdentifier.equals(that.pipelineIdentifier) : that.pipelineIdentifier != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return pipelineIdentifier != null ? pipelineIdentifier.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("PipelineRevision{stages=%s, pipelineIdentifier=%s}", stages, pipelineIdentifier);
    }

    public void addStage(Stage stage) {
        stages.add(stage);
    }

    public void addStages(Stages stages) {
        this.stages.addAll(stages);
    }

    public int compareTo(PipelineRevision other) {
        return other.getCounter().compareTo(this.getCounter());
    }
}
