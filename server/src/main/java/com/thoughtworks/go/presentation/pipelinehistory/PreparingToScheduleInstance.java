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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.domain.buildcause.BuildCause;

import java.util.Date;

/**
 * Understands a pipeline that is preparing to schedule
 */
public class PreparingToScheduleInstance extends PipelineInstanceModel {

    PreparingToScheduleInstance(String pipelineName, StageInstanceModels stageHistory) {
        super(pipelineName, -1, "TBD", new PreparingToScheduleBuildCause(), stageHistory);
        canRun = false;
        isPreparingToSchedule = true;
    }

    @Override
    public String getBuildCauseMessage() {
        return "Preparing to schedule";
    }

    @Override
    public Date getScheduledDate() {
        return new Date();
    }

    public static class PreparingToScheduleBuildCause extends BuildCause {}

}
