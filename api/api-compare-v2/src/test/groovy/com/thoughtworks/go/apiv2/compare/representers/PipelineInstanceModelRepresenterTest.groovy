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
package com.thoughtworks.go.apiv2.compare.representers

import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.helper.StageMother
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineInstanceModelRepresenterTest {
  @Test
  void 'should serialize into json'() {
    def date = new Date()
    def stage = StageMother.passedStageInstance("pipelineName", "stageName", 4, "buildName", date)
    def stageInstanceModel = StageMother.toStageInstanceModel(stage)
    def stageInstanceModels = new StageInstanceModels()
    stageInstanceModels.add(stageInstanceModel)

    def materialRevisions = ModificationsMother.multipleModifications()
    def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")

    def pipelineInstanceModel = PipelineInstanceModel.createPipeline("pipelineName", 4, "label", buildCause, stageInstanceModels)

    def buildCauseJson = toObject({ BuildCauseRepresenter.toJSON(it, pipelineInstanceModel.getBuildCause()) })
    def actualJson = toObjectString({ PipelineInstanceModelRepresenter.toJSON(it, pipelineInstanceModel) })

    def expectedJson = [
      "name"          : "pipelineName",
      "counter"       : 4,
      "label"         : "label",
      "natural_order" : pipelineInstanceModel.getNaturalOrder(),
      "comment"       : null,
      "scheduled_date": pipelineInstanceModel.getScheduledDate().getTime(),
      "build_cause"   : buildCauseJson,
      "stages"        : pipelineInstanceModel.getStageHistory().collect { eachItem ->
        toObject({
          StageInstanceModelRepresenter.toJSON(it, eachItem)
        })
      }
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
