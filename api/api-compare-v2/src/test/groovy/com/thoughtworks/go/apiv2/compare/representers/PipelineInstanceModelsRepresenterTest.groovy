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

import com.thoughtworks.go.domain.PipelineRunIdInfo
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.helper.StageMother
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineInstanceModelsRepresenterTest {
  private PipelineInstanceModels pipelineInstanceModels = PipelineInstanceModels.createPipelineInstanceModels()

  @BeforeEach
  void setUp() {
    for (int i = 6; i >= 2; i--) {
      def pipelineInstanceModel = createPipelineInstance(i + 4)
      pipelineInstanceModel.id = i
      pipelineInstanceModels.add(pipelineInstanceModel)
    }
  }

  @Test
  void 'should serializer into json with next and previous links'() {
    def actualJson = toObjectString({ PipelineInstanceModelsRepresenter.toJSON(it, pipelineInstanceModels) })

    def expectedJson = [
      "pipelines": pipelineInstanceModels.collect { model ->
        toObject({ PipelineInstanceModelRepresenter.toJSON(it, model) })
      }
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  private static def createPipelineInstance(int counter) {
    def stage = StageMother.passedStageInstance("pipelineName", "stageName", 4, "buildName", new Date())
    def stageInstanceModel = StageMother.toStageInstanceModel(stage)
    def stageInstanceModels = new StageInstanceModels()
    stageInstanceModels.add(stageInstanceModel)

    def materialRevisions = ModificationsMother.multipleModifications()
    def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")
    return PipelineInstanceModel.createPipeline("pipelineName", counter, "label", buildCause, stageInstanceModels)
  }
}
