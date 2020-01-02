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

import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class BuildCauseRepresenterTest {
  @Test
  void 'should serialize build cause'() {
    def buildCause = BuildCause.createWithModifications(ModificationsMother.multipleModifications(), "approver")

    def expectedJSON = [
      "approver"          : "approver",
      "is_forced"         : false,
      "trigger_message"   : "modified by committer <html />",
      "material_revisions": [
        [
          "material_type": "Subversion",
          "material_name": "svn-material",
          "changed"      : false,
          "modifications": [
            [
              "_links"       : [
                "vsm": [
                  "href":
                    "http://test.host/go/materials/value_stream_map/0b4bba9653593af09fb45c3195ce227b50f14d7f90405b70aa6005966b8b0a35/3"
                ]
              ],
              "user_name"    : "committer <html />",
              "email_address": "foo@bar.com",
              "revision"     : "3",
              "modified_time": jsonDate(buildCause.getMaterialRevisions().getMaterialRevision(0).getModification(0).getModifiedTime()),
              "comment"      : "Added the README file with <html />"
            ], [
              "_links"       : [
                "vsm": [
                  "href":
                    "http://test.host/go/materials/value_stream_map/0b4bba9653593af09fb45c3195ce227b50f14d7f90405b70aa6005966b8b0a35/2"
                ]
              ],
              "user_name"    : "committer",
              "email_address": "foo@bar.com",
              "revision"     : "2",
              "modified_time": jsonDate(buildCause.getMaterialRevisions().getMaterialRevision(0).getModification(1).getModifiedTime()),
              "comment"      : "Added the README file"
            ], [
              "_links"       : [
                "vsm": [
                  "href":
                    "http://test.host/go/materials/value_stream_map/0b4bba9653593af09fb45c3195ce227b50f14d7f90405b70aa6005966b8b0a35/1"
                ]
              ],
              "user_name"    : "lgao",
              "email_address": "foo@bar.com",
              "revision"     : "1",
              "modified_time": jsonDate(buildCause.getMaterialRevisions().getMaterialRevision(0).getModification(2).getModifiedTime()),
              "comment"      : "Fixing the not checked in files"
            ]]
        ]]
    ]

    def actualJson = toObjectString({ BuildCauseRepresenter.toJSON(it, buildCause) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
