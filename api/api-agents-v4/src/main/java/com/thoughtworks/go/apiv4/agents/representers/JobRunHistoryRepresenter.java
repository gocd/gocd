/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv4.agents.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.server.ui.JobInstancesModel;
import com.thoughtworks.go.server.util.Pagination;

public class JobRunHistoryRepresenter {
    public static void toJSON(OutputWriter outputWriter, JobInstancesModel jobInstances) {
        outputWriter.addChildList("jobs", jobWriter -> jobInstancesModelToJSON(jobWriter, jobInstances));
        outputWriter.addChild("pagination", paginationWriter -> paginationToJSON(paginationWriter, jobInstances.getPagination()));
    }

    private static void paginationToJSON(OutputWriter paginationWriter, Pagination pagination) {
        paginationWriter
                .add("page_size", pagination.getPageSize())
                .add("offset", pagination.getOffset())
                .add("total", pagination.getTotal());
    }

    private static void jobInstancesModelToJSON(OutputListWriter outputWriter, JobInstancesModel jobInstances) {
        jobInstances.getJobInstances()
                .forEach(instance -> outputWriter.addChild(writer -> jobInstanceToJSON(writer, instance)));

    }

    private static void jobInstanceToJSON(OutputWriter writer, JobInstance jobInstance) {
        writer
                .add("id", jobInstance.getId())
                .add("name", jobInstance.getName())
                .add("rerun", jobInstance.isRerun())
                .add("agent_uuid", jobInstance.getAgentUuid())
                .add("pipeline_name", jobInstance.getPipelineName())
                .add("pipeline_counter", jobInstance.getPipelineCounter())
                .add("stage_name", jobInstance.getStageName())
                .add("stage_counter", jobInstance.getStageCounter())
                .addChildList("job_state_transitions", jobTransitionsWriter -> jobInstance.getTransitions()
                        .forEach(instance -> jobTransitionsWriter.addChild(jobTransitionWriter -> jobStateTransitionToJSON(writer, instance))));

        if (jobInstance.getOriginalJobId() == null) {
            writer.renderNull("original_job_id");
        } else {
            writer.add("original_job_id", jobInstance.getOriginalJobId());
        }

        if (jobInstance.getState() != null) {
            writer.add("state", jobInstance.getState().toString());
        }

        if (jobInstance.getResult() != null) {
            writer.add("result", jobInstance.getResult().toString());
        }

        if (jobInstance.getScheduledDate() != null) {
            writer.add("scheduled_date", jobInstance.getScheduledDate().getTime());
        }

    }

    private static void jobStateTransitionToJSON(OutputWriter writer, JobStateTransition jobStateTransition) {
        writer.add("id", jobStateTransition.getId());

        if (jobStateTransition.getStateChangeTime() != null) {
            writer.add("state_change_time", jobStateTransition.getStateChangeTime().getTime());

        }

        if (jobStateTransition.getCurrentState() != null) {
            writer.add("state", jobStateTransition.getCurrentState().toString());
        }
    }
}
