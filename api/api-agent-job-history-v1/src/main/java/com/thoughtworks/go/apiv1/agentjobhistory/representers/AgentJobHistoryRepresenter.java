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
package com.thoughtworks.go.apiv1.agentjobhistory.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.PaginationRepresenter;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.server.ui.JobInstancesModel;
import com.thoughtworks.go.spark.Routes;

public class AgentJobHistoryRepresenter {
    public static void toJSON(OutputWriter outputWriter, String uuid, JobInstancesModel jobInstances) {
        outputWriter
                .addLinks(outputLinkWriter -> {
                    outputLinkWriter
                            .addAbsoluteLink("doc", Routes.AgentJobHistory.DOC)
                            .addLink("self", Routes.AgentJobHistory.forAgent(uuid))
                            .addLink("find", Routes.AgentsAPI.find())
                    ;
                })
                .add("uuid", uuid)
                .addChildList("jobs", jobsOutputWriter -> {
                    jobInstances.forEach(jobInstance -> {
                        jobsOutputWriter.addChild(jobOutputWriter -> toJSON(jobInstance, jobOutputWriter));
                    });
                })
                .addChild("pagination", PaginationRepresenter.toJSON(jobInstances.getPagination()));
    }

    private static void toJSON(JobInstance jobInstance, OutputWriter jobOutputWriter) {
        jobOutputWriter
                .addChildList("job_state_transitions", outputListWriter -> {
                    jobInstance.getTransitions().forEach(jobStateTransition -> {
                        outputListWriter.addChild(jobStateTransitionWriter -> {
                            toJSON(jobStateTransition, jobStateTransitionWriter);
                        });
                    });
                })
                .add("job_name", jobInstance.getName())
                .add("stage_name", jobInstance.getStageName())
                .add("stage_counter", jobInstance.getStageCounter())
                .add("pipeline_name", jobInstance.getPipelineName())
                .add("pipeline_counter", jobInstance.getPipelineCounter())

                .add("result", jobInstance.getResult())
                .add("rerun", jobInstance.isRerun());
    }

    private static void toJSON(JobStateTransition jobStateTransition, OutputWriter jobStateTransitionWriter) {
        jobStateTransitionWriter
                .add("state_change_time", jobStateTransition.getStateChangeTime())
                .add("state", jobStateTransition.getCurrentState());
    }
}

/*
//xxx      "agent_uuid": "5c5c318f-e6d3-4299-9120-7faff6e6030b",
      "job_state_transitions": [
        {
          "state_change_time": 1435631497131,
          xxxx"id": 539906,
          "state": "Scheduled"
        },
        ...
      ],
      xxx"original_job_id": null,
      xxx"id": 100129,

//      "job_name": "upload",
//      "stage_name": "upload-installers"
//      "stage_counter": "1",
//      "pipeline_name": "distributions-all",
//      "pipeline_counter": 251,

//      "scheduled_at": 1435631497131,

//      "result": "Passed",

//xxx      "state": "Completed",
//      "rerun": false,
    }
    * */
