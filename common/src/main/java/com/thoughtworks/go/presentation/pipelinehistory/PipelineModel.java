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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.domain.PipelinePauseInfo;

public class PipelineModel {

    private final String pipelineName;
    private final boolean canForce;
    private final boolean canOperate;
    private boolean canAdminister = false;
    private final PipelinePauseInfo pipelinePauseInfo;
    private final PipelineInstanceModels activePipelineInstances;


    public PipelineModel(String pipelineName, boolean canForce, boolean canOperate, PipelinePauseInfo pipelinePauseInfo) {
        this.pipelineName = pipelineName;
        this.canForce = canForce;
        this.pipelinePauseInfo = pipelinePauseInfo;
        this.canOperate = canOperate;
        activePipelineInstances = PipelineInstanceModels.createPipelineInstanceModels();
    }

    public PipelineModel(PipelineModel other, boolean copyInstances) {
        this(other.pipelineName, other.canForce, other.canOperate, other.pipelinePauseInfo);
        this.canAdminister = other.canAdminister;

        if (copyInstances) {
            this.activePipelineInstances.addAll(other.activePipelineInstances);
        }
    }

    public void addPipelineInstance(PipelineInstanceModel pipelineInstanceModel) {
        activePipelineInstances.add(pipelineInstanceModel);
    }

    public PipelineModel addPipelineInstances(PipelineInstanceModels instanceModels) {
        for (PipelineInstanceModel instanceModel : instanceModels) {
            addPipelineInstance(instanceModel);
        }
        return this;
    }

    public String getName() {
        return pipelineName;
    }

    public boolean hasNewRevisions() {
        return getLatestPipelineInstance().hasNewRevisions();
    }

    public boolean hasNeverCheckedForRevisions() {
        return getLatestPipelineInstance().hasNeverCheckedForRevisions();
    }

    /**
     * Note: this check should be part of the scheduling checker.
     * We will refactor it down to that point.
     */
    public boolean canForce() {
        return canForce && !getLatestPipelineInstance().isPreparingToSchedule();
    }

    public PipelineInstanceModel getLatestPipelineInstance() {
        return activePipelineInstances.first();
    }

    public PipelineInstanceModels getActivePipelineInstances() {
        return activePipelineInstances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineModel that = (PipelineModel) o;

        if (canForce != that.canForce) {
            return false;
        }
        if (!activePipelineInstances.equals(that.activePipelineInstances)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (canForce ? 1 : 0);
        result = 31 * result + (activePipelineInstances.hashCode());
        return result;
    }

    @Override public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "canForce='" + canForce + '\'' +
                ", activePipelineInstances='" + activePipelineInstances + '\'' +
                '}';
    }

    public PipelinePauseInfo getPausedInfo() {
        return pipelinePauseInfo;
    }

    public boolean canOperate() {
        return canOperate;
    }

    public boolean canAdminister() {
        return canAdminister;
    }

    public PipelineModel updateAdministrability(boolean value) {
        canAdminister = value;
        return this;
    }
}