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

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class VSMRepresenterTest {
  @Test
  void 'render current pipeline'() {
    def current = new PipelineDependencyNode(new CaseInsensitiveString("current"), "current")
    def vsmModel = new ValueStreamMapPresentationModel(current, null, [])

    def actualJson = toObjectString({ VSMRepresenter.toJSON(it, vsmModel) })

    def expectedJson = [
      current_pipeline: "current",
      levels          : []
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render current material and its dependent'() {
    def dependent = new PipelineDependencyNode(new CaseInsensitiveString("current"), "current")
    def current = new SCMDependencyNode("fingerprint", "url", "git")

    def vsmModel = new ValueStreamMapPresentationModel(null, current, [[dependent]])

    def actualJson = toObjectString({ VSMRepresenter.toJSON(it, vsmModel) })

    def expectedJson = [
      current_material: "url",
      levels          : [
        toObject({ VSMNodesRepresenter.toJSON(it, [dependent]) })
      ]]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
