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
package com.thoughtworks.go.apiv2.stageoperations.representers

import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class JobHistoryItemRepresenterTest {

  @Test
  void 'should represent pagination with hal'() {
    def jobHistoryItem = new JobHistoryItem("job", JobState.Completed, JobResult.Passed, new Date(12345))
    jobHistoryItem.setId(34)

    def actualJson = toObjectString({JobHistoryItemRepresenter.toJSON(it, jobHistoryItem) })

    assertThatJson(actualJson).isEqualTo(jobHash)
  }

  def jobHash = [
    name: 'job',
    state: 'Completed',
    result: 'Passed',
    scheduled_date: 12345
  ]
}
