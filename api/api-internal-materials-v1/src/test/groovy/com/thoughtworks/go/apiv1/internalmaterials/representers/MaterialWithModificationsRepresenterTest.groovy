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

package com.thoughtworks.go.apiv1.internalmaterials.representers

import com.thoughtworks.go.apiv1.internalmaterials.models.MaterialInfo
import com.thoughtworks.go.apiv1.internalmaterials.representers.materials.MaterialsRepresenter
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Collections.emptyMap
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class MaterialWithModificationsRepresenterTest {
  @Test
  void 'should render empty json if map is empty'() {
    def actualJson = toObjectString({ MaterialWithModificationsRepresenter.toJSON(it, emptyMap()) })

    def expectedJson = [
      _links   : [
        self: [
          href: "http://test.host/go/api/internal/materials"
        ],
        doc : [
          href: apiDocsUrl("#materials")
        ]
      ],
      materials: []
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render json'() {
    def map = new HashMap();
    def git = MaterialConfigsMother.git("http://example.com", "main")
    def modification = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()
    map.put(git, new MaterialInfo(modification, true))

    def actualJson = toObjectString({ MaterialWithModificationsRepresenter.toJSON(it, map) })

    def expectedJson = [
      _links   : [
        self: [
          href: "http://test.host/go/api/internal/materials"
        ],
        doc : [
          href: apiDocsUrl("#materials")
        ]
      ],
      materials: [
        [
          "config"                     : toObject(MaterialsRepresenter.toJSON(git)),
          "material_update_in_progress": true,
          "modification"               : [
            "username"     : "lgao",
            "email_address": "foo@bar.com",
            "revision"     : modification.revision,
            "modified_time": jsonDate(modification.modifiedTime),
            "comment"      : "Fixing the not checked in files"
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render modification as null'() {
    def map = new HashMap();
    def git = MaterialConfigsMother.git("http://example.com", "main")
    map.put(git, new MaterialInfo(null, false))

    def actualJson = toObjectString({ MaterialWithModificationsRepresenter.toJSON(it, map) })

    def expectedJson = [
      _links   : [
        self: [
          href: "http://test.host/go/api/internal/materials"
        ],
        doc : [
          href: apiDocsUrl("#materials")
        ]
      ],
      materials: [
        [
          "config"                     : toObject(MaterialsRepresenter.toJSON(git)),
          "material_update_in_progress": false,
          "modification"               : null
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
