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
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class VSMNodesRepresenterTest {
  @Test
  void 'should render list of nodes for VSM'() {
    def dependencyNode = new PipelineDependencyNode(new CaseInsensitiveString("pipeline"), "pipeline")
    def scmNode = new SCMDependencyNode("fingerprint", "url", "git")

    def actualJson = toObjectString({ VSMNodesRepresenter.toJSON(it, [dependencyNode, scmNode]) })

    def expectedJson = [
      nodes: [
        toObject({ PipelineDependencyNodeRepresenter.toJSON(it, dependencyNode) }),
        toObject({ SCMDependencyNodeRepresenter.toJSON(it, scmNode) })
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
