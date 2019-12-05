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

import com.thoughtworks.go.domain.NullJobInstance
import com.thoughtworks.go.helper.JobInstanceMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class JobInstanceRepresenterTest {
  @Test
  void 'should serialize into json'() {
    def jobInstance = JobInstanceMother.building('job_name', new Date())

    def actualJson = toObjectString({ JobInstanceRepresenter.toJSON(it, jobInstance) })
    def expectedJson = [
      "name"                 : "job_name",
      "state"                : "Building",
      "result"               : "Unknown",
      "original_job_id"      : null,
      "scheduled_date"       : jsonDate(jobInstance.scheduledDate),
      "rerun"                : false,
      "agent_uuid"           : null,
      "pipeline_name"        : "pipeline",
      "pipeline_counter"     : 1,
      "stage_name"           : "stage",
      "stage_counter"        : "1",
      "job_state_transitions": jobInstance.getTransitions().collect { eachItem ->
        toObject({
          JobStateTransitionRepresenter.toJSON(it, eachItem)
        })
      }
    ]
    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not render state if null'() {
    def jobInstance = JobInstanceMother.building('job_name', new Date())
    jobInstance.state = null

    def actualJson = toObjectString({ JobInstanceRepresenter.toJSON(it, jobInstance) })
    def expectedJson = [
      "name"                 : "job_name",
      "result"               : "Unknown",
      "original_job_id"      : null,
      "scheduled_date"       : jsonDate(jobInstance.scheduledDate),
      "rerun"                : false,
      "agent_uuid"           : null,
      "pipeline_name"        : "pipeline",
      "pipeline_counter"     : 1,
      "stage_name"           : "stage",
      "stage_counter"        : "1",
      "job_state_transitions": jobInstance.getTransitions().collect { eachItem ->
        toObject({
          JobStateTransitionRepresenter.toJSON(it, eachItem)
        })
      }
    ]
    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not render result if null'() {
    def jobInstance = JobInstanceMother.building('job_name', new Date())
    jobInstance.result = null

    def actualJson = toObjectString({ JobInstanceRepresenter.toJSON(it, jobInstance) })
    def expectedJson = [
      "name"                 : "job_name",
      "state"                : "Building",
      "original_job_id"      : null,
      "scheduled_date"       : jsonDate(jobInstance.scheduledDate),
      "rerun"                : false,
      "agent_uuid"           : null,
      "pipeline_name"        : "pipeline",
      "pipeline_counter"     : 1,
      "stage_name"           : "stage",
      "stage_counter"        : "1",
      "job_state_transitions": jobInstance.getTransitions().collect { eachItem ->
        toObject({
          JobStateTransitionRepresenter.toJSON(it, eachItem)
        })
      }
    ]
    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render original job id if not null'() {
    def jobInstance = JobInstanceMother.building('job_name', new Date())
    jobInstance.originalJobId = 3

    def actualJson = toObjectString({ JobInstanceRepresenter.toJSON(it, jobInstance) })
    def expectedJson = [
      "name"                 : "job_name",
      "state"                : "Building",
      "result"               : "Unknown",
      "original_job_id"      : 3,
      "scheduled_date"       : jsonDate(jobInstance.scheduledDate),
      "rerun"                : false,
      "agent_uuid"           : null,
      "pipeline_name"        : "pipeline",
      "pipeline_counter"     : 1,
      "stage_name"           : "stage",
      "stage_counter"        : "1",
      "job_state_transitions": jobInstance.getTransitions().collect { eachItem ->
        toObject({
          JobStateTransitionRepresenter.toJSON(it, eachItem)
        })
      }
    ]
    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
