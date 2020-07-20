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

import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class UsagesRepresenterTest {
  @Test
  void 'should represent usages'() {
    def usages = new HashMap<>()
    usages.put("grp", ["pipeline1", "pipeline2"])

    def actualJson = toObjectString({ UsagesRepresenter.toJSON(it, "fingerprint", usages) })

    def expectedJson = [
      _links: [
        self: [
          href: "http://test.host/go/api/config/materials/fingerprint/usages"
        ],
        doc : [
          href: apiDocsUrl("#materials")
        ]
      ],
      usages: [
        [
          group   : "grp",
          pipelines: ["pipeline1", "pipeline2"]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should represent empty usages as well'() {
    def actualJson = toObjectString({
      UsagesRepresenter.toJSON(it, "fingerprint", new HashMap<String, List<String>>())
    })

    def expectedJson = [
      _links: [
        self: [
          href: "http://test.host/go/api/config/materials/fingerprint/usages"
        ],
        doc : [
          href: apiDocsUrl("#materials")
        ]
      ],
      usages: []
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
