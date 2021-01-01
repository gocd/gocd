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
package com.thoughtworks.go.apiv2.compare.representers

import com.thoughtworks.go.domain.materials.Modification
import com.thoughtworks.go.domain.materials.Modifications
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class MaterialModificationRepresenterTest {
  @Test
  void 'should render modification related details'() {
    def date = new Date()
    def modification1 = new Modification("user1", "some comment", "abc@xyz.com", date, "some-revision")
    def modification2 = new Modification("user2", "some related comment", "def@xyz.com", date, "revision-sha")
    def modifications = new Modifications(modification1, modification2)

    def json = toArrayString({
      MaterialModificationRepresenter.toJSONArray(it, modifications)
    })

    def expectedJson = [
        [
            "commit_message": "some comment",
            "modified_at"   : jsonDate(date),
            "modified_by"   : "user1",
            "revision_sha"  : "some-revision"
        ],
        [
            "commit_message": "some related comment",
            "modified_at"   : jsonDate(date),
            "modified_by"   : "user2",
            "revision_sha"  : "revision-sha"
        ]
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }
}
