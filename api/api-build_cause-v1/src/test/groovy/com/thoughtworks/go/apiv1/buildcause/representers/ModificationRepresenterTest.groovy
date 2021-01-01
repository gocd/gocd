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
package com.thoughtworks.go.apiv1.buildcause.representers

import com.thoughtworks.go.helper.MaterialsMother
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ModificationRepresenterTest {
  @Test
  void 'should serialize a modification'() {
    def modification = ModificationsMother.aCheckIn("rev1", "file1")
    def material = MaterialsMother.gitMaterial("git1")

    def expectedJSON = [
      "_links"       : [
        "vsm": [
          "href": "http://test.host/go/materials/value_stream_map/2fde537a026695884e2ee13e8f9730eca0610a3e407dbcc6bbce974f595c2f7c/rev1"
        ]
      ],
      "user_name"    : "committer",
      "email_address": "foo@bar.com",
      "revision"     : "rev1",
      "modified_time": jsonDate(modification.getModifiedTime()),
      "comment"      : "Added the README file"
    ]

    def actualJson = toObjectString({ ModificationRepresenter.toJSON(it, modification, material) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
