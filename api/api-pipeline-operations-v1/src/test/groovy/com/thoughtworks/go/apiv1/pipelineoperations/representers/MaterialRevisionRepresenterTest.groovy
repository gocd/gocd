/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.pipelineoperations.representers


import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv1.pipelineoperations.representers.MaterialRevisionRepresenter.fromJSON
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class MaterialRevisionRepresenterTest {
  @Test
  void 'should render material revision with modifications'() {
    def materialRevision = ModificationsMother.createHgMaterialRevisions().getMaterialRevision(0)

    def expectedJSON = [
      "changed"      : false,
      "material"     : [
        "id"         : -1,
        "name"       : "hg-url",
        "fingerprint": "4290e91721d0a0be34955725cfd754113588d9c27c39f9bd1a97c15e55832515",
        "type"       : "Mercurial",
        "description": "URL: hg-url"
      ],
      "modifications": [
        [
          "id"           : -1,
          "revision"     : "9fdcf27f16eadc362733328dd481d8a2c29915e1",
          "modified_time": jsonDate(materialRevision.getModification(0).modifiedTime),
          "user_name"    : "user2",
          "comment"      : "comment2",
          "email_address": "email2"
        ],
        [
          "id"           : -1,
          "revision"     : "eef77acd79809fc14ed82b79a312648d4a2801c6",
          "modified_time": jsonDate(materialRevision.getModification(1).modifiedTime),
          "user_name"    : "user1",
          "comment"      : "comment1",
          "email_address": "email1"
        ]
      ]
    ]

    def actualJson = toObjectString({ MaterialRevisionRepresenter.toJSON(it, materialRevision) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }

  @Test
  void 'should deserialize json'() {
    def inputJson = [
      fingerprint: "some-random-fingerprint",
      revision   : "some-revision"
    ]
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(inputJson)

    def materialForScheduling = fromJSON(jsonReader)

    assertThat(materialForScheduling.fingerprint).isEqualTo("some-random-fingerprint")
    assertThat(materialForScheduling.revision).isEqualTo("some-revision")
  }
}
