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
package com.thoughtworks.go.apiv3.stageinstance.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.JobInstance;

public class JobInstanceRepresenter {
    public static void toJSON(OutputWriter jsonWriter, JobInstance jobInstance) {
        jsonWriter.add("name", jobInstance.getName());
        if (jobInstance.getState() != null) {
            jsonWriter.add("state", jobInstance.getState().toString());
        }
        if (jobInstance.getResult() != null) {
            jsonWriter.add("result", jobInstance.getResult().toString());
        }
        if (jobInstance.getScheduledDate() != null) {
            jsonWriter.add("scheduled_date", jobInstance.getScheduledDate().getTime());
        }
        jsonWriter.add("rerun", jobInstance.isRerun());
        if (jobInstance.getOriginalJobId() == null) {
            jsonWriter.add("original_job_id", (String) null);
        }
        else {
            jsonWriter.add("original_job_id", jobInstance.getOriginalJobId());
        }
        jsonWriter.addWithDefaultIfBlank("agent_uuid", jobInstance.getAgentUuid(), (String) null);
        jsonWriter.add("pipeline_name", (String) null);
        jsonWriter.add("pipeline_counter", (String) null);
        jsonWriter.add("stage_name", (String) null);
        jsonWriter.add("stage_counter", (String) null);
        jsonWriter.addChildList("job_state_transitions", jobStateTransitionsWriter -> jobInstance.getTransitions().forEach(
                jobStateTransition -> jobStateTransitionsWriter.addChild(
                        jobStateTransitionWriter -> JobStateTransitionRepresenter.toJSON(jobStateTransitionWriter, jobStateTransition))));

    }
}
