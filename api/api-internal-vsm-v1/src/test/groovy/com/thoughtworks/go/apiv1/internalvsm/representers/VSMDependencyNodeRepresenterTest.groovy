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
import com.thoughtworks.go.domain.valuestreammap.DummyNode
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode
import com.thoughtworks.go.domain.valuestreammap.VSMViewType
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class VSMDependencyNodeRepresenterTest {
  @Test
  void 'should render vsm dependency node'() {
    def parent = new PipelineDependencyNode(new CaseInsensitiveString("id"), "pipeline-name")
    def dependent = new PipelineDependencyNode(new CaseInsensitiveString("other-id"), "some-other-pipeline-name")
    def node = new DummyNode("node-id", "node-name")
    node.depth = 3
    node.addParentIfAbsent(parent)
    node.addChildIfAbsent(dependent)

    def actualJson = toObjectString({ VSMDependencyNodeRepresenter.toJSON(it, node) })
    def expectedJson = [
      id        : "node-id",
      name      : "node-name",
      dependents: ["other-id"],
      parents   : ["id"],
      depth     : 3
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render view type if present'() {
    def node = new DummyNode("node-id", "node-name")
    node.setViewType(VSMViewType.NO_PERMISSION)

    def actualJson = toObjectString({ VSMDependencyNodeRepresenter.toJSON(it, node) })
    def expectedJson = [
      id        : "node-id",
      name      : "node-name",
      dependents: [],
      parents   : [],
      depth     : 0,
      view_type : "NO_PERMISSION"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should add message if not null'() {
    def node = new PipelineDependencyNode(new CaseInsensitiveString("id"), "pipeline-name")
    node.message = "Some error message"

    def actualJson = toObjectString({ VSMDependencyNodeRepresenter.toJSON(it, node) })

    def expectedJson = [
      id        : "id",
      name      : "pipeline-name",
      dependents: [],
      parents   : [],
      depth     : 0,
      message   : "Some error message"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
