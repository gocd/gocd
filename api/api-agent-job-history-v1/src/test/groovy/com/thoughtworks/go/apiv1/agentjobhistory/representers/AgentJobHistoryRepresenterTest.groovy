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
package com.thoughtworks.go.apiv1.agentjobhistory.representers

import com.thoughtworks.go.api.representers.PaginationRepresenter
import com.thoughtworks.go.domain.JobInstances
import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.helper.JobInstanceMother
import com.thoughtworks.go.server.ui.JobInstancesModel
import com.thoughtworks.go.server.util.Pagination
import com.thoughtworks.go.spark.Routes
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class AgentJobHistoryRepresenterTest {
  @Test
  void 'should serialize'() {
    def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)
    def pagination = Pagination.pageStartingAt(1, 42, 10)
    def jobInstancesModel = new JobInstancesModel(new JobInstances(jobInstance), pagination)

    def actualJson = toObjectString({
      AgentJobHistoryRepresenter.toJSON(it, "some-uuid", jobInstancesModel)
    })

    def expectedJSON = [
      _links    : [
        "doc" : ["href": Routes.AgentJobHistory.DOC],
        "self": ["href": "http://test.host/go/api/agents/some-uuid/job_run_history"],
        "find": ["href": "http://test.host/go/api/agents/:uuid"],
      ],
      uuid      : 'some-uuid',
      jobs      : [
        [
          job_name             : "blah",
          stage_name           : "stage",
          stage_counter        : "1",
          pipeline_name        : "pipeline",
          pipeline_counter     : 1,
          result               : "Passed",
          rerun                : false,
          job_state_transitions: [
            [
              state_change_time: jsonDate(jobInstance.getStartedDateFor(JobState.Scheduled)),
              state            : "Scheduled"
            ],
            [
              state_change_time: jsonDate(jobInstance.getStartedDateFor(JobState.Assigned)),
              state            : "Assigned"
            ],
            [
              state_change_time: jsonDate(jobInstance.getStartedDateFor(JobState.Preparing)),
              state            : "Preparing"
            ],
            [
              state_change_time: jsonDate(jobInstance.getStartedDateFor(JobState.Building)),
              state            : "Building"
            ],
            [
              state_change_time: jsonDate(jobInstance.getStartedDateFor(JobState.Completing)),
              state            : "Completing"
            ],
            [
              state_change_time: jsonDate(jobInstance.getStartedDateFor(JobState.Completed)),
              state            : "Completed"
            ]
          ]
        ]
      ],
      pagination: toObject({ PaginationRepresenter.toJSON(it, pagination) })
    ]

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
