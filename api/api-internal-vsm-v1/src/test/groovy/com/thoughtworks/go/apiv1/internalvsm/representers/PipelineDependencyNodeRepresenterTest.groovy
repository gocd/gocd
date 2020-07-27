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
import com.thoughtworks.go.domain.valuestreammap.VSMViewType
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineDependencyNodeRepresenterTest {
  @Test
  void 'should render pipeline dependency node details'() {
    def node = new PipelineDependencyNode(new CaseInsensitiveString("pipeline-name"), "pipeline-name")
    node.depth = 5
    node.addParentIfAbsent(new SCMDependencyNode("fingerprint", "some-url", "git"))
    node.addChildIfAbsent(new PipelineDependencyNode(new CaseInsensitiveString("downstream"), "downstream"))

    def actualJson = toObjectString({ PipelineDependencyNodeRepresenter.toJSON(it, node) })

    def expectedJson = [
      id        : "pipeline-name",
      name      : "pipeline-name",
      dependents: ["downstream"],
      parents   : ["fingerprint"],
      depth     : 5,
      locator   : "/go/pipeline/activity/pipeline-name",
      node_type : "PIPELINE",
      instances : [],
      edit_path : "/go/admin/pipelines/pipeline-name/edit",
      can_edit  : false
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render locator as empty if view_type is not null'() {
    def node = new PipelineDependencyNode(new CaseInsensitiveString("pipeline-name"), "pipeline-name")
    node.viewType = VSMViewType.NO_PERMISSION

    def actualJson = toObjectString({ PipelineDependencyNodeRepresenter.toJSON(it, node) })

    def expectedJson = [
      id        : "pipeline-name",
      name      : "pipeline-name",
      dependents: [],
      parents   : [],
      depth     : 0,
      view_type : "NO_PERMISSION",
      locator   : "",
      node_type : "PIPELINE",
      instances : [],
      edit_path : "/go/admin/pipelines/pipeline-name/edit",
      can_edit  : false
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
