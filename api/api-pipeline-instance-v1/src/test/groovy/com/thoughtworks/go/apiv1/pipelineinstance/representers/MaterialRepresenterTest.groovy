/*
 * Copyright Thoughtworks, Inc.
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
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson

class MaterialRepresenterTest {
  @Test
  void 'should render material config properties'() {
    def material = MaterialsMother.gitMaterial("git1")

    def expectedJSON = [
      name       : "git1",
      fingerprint: "8b750b14ff6789936bd28b8408c2e04f37661e7bbc7a017fe1a3c4bcd963407a",
      type       : "Git",
      description: "URL: git1, Branch: main"
    ]

    def actualJson = toObjectString({ MaterialRepresenter.toJSON(it, material) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
