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
package com.thoughtworks.go.apiv2.stageoperations.representers

import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import com.thoughtworks.go.server.util.Pagination
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class StageInstancesRepresenterTest {
  @Test
  void 'should represent stage history with hal'() {

    def jobHistoryItem = new JobHistoryItem("job", JobState.Completed, JobResult.Passed, new Date(12345))
    jobHistoryItem.setId(34)
    def jobHistory = new JobHistory()
    jobHistory.add(jobHistoryItem)
    def stageInstanceModel = new StageInstanceModel("stage", "3", jobHistory)
    stageInstanceModel.setId(21)
    def stageInstances = new StageInstanceModels()
    stageInstances.add(stageInstanceModel)
    def pagination = new Pagination(1, 20, 10)

    def actualJson = toObjectString({StageInstancesRepresenter.toJSON(it, stageInstances, pagination) })

    assertThatJson(actualJson).isEqualTo(stageHash)
  }

  def stageHash = [
    stages: [
      [
        name:'stage',
        counter:'3',
        approval_type:null,
        approved_by:null,
        rerun_of_counter:null,
        jobs:[
          [
            name:'job',
            state:'Completed',
            result:'Passed',
            scheduled_date:12345
          ]
        ]
      ]
    ],
    pagination : [
      offset: 1,
      page_size: 10,
      total: 20
    ]
  ]
}
