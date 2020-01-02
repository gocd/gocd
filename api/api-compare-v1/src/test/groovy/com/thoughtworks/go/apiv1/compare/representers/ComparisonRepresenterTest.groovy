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
package com.thoughtworks.go.apiv1.compare.representers


import com.thoughtworks.go.domain.MaterialRevision
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv1.compare.representers.MaterialRevisionsRepresenterTest.getRevisions
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ComparisonRepresenterTest {
  @Test
  void 'should represent name and counters'() {
    def pipelineName = 'up42'
    def fromCounter = 1
    def toCounter = 4
    def date = new Date()
    def isBisect = false
    List<MaterialRevision> revisionList = getRevisions(date)

    def json = toObjectString({
      ComparisonRepresenter.toJSON(it, pipelineName, fromCounter, toCounter, isBisect, revisionList)
    })

    def expectedJson = [
      "_links"       : [
        "self": [
          "href": "http://test.host/go/api/pipelines/:pipeline_name/compare/:from_counter/:to_counter"
        ],
        "doc" : [
          "href": apiDocsUrl("#compare")
        ]
      ],
      "pipeline_name": pipelineName,
      "from_counter" : fromCounter,
      "to_counter"   : toCounter,
      "is_bisect"    : isBisect,
      "changes"      : [
        [
          "material": [
            "attributes": [
              "auto_update"  : true,
              "destination"  : "hg",
              "filter"       : null,
              "invert_filter": false,
              "name"         : null,
              "url"          : "hg-url"
            ],
            "type"      : "hg"
          ],
          "revision": [
            [
              "commit_message": "#4521 - get gadget working",
              "modified_at"   : jsonDate(date),
              "modified_by"   : "committer",
              "revision_sha"  : "cdef"
            ]
          ]
        ],
        [
          "material": [
            "attributes": [
              "auto_update"     : true,
              "branch"          : "master",
              "destination"     : "git",
              "filter"          : null,
              "invert_filter"   : false,
              "name"            : null,
              "shallow_clone"   : false,
              "submodule_folder": null,
              "url"             : "http://github.com"
            ],
            "type"      : "git"
          ],
          "revision": [
            [
              "commit_message": "#4200 - whatever",
              "modified_at"   : jsonDate(date),
              "modified_by"   : "committer",
              "revision_sha"  : "2345"
            ]
          ]
        ]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }

}