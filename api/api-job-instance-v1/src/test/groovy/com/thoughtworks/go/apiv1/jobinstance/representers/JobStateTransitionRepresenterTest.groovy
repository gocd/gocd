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

import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.domain.JobStateTransition
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class JobStateTransitionRepresenterTest {
  @Test
  void 'should serialize into json'() {
    def date = new Date()
    def jobStateTransition = new JobStateTransition(JobState.Completed, date)

    def actualJson = toObjectString({ JobStateTransitionRepresenter.toJSON(it, jobStateTransition) })
    def expectedJson = [
      "state"            : "Completed",
      "state_change_time": jsonDate(date)
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not render state change time if null'() {
    def jobStateTransition = new JobStateTransition(JobState.Completed, null)

    def actualJson = toObjectString({ JobStateTransitionRepresenter.toJSON(it, jobStateTransition) })
    def expectedJson = [
      "state": "Completed"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not render state if null'() {
    def date = new Date()
    def jobStateTransition = new JobStateTransition(null, date)

    def actualJson = toObjectString({ JobStateTransitionRepresenter.toJSON(it, jobStateTransition) })
    def expectedJson = [
      "state_change_time": jsonDate(date)
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
