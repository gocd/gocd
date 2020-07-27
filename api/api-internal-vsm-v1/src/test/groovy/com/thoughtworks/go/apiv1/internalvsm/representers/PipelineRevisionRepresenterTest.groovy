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

package com.thoughtworks.go.apiv1.internalvsm.representers

import com.thoughtworks.go.domain.Stage
import com.thoughtworks.go.domain.StageState
import com.thoughtworks.go.domain.valuestreammap.PipelineRevision
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArray
import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineRevisionRepresenterTest {
  @Test
  void 'should return empty if revisions are not present'() {
    def actualJson = toArrayString({ PipelineRevisionRepresenter.toJSON(it, "pipeline-name", []) })

    assertThatJson(actualJson).isEqualTo([])
  }

  @Test
  void 'should render pipeline revisions'() {
    def revision = new PipelineRevision("pipeline-name", 1, "pipeline-label")
    def stage = new Stage()
    stage.name = "stage-name"
    stage.state = StageState.Unknown
    revision.addStage(stage)

    def actualJson = toArrayString({ PipelineRevisionRepresenter.toJSON(it, "pipeline-name", [revision]) })

    def expectedJson = [
      [
        label  : "pipeline-label",
        counter: 1,
        locator: "/go/pipelines/value_stream_map/pipeline-name/1",
        stages : toArray({ StageRepresenter.toJSON(it, revision.stages, "pipeline-name", 1) })
      ]]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render locator as empty when counter is 0'() {
    def revision = new PipelineRevision("pipeline-name", 0, "pipeline-label")

    def actualJson = toArrayString({ PipelineRevisionRepresenter.toJSON(it, "pipeline-name", [revision]) })

    def expectedJson = [
      [
        label  : "pipeline-label",
        counter: 0,
        locator: "",
        stages : []
      ]]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
