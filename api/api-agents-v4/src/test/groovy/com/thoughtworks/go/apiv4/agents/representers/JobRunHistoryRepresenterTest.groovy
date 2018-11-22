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

package com.thoughtworks.go.apiv4.agents.representers

import com.thoughtworks.go.domain.JobInstance
import com.thoughtworks.go.domain.JobInstances
import com.thoughtworks.go.helper.JobInstanceMother
import com.thoughtworks.go.server.ui.JobInstancesModel
import com.thoughtworks.go.server.util.Pagination
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.domain.JobState.*
import static com.thoughtworks.go.server.util.Pagination.pageStartingAt
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class JobRunHistoryRepresenterTest {

  @Test
  void 'should convert job instance model to job run history json'() {
    JobInstance jobInstance = JobInstanceMother.building("some-config")
    JobInstances jobInstances = new JobInstances(jobInstance)
    Pagination pagination = pageStartingAt(10, 100, 10)

    def json = toObjectString({ writer -> JobRunHistoryRepresenter.toJSON(writer, new JobInstancesModel(jobInstances, pagination)) })

    assertThatJson(json).isEqualTo([
      "jobs"      : [
        [
          "id"                   : -1,
          "name"                 : "some-config",
          "rerun"                : false,
          "agent_uuid"           : "1234",
          "pipeline_name"        : "pipeline",
          "pipeline_counter"     : 1,
          "stage_name"           : "stage",
          "stage_counter"        : "1",
          "job_state_transitions": [
            [
              "id"               : -1,
              "state_change_time": jobInstance.getTransition(Scheduled).getStateChangeTime().getTime(),
              "state"            : "Scheduled"
            ],
            [
              "id"               : -1,
              "state_change_time": jobInstance.getTransition(Assigned).getStateChangeTime().getTime(),
              "state"            : "Assigned"
            ],
            [
              "id"               : -1,
              "state_change_time": jobInstance.getTransition(Preparing).getStateChangeTime().getTime(),
              "state"            : "Preparing"
            ],
            [
              "id"               : -1,
              "state_change_time": jobInstance.getTransition(Building).getStateChangeTime().getTime(),
              "state"            : "Building"
            ]
          ],
          "original_job_id"      : null,
          "state"                : "Building",
          "result"               : "Unknown",
          "scheduled_date"       : jobInstance.getScheduledDate().getTime()
        ]
      ],
      "pagination": [
        "offset"   : 10,
        "total"    : 100,
        "page_size": 10
      ]
    ])
  }
}
