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

package com.thoughtworks.go.apiv1.pipelineinstance.representers

import com.thoughtworks.go.helper.StageMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class StageInstanceModelRepresenterTest {
  @Test
  void 'should deserialize into json'() {
    def date = new Date()
    def stage = StageMother.passedStageInstance("pipelineName", "stageName", 4, "buildName", date)
    def stageInstanceModel = StageMother.toStageInstanceModel(stage)

    def actualJson = toObjectString({ StageInstanceModelRepresenter.toJSON(it, stageInstanceModel) })

    def expectedJson = [
      "result"            : "Passed",
      "status"            : "Passed",
      "rerun_of_counter"  : null,
      "name"              : "stageName",
      "counter"           : "4",
      "scheduled"         : true,
      "approval_type"     : "success",
      "approved_by"       : "changes",
      "operate_permission": false,
      "can_run"           : false,
      "jobs"              : stageInstanceModel.getBuildHistory().collect {
        eachItem ->
          toObject({
            JobHistoryItemRepresenter.toJSON(it, eachItem)
          })
      }
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not add result if null'() {
    def date = new Date()
    def stage = StageMother.passedStageInstance("pipelineName", "stageName", 4, "buildName", date)
    def stageInstanceModel = StageMother.toStageInstanceModel(stage)
    stageInstanceModel.result = null

    def actualJson = toObjectString({ StageInstanceModelRepresenter.toJSON(it, stageInstanceModel) })

    def expectedJson = [
      "status"            : "Passed",
      "rerun_of_counter"  : null,
      "name"              : "stageName",
      "counter"           : "4",
      "scheduled"         : true,
      "approval_type"     : "success",
      "approved_by"       : "changes",
      "operate_permission": false,
      "can_run"           : false,
      "jobs"              : stageInstanceModel.getBuildHistory().collect {
        eachItem ->
          toObject({
            JobHistoryItemRepresenter.toJSON(it, eachItem)
          })
      }
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should add rerun_of_counter if not null'() {
    def date = new Date()
    def stage = StageMother.passedStageInstance("pipelineName", "stageName", 4, "buildName", date)
    def stageInstanceModel = StageMother.toStageInstanceModel(stage)
    stageInstanceModel.setRerunOfCounter(3)

    def actualJson = toObjectString({ StageInstanceModelRepresenter.toJSON(it, stageInstanceModel) })

    def expectedJson = [
      "result"            : "Passed",
      "status"            : "Passed",
      "rerun_of_counter"  : 3,
      "name"              : "stageName",
      "counter"           : "4",
      "scheduled"         : true,
      "approval_type"     : "success",
      "approved_by"       : "changes",
      "operate_permission": false,
      "can_run"           : false,
      "jobs"              : stageInstanceModel.getBuildHistory().collect {
        eachItem ->
          toObject({
            JobHistoryItemRepresenter.toJSON(it, eachItem)
          })
      }

    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }


}
