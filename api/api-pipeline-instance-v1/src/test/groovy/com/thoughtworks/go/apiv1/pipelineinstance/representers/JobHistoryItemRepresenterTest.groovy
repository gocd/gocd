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
package com.thoughtworks.go.apiv1.pipelineinstance.representers

import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class JobHistoryItemRepresenterTest {
  @Test
  void 'should deserialize into json'() {
    Date date = new Date();
    def jobHistoryItem = new JobHistoryItem("jobName", JobState.Completed, JobResult.Passed, date)
    jobHistoryItem.setId(5)

    def actualJson = toObjectString({ JobHistoryItemRepresenter.toJSON(it, jobHistoryItem) })

    def expectedJson = [
      "name"          : "jobName",
      "scheduled_date": date.getTime(),
      "state"         : "Completed",
      "result"        : "Passed"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not add scheduled_date if set to null'() {
    def jobHistoryItem = new JobHistoryItem("jobName", JobState.Completed, JobResult.Passed, new Date())
    jobHistoryItem.setId(5)
    jobHistoryItem.setScheduledDate(null)

    def actualJson = toObjectString({ JobHistoryItemRepresenter.toJSON(it, jobHistoryItem) })

    def expectedJson = [
      "name"  : "jobName",
      "state" : "Completed",
      "result": "Passed"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not set state if set to null'() {
    Date date = new Date()
    def jobHistoryItem = new JobHistoryItem("jobName", null, JobResult.Passed, date)
    jobHistoryItem.setId(5)

    def actualJson = toObjectString({ JobHistoryItemRepresenter.toJSON(it, jobHistoryItem) })

    def expectedJson = [
      "name"          : "jobName",
      "scheduled_date": date.getTime(),
      "result"        : "Passed"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }


  @Test
  void 'should not set result if set to null'() {
    Date date = new Date()
    def jobHistoryItem = new JobHistoryItem("jobName", JobState.Completed, null, date)
    jobHistoryItem.setId(5)

    def actualJson = toObjectString({ JobHistoryItemRepresenter.toJSON(it, jobHistoryItem) })

    def expectedJson = [
      "name"          : "jobName",
      "scheduled_date": date.getTime(),
      "state"         : "Completed"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
