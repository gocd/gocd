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
package com.thoughtworks.go.apiv1.buildcause.representers

import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class MaterialRevisionRepresenterTest {
  @Test
  void 'should serialize a material revision which uses an SCM material'() {
    def materialRevision = ModificationsMother.createHgMaterialRevisions().getMaterialRevision(0)

    def expectedJSON = [
      "material_type": "Mercurial",
      "material_name": "hg-url",
      "changed"      : false,
      "modifications": [
        [
          "_links"       : [
            "vsm": [
              "href": "http://test.host/go/materials/value_stream_map/4290e91721d0a0be34955725cfd754113588d9c27c39f9bd1a97c15e55832515/9fdcf27f16eadc362733328dd481d8a2c29915e1"
            ]
          ],
          "user_name"    : "user2",
          "email_address": "email2",
          "revision"     : "9fdcf27f16eadc362733328dd481d8a2c29915e1",
          "modified_time": jsonDate(materialRevision.getModification(0).getModifiedTime()),
          "comment"      : "comment2"
        ],
        [
          "_links"       : [
            "vsm": [
              "href": "http://test.host/go/materials/value_stream_map/4290e91721d0a0be34955725cfd754113588d9c27c39f9bd1a97c15e55832515/eef77acd79809fc14ed82b79a312648d4a2801c6"
            ]
          ],
          "user_name"    : "user1",
          "email_address": "email1",
          "revision"     : "eef77acd79809fc14ed82b79a312648d4a2801c6",
          "modified_time": jsonDate(materialRevision.getModification(1).getModifiedTime()),
          "comment"      : "comment1"
        ]
      ]
    ]

    def actualJson = toObjectString({ MaterialRevisionRepresenter.toJSON(it, materialRevision) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }

  @Test
  void 'should serialize a material revision which uses a dependency material'() {
    def materialRevision = ModificationsMother.dependencyMaterialRevision("pipeline1", 1, "label", "stage1", 1, new Date())

    def expectedJSON = [
      "material_type": "Pipeline",
      "material_name": "pipeline1",
      "changed"      : false,
      "modifications": [
        [
          "_links"        : [
            "vsm"              : [
              "href": "http://test.host/go/pipelines/value_stream_map/pipeline1/1"
            ],
            "stage_details_url": [
              "href": "http://test.host/go/pipelines/pipeline1/1/stage1/1"
            ]
          ],
          "revision"      : "pipeline1/1/stage1/1",
          "modified_time" : jsonDate(materialRevision.getModification(0).getModifiedTime()),
          "pipeline_label": "label"
        ]
      ]
    ]

    def actualJson = toObjectString({ MaterialRevisionRepresenter.toJSON(it, materialRevision) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
