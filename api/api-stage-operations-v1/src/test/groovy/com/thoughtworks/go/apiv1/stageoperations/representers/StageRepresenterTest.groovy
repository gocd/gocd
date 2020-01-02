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
package com.thoughtworks.go.apiv1.stageoperations.representers

import com.thoughtworks.go.domain.JobInstance
import com.thoughtworks.go.domain.JobInstances
import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.domain.JobStateTransition
import com.thoughtworks.go.domain.JobStateTransitions
import com.thoughtworks.go.domain.Stage
import com.thoughtworks.go.domain.StageIdentifier
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class StageRepresenterTest {
  @Test
  void 'should represent a stage model'() {
    def stageModel = new Stage()
    stageModel.setId(456)
    stageModel.setName('stage name')
    stageModel.setCounter(1)
    stageModel.setApprovalType('manual')
    stageModel.setApprovedBy('me')
    stageModel.setRerunOfCounter(1)
    stageModel.setIdentifier(new StageIdentifier('pipeline name', 213, 'stage name', '4'))
    stageModel.setJobInstances(new JobInstances(getJobInstance()))

    def actualJson = toObjectString({StageRepresenter.toJSON(it, stageModel) })
    assertThatJson(actualJson).isEqualTo(stageHash)
  }

  def getJobInstance() {
    def jobInstance = new JobInstance("job")
    jobInstance.setId(1);
    jobInstance.setState(JobState.Assigned)
    jobInstance.setResult(JobResult.Unknown)
    jobInstance.setAgentUuid("uuid")
    jobInstance.setScheduledDate(new Date(12345))
    jobInstance.setOriginalJobId(1)
    jobInstance.setTransitions(new JobStateTransitions(new JobStateTransition(JobState.Scheduled, new Date(12345)),
      new JobStateTransition(JobState.Assigned, null)))

    return jobInstance
  }

  def stageHash = [
    id: 456,
    name: 'stage name',
    counter: 1,
    approval_type: 'manual',
    approved_by: 'me',
    result: 'Unknown',
    rerun_of_counter: 1,
    fetch_materials: true,
    clean_working_directory: false,
    artifacts_deleted: false,
    pipeline_name: 'pipeline name',
    pipeline_counter: 213,
    jobs: [
      [
        id: 1,
        name: 'job',
        state: 'Assigned',
        result: 'Unknown',
        scheduled_date: 12345,
        rerun: false,
        original_job_id: 1,
        agent_uuid: 'uuid',
        pipeline_name: null,
        pipeline_counter: null,
        stage_name: null,
        stage_counter: null,
        job_state_transitions: [
          [
            id: -1,
            state: 'Scheduled',
            state_change_time: 12345
          ],
          [
            id: -1,
            state: 'Assigned'
          ]
        ]
      ]
    ]
  ]
}
