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
import com.thoughtworks.go.domain.MaterialRevision
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode
import com.thoughtworks.go.helper.MaterialsMother
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArray
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class SCMDependencyNodeRepresenterTest {
  @Test
  void 'should render scm dependency node details'() {
    def node = new SCMDependencyNode("fingerprint", "http://example.com", "Git")
    node.depth = 2
    node.addParentIfAbsent(new PipelineDependencyNode(new CaseInsensitiveString("pipeline-name"), "pipeline-name"))

    def materialRevision = new MaterialRevision(MaterialsMother.gitMaterial("http://example.com"), ModificationsMother.withModifiedFileWhoseNameLengthIsOneK())
    node.addMaterialRevision(materialRevision)

    def actualJson = toObjectString({ SCMDependencyNodeRepresenter.toJSON(it, node) })

    def expectedJson = [
      id                : "fingerprint",
      name              : "http://example.com",
      dependents        : [],
      parents           : ["pipeline-name"],
      depth             : 2,
      locator           : "",
      node_type         : "GIT",
      instances         : [],
      material_revisions: [
        [
          modifications: toArray({ ModificationsRepresenter.toJSON(it, node.id, materialRevision.modifications) })
        ]]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render materials names if provided'() {
    def node = new SCMDependencyNode("fingerprint", "http://example.com", "Git")
    node.addMaterialName("name1")

    def actualJson = toObjectString({ SCMDependencyNodeRepresenter.toJSON(it, node) })

    def expectedJson = [
      id                : "fingerprint",
      name              : "http://example.com",
      dependents        : [],
      parents           : [],
      depth             : 0,
      locator           : "",
      node_type         : "GIT",
      instances         : [],
      material_revisions: [],
      material_names    : ["name1"]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
