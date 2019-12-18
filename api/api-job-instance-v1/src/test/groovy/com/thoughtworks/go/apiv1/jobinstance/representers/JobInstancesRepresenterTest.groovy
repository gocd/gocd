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

package com.thoughtworks.go.apiv1.jobinstance.representers


import com.thoughtworks.go.domain.JobInstances
import com.thoughtworks.go.domain.PipelineRunIdInfo
import com.thoughtworks.go.helper.JobInstanceMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class JobInstancesRepresenterTest {
  @Test
  void 'should convert job instance to json'() {
    def jobInstances = getJobInstances()
    def runIdInfo = new PipelineRunIdInfo(50, 10)

    def actualJson = toObjectString({ JobInstancesRepresenter.toJSON(it, jobInstances, runIdInfo) })

    def expectedJson = [
      "_links": [
        "previous": [
          "href": "http://test.host/go/api/jobs/pipeline/stage/job_name/history?before=2"
        ],
        "next"    : [
          "href": "http://test.host/go/api/jobs/pipeline/stage/job_name/history?after=2"
        ]
      ],
      "jobs"  : [
        [
          "name"                 : "job_name",
          "state"                : "Building",
          "result"               : "Unknown",
          "original_job_id"      : null,
          "scheduled_date"       : jobInstances.get(0).getScheduledDate().getTime() + "",
          "rerun"                : false,
          "agent_uuid"           : "1234",
          "pipeline_name"        : "pipeline",
          "pipeline_counter"     : 1,
          "stage_name"           : "stage",
          "stage_counter"        : "1",
          "job_state_transitions": jobInstances.get(0).getTransitions().collect { state ->
            toObject({
              JobStateTransitionRepresenter.toJSON(it, state)
            })
          }
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should return empty json list if the job instances is empty'() {
    def jobInstances = getJobInstances(0)
    def runIdInfo = new PipelineRunIdInfo(50, 10)

    def actualJson = toObjectString({ JobInstancesRepresenter.toJSON(it, jobInstances, runIdInfo) })

    def expectedJson = [
      "jobs": []
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not add previous link if there are no newer records'() {
    def jobInstances = getJobInstances(1)
    def runIdInfo = new PipelineRunIdInfo(2, 1)

    def actualJson = toObjectString({ JobInstancesRepresenter.toJSON(it, jobInstances, runIdInfo) })

    def expectedJson = [
      "_links": [
        "next": [
          "href": "http://test.host/go/api/jobs/pipeline/stage/job_name/history?after=2"
        ]
      ],
      "jobs"  : [
        [
          "name"                 : "job_name",
          "state"                : "Building",
          "result"               : "Unknown",
          "original_job_id"      : null,
          "scheduled_date"       : jobInstances.get(0).getScheduledDate().getTime() + "",
          "rerun"                : false,
          "agent_uuid"           : "1234",
          "pipeline_name"        : "pipeline",
          "pipeline_counter"     : 1,
          "stage_name"           : "stage",
          "stage_counter"        : "1",
          "job_state_transitions": jobInstances.get(0).getTransitions().collect { state ->
            toObject({
              JobStateTransitionRepresenter.toJSON(it, state)
            })
          }
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not add next link if there are no older records'() {
    def jobInstances = getJobInstances(1)
    def runIdInfo = new PipelineRunIdInfo(3, 2)

    def actualJson = toObjectString({ JobInstancesRepresenter.toJSON(it, jobInstances, runIdInfo) })

    def expectedJson = [
      "_links": [
        "previous": [
          "href": "http://test.host/go/api/jobs/pipeline/stage/job_name/history?before=2"
        ]
      ],
      "jobs"  : [
        [
          "name"                 : "job_name",
          "state"                : "Building",
          "result"               : "Unknown",
          "original_job_id"      : null,
          "scheduled_date"       : jobInstances.get(0).getScheduledDate().getTime() + "",
          "rerun"                : false,
          "agent_uuid"           : "1234",
          "pipeline_name"        : "pipeline",
          "pipeline_counter"     : 1,
          "stage_name"           : "stage",
          "stage_counter"        : "1",
          "job_state_transitions": jobInstances.get(0).getTransitions().collect { state ->
            toObject({
              JobStateTransitionRepresenter.toJSON(it, state)
            })
          }
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not links if there is only one page'() {
    def jobInstances = getJobInstances(1)
    def runIdInfo = new PipelineRunIdInfo(2, 2)

    def actualJson = toObjectString({ JobInstancesRepresenter.toJSON(it, jobInstances, runIdInfo) })

    def expectedJson = [
      "jobs": [
        [
          "name"                 : "job_name",
          "state"                : "Building",
          "result"               : "Unknown",
          "original_job_id"      : null,
          "scheduled_date"       : jobInstances.get(0).getScheduledDate().getTime() + "",
          "rerun"                : false,
          "agent_uuid"           : "1234",
          "pipeline_name"        : "pipeline",
          "pipeline_counter"     : 1,
          "stage_name"           : "stage",
          "stage_counter"        : "1",
          "job_state_transitions": jobInstances.get(0).getTransitions().collect { state ->
            toObject({
              JobStateTransitionRepresenter.toJSON(it, state)
            })
          }
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  def getJobInstances(int count = 1) {
    def jobInstances = new JobInstances()
    for (int i = 0; i < count; i++) {
      def jobInstance = JobInstanceMother.building('job_name')
      jobInstance.setId(i + 2)
      jobInstances.add(jobInstance)
    }
    return jobInstances
  }
}
