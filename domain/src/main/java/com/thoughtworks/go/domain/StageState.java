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
package com.thoughtworks.go.domain;

import java.util.List;

public enum StageState {
    Building(StageResult.Unknown),
    Failing(StageResult.Failed),
    Passed(StageResult.Passed),
    Failed(StageResult.Failed),
    Unknown(StageResult.Unknown),
    Cancelled(StageResult.Cancelled);

    private StageResult stageResult;

    StageState(StageResult stageResult) {
        this.stageResult = stageResult;
    }

    public boolean completed() {
        return this == Passed || this == Failed || this == Cancelled;
    }

    public boolean isActive() {
        return !completed() && this != Unknown;
    }

    public static StageState findByBuilds(List<? extends BuildStateAware> builds) {
        boolean anyBuilding = false;
        boolean anyFailed = false;
        boolean anyCancelled = false;
        boolean anyUnknown = false;

        if (builds.isEmpty()) {
            return Unknown;
        }

        for (BuildStateAware build : builds) {
            if (build.getState().isBuilding() || build.getState().isAssignedOrScheduled()) {
                anyBuilding = true;
            }
            if (build.getResult() == JobResult.Cancelled) {
                anyCancelled = true;
            }
            if (build.getResult() == JobResult.Failed) {
                anyFailed = true;
            }
            if (build.getResult() == JobResult.Unknown) {
                anyUnknown = true;
            }
        }
        if (anyBuilding && anyFailed) {
            return Failing;
        }
        if (anyBuilding) {
            return Building;
        }
        if (anyCancelled) {
            return Cancelled;
        }
        if (anyFailed) {
            return Failed;
        }

        if (anyUnknown) {
            return Unknown;
        }
        return Passed;

    }

    public StageResult stageResult() {
        return stageResult;
    }


    public String cctrayStatus() {
        switch (this) {
            case Failed:
            case Failing:
            case Cancelled:
                return "Failure";
            default:
                return "Success";
        }

    }

    public String cctrayActivity() {
        switch (this) {
            case Building:
            case Failing:
                return "Building";
            default:
                return "Sleeping";
        }
    }

    public String status() {
        return completed()? "Completed" : toString(); 
    }
}

