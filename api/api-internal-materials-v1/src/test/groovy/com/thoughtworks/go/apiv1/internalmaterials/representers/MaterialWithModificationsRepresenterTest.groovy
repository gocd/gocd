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
import static com.thoughtworks.go.serverhealth.HealthStateScope.forMaterialConfig
import static com.thoughtworks.go.serverhealth.HealthStateType.general
import static com.thoughtworks.go.serverhealth.ServerHealthState.error
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning
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
    def timestamp = new Date().toTimestamp()
    def logs = [
      warning("Material Update hung", "The update has been hung for last 4 minutes", general(forMaterialConfig(git))),
      error("Updated failed", "There was an error executing the command", general(forMaterialConfig(git)))
    ]
    map.put(git, new MaterialInfo(modification, true, true, timestamp, logs))

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
          "can_trigger_update"         : true,
          "material_update_in_progress": true,
          "material_update_start_time" : jsonDate(timestamp),
          "modification"               : [
            "username"     : "lgao",
            "email_address": "foo@bar.com",
            "revision"     : modification.revision,
            "modified_time": jsonDate(modification.modifiedTime),
            "comment"      : "Fixing the not checked in files"
          ],
          "messages"                   : [
            [
              "description": "The update has been hung for last 4 minutes",
              "level"      : "WARNING",
              "message"    : "Material Update hung"
            ],
            [
              "description": "There was an error executing the command",
              "level"      : "ERROR",
              "message"    : "Updated failed"
            ]
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
    def timestamp = new Date().toTimestamp()
    map.put(git, new MaterialInfo(null, false, true, timestamp, []))

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
          "can_trigger_update"         : false,
          "material_update_in_progress": true,
          "material_update_start_time" : jsonDate(timestamp),
          "modification"               : null,
          "messages"                   : []
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not render material update start time if null'() {
    def map = new HashMap();
    def git = MaterialConfigsMother.git("http://example.com", "main")
    map.put(git, new MaterialInfo(null, false, false, null, []))

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
          "can_trigger_update"         : false,
          "material_update_in_progress": false,
          "modification"               : null,
          "messages"                   : []
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
