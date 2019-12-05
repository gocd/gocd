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

package com.thoughtworks.go.apiv1.jobinstance.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.JobInstance;

import static java.util.Collections.emptyList;

public class JobInstanceRepresenter {
    public static void toJSON(OutputWriter outputWriter, JobInstance jobInstance) {
        outputWriter.add("name", jobInstance.getName());
        if (jobInstance.getState() != null) {
            outputWriter.add("state", jobInstance.getState().toString());
        }
        if (jobInstance.getResult() != null) {
            outputWriter.add("result", jobInstance.getResult().toString());
        }
        if (jobInstance.isNull()) {
            renderNullJobInstance(outputWriter);
            return;
        }
        if (jobInstance.getOriginalJobId() == null) {
            outputWriter.renderNull("original_job_id");
        } else {
            outputWriter.add("original_job_id", jobInstance.getOriginalJobId());
        }
        outputWriter.addIfNotNull("scheduled_date", jobInstance.getScheduledDate())
                .add("rerun", jobInstance.isRerun())
                .add("agent_uuid", jobInstance.getAgentUuid())
                .add("pipeline_name", jobInstance.getPipelineName())
                .add("pipeline_counter", jobInstance.getPipelineCounter())
                .add("stage_name", jobInstance.getStageName())
                .add("stage_counter", jobInstance.getStageCounter())
                .addChildList("job_state_transitions", outputListWriter -> jobInstance.getTransitions().forEach(jobStateTransition -> outputListWriter.addChild(itemWriter -> JobStateTransitionRepresenter.toJSON(itemWriter, jobStateTransition))));
    }

    private static void renderNullJobInstance(OutputWriter outputWriter) {
        outputWriter.renderNull("original_job_id");
        outputWriter.renderNull("scheduled_date");
        outputWriter.renderNull("rerun");
        outputWriter.renderNull("agent_uuid");
        outputWriter.renderNull("pipeline_name");
        outputWriter.renderNull("pipeline_counter");
        outputWriter.renderNull("stage_name");
        outputWriter.renderNull("stage_counter");
        outputWriter.addChildList("job_state_transitions", emptyList());
    }
}
