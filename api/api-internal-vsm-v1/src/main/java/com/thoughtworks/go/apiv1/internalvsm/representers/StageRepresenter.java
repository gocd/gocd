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

package com.thoughtworks.go.apiv1.internalvsm.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.domain.RunDuration;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.spark.Routes;

public class StageRepresenter {

    public static void toJSON(OutputListWriter listWriter, Stages stages, String pipelineName, Integer pipelineCounter) {
        if (stages == null) {
            return;
        }
        stages.forEach((stage) -> listWriter.addChild(writer -> {
            writer.add("name", stage.getName())
                    .add("status", stage.getState().toString());
            if (stage.isCompleted()) {
                writer.add("duration", ((RunDuration.ActualDuration) stage.getDuration()).getTotalSeconds());
            } else {
                writer.renderNull("duration");
            }
            if (stage.getState() != StageState.Unknown) {
                writer.add("locator", Routes.InternalVsm.pipelineStageLocator(pipelineName, pipelineCounter, stage.getName(), stage.getCounter()));
            }
        }));
    }
}
