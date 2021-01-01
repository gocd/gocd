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
package com.thoughtworks.go.apiv2.materials.representers


import com.thoughtworks.go.api.base.JsonUtils
import com.thoughtworks.go.domain.materials.Modification
import com.thoughtworks.go.domain.materials.Modifications
import com.thoughtworks.go.server.util.Pagination
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ModificationsRepresenterTest {

  @Test
  void "should serialize modifications"() {
    def pagination = Pagination.pageStartingAt(null, 120, 10)
    def modification1 = new Modification("user1", "comment1", "email@ediblefrog", new Date(), "revision1")
    modification1.id = 1
    def modification2 = new Modification("user2", "comment2", "email@argentino", new Date(), "anotherRevision")
    modification2.id = 2
    def modifications = new Modifications(modification1, modification2)

    def actualJSON = JsonUtils.toObjectString({
      ModificationsRepresenter.toJSON(it, modifications, pagination, "some-fingerprint")
    })

    assertThatJson(actualJSON).isEqualTo([
      _links   : [
        self: [href: "http://test.host/go/api/materials/some-fingerprint/modifications"],
        doc : [href: apiDocsUrl("#materials")],
        find: [href: "http://test.host/go/api/materials/:fingerprint/modifications/{offset}"],
      ],
      _embedded: [
        modifications: [
          [
            email_address: "email@ediblefrog",
            id           : 1,
            modified_time: modification1.modifiedTime,
            user_name    : "user1",
            comment      : "comment1",
            revision     : "revision1"
          ],
          [
            email_address: "email@argentino",
            id           : 2,
            modified_time: modification2.modifiedTime,
            user_name    : "user2",
            comment      : "comment2",
            revision     : "anotherRevision"
          ]
        ],
        pagination   : [
          offset   : 0,
          total    : 120,
          page_size: 10
        ]
      ]
    ])
  }

  @Test
  void "should create empty json when modifications are null"() {
    def pagination = Pagination.pageStartingAt(20, 1, 10)

    def actualJSON = JsonUtils.toObjectString({
      ModificationsRepresenter.toJSON(it, null, pagination, "some-fingerprint")
    })

    assertThatJson(actualJSON).isEqualTo([
      _links   : [
        self: [href: "http://test.host/go/api/materials/some-fingerprint/modifications"],
        doc : [href: apiDocsUrl("#materials")],
        find: [href: "http://test.host/go/api/materials/:fingerprint/modifications/{offset}"],
      ],
      _embedded: [
        modifications: [],
        pagination   : [
          offset   : 20,
          total    : 1,
          page_size: 10
        ]
      ]
    ])
  }
}
