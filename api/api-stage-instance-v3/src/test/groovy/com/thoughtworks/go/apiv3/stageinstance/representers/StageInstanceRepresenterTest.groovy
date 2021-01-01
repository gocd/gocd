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
package com.thoughtworks.go.apiv3.stageinstance.representers

import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class StageInstanceRepresenterTest {
  @Test
  void 'should represent stage instance with hal'() {
    def jobHistoryItem = new JobHistoryItem("job", JobState.Completed, JobResult.Passed, new Date(12345))
    jobHistoryItem.setId(34)
    def jobHistory = new JobHistory()
    jobHistory.add(jobHistoryItem)
    def stageInstanceModel = new StageInstanceModel("stage", "3", jobHistory)
    stageInstanceModel.setId(21)
    def actualJson = toObjectString({StageInstanceRepresenter.toJSON(it, stageInstanceModel) })

    assertThatJson(actualJson).isEqualTo(stageHash)
  }

  def stageHash = [
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
}
