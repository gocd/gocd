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
import com.thoughtworks.go.domain.Stages
import com.thoughtworks.go.helper.StageMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class StageRepresenterTest {
  @Test
  void 'should return empty if stages are not present'() {
    def actualJson = toArrayString({ StageRepresenter.toJSON(it, null, "", 1) })

    assertThatJson(actualJson).isEqualTo([])
  }

  @Test
  void 'should render stages'() {
    def stage = StageMother.passedStageInstance("stage-name", "job-name", "pipeline-name")
    def actualJson = toArrayString({ StageRepresenter.toJSON(it, new Stages(stage), "", 1) })

    def expectedJson = [
      [
        name    : "stage-name",
        status  : "Passed",
        duration: 0,
        locator : "/go/pipelines//1/stage-name/1"
      ]]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'render duration as null if stage is not completed yet'() {
    def stage = StageMother.scheduledStage("pipeline-name", 1, "stage-name", 1, "job-name")
    def actualJson = toArrayString({ StageRepresenter.toJSON(it, new Stages(stage), "", 1) })

    def expectedJson = [
      [
        name    : "stage-name",
        status  : "Building",
        duration: null,
        locator : "/go/pipelines//1/stage-name/1"
      ]]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not render locator if stage state is unknown'() {
    def stage = new Stage()
    stage.name = "stage-name"
    stage.state = StageState.Unknown

    def actualJson = toArrayString({ StageRepresenter.toJSON(it, new Stages(stage), "", 1) })

    def expectedJson = [
      [
        name    : "stage-name",
        status  : "Unknown",
        duration: null
      ]]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
