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

import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class BuildCauseRepresenterTest {
  @Test
  void 'should serialize build cause to json'() {
    def materialRevisions = ModificationsMother.multipleModifications()
    def modifications = materialRevisions.getMaterialRevision(0).modifications
    def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")

    def actualJson = toObjectString({ BuildCauseRepresenter.toJSON(it, buildCause) })

    def expectedJSON = [
      "trigger_message"   : "modified by committer <html />",
      "trigger_forced"    : false,
      "approver"          : "approver",
      "material_revisions": [
        [
          "changed"      : false,
          "material"     : [
            "id"         : -1,
            "name"       : "svn-material",
            "fingerprint": "0b4bba9653593af09fb45c3195ce227b50f14d7f90405b70aa6005966b8b0a35",
            "type"       : "Subversion",
            "description": "URL: http://foo/bar/baz, Username: username, CheckExternals: false"
          ],
          "modifications": [
            [
              "id"           : -1,
              "revision"     : "3",
              "modified_time": jsonDate(modifications.get(0).modifiedTime),
              "user_name"    : "committer <html />",
              "comment"      : "Added the README file with <html />",
              "email_address": "foo@bar.com"
            ],
            [
              "id"           : -1,
              "revision"     : "2",
              "modified_time": jsonDate(modifications.get(1).modifiedTime),
              "user_name"    : "committer",
              "comment"      : "Added the README file",
              "email_address": "foo@bar.com"
            ],
            [
              "id"           : -1,
              "revision"     : "1",
              "modified_time": jsonDate(modifications.get(2).modifiedTime),
              "user_name"    : "lgao",
              "comment"      : "Fixing the not checked in files",
              "email_address": "foo@bar.com"
            ]
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
