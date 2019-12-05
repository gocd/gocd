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
package com.thoughtworks.go.apiv2.stageoperations.representers

import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.domain.JobStateTransition
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class JobStateTransitionRepresenterTest {

  @Test
  void 'should represent a job state transition'() {
    def transition = new JobStateTransition(JobState.Building, new Date(12345))
    transition.setId(21);
    def actualJson = toObjectString({JobStateTransitionRepresenter.toJSON(it, transition) })

    assertThatJson(actualJson).isEqualTo(jobStateTransitionHash)
  }

  def jobStateTransitionHash = [
    id: 21,
    state: 'Building',
    state_change_time: 12345
  ]
}
