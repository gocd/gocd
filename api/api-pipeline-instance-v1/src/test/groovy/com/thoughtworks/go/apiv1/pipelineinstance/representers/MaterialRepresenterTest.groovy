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
package com.thoughtworks.go.apiv1.pipelineinstance.representers

import com.thoughtworks.go.helper.MaterialsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class MaterialRepresenterTest {
  @Test
  void 'should render material config properties'() {
    def material = MaterialsMother.gitMaterial("git1")

    def expectedJSON = [
      name       : "git1",
      fingerprint: "2fde537a026695884e2ee13e8f9730eca0610a3e407dbcc6bbce974f595c2f7c",
      type       : "Git",
      description: "URL: git1, Branch: master"
    ]

    def actualJson = toObjectString({ MaterialRepresenter.toJSON(it, material) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
