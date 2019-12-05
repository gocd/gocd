/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.pipelineoperations.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.presentation.PipelineStatusModel;

public class PipelineStatusModelRepresenter {
    public static void toJSON(OutputWriter outputWriter, PipelineStatusModel pipelineStatus) {
        outputWriter.add("paused", pipelineStatus.isPaused())
                .add("paused_cause", pipelineStatus.pausedCause())
                .add("paused_by", pipelineStatus.pausedBy())
                .add("locked", pipelineStatus.isLocked())
                .add("schedulable", pipelineStatus.isSchedulable());
    }
}
