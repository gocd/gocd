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
package com.thoughtworks.go.apiv1.pipelineoperations.representers

import com.thoughtworks.go.domain.PipelinePauseInfo
import com.thoughtworks.go.presentation.PipelineStatusModel
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineStatusModelRepresenterTest {
  @Test
  void 'should serialize pipeline status into json'() {
    def pipelineStatusModel = new PipelineStatusModel(false, true, new PipelinePauseInfo(true, "some pause cause", "admin"))

    def actualJson = toObjectString({ PipelineStatusModelRepresenter.toJSON(it, pipelineStatusModel) })

    def expectedJson = [
      "paused"      : true,
      "paused_cause": "some pause cause",
      "paused_by"   : "admin",
      "locked"      : false,
      "schedulable" : true
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
