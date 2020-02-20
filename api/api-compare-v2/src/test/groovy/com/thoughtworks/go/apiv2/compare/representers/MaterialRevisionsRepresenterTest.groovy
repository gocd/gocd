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
package com.thoughtworks.go.apiv2.compare.representers

import com.thoughtworks.go.config.materials.ScmMaterial
import com.thoughtworks.go.domain.MaterialRevision
import com.thoughtworks.go.domain.materials.Modification
import com.thoughtworks.go.helper.MaterialsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static com.thoughtworks.go.helper.ModificationsMother.checkinWithComment
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class MaterialRevisionsRepresenterTest {
  @Test
  void 'should render modification related details'() {
    def checkinTime = new Date()
    List<MaterialRevision> revisions = getRevisions(checkinTime)

    def json = toArrayString({
      MaterialRevisionsRepresenter.toJSONArray(it, revisions)
    })

    def expectedJson = [
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
                    "modified_at"   : jsonDate(checkinTime),
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
                    "modified_at"   : jsonDate(checkinTime),
                    "modified_by"   : "committer",
                    "revision_sha"  : "2345"
                ]
            ]
        ]
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }

  static List<MaterialRevision> getRevisions(Date checkinTime) {
    ScmMaterial git = MaterialsMother.gitMaterial("http://github.com", null, "master")
    git.setFolder("git")
    ScmMaterial hg = MaterialsMother.hgMaterial()
    hg.setFolder("hg")

    Modification hgCommit = checkinWithComment("cdef", "#4521 - get gadget working", checkinTime)
    Modification gitCommit = checkinWithComment("2345", "#4200 - whatever", checkinTime)

    List<MaterialRevision> revisionList = Arrays.asList(
        new MaterialRevision(hg, hgCommit),
        new MaterialRevision(git, gitCommit))
    return revisionList
  }
}
