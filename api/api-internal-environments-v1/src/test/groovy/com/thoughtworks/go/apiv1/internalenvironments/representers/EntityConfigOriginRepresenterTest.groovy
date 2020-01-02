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
package com.thoughtworks.go.apiv1.internalenvironments.representers

import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class EntityConfigOriginRepresenterTest {
  @Test
  void 'should render config xml origin with hal representation'() {
    def actualJSON = toObjectString({ EntityConfigOriginRepresenter.toJSON(it, new FileConfigOrigin()) })

    def expectedJSON = [
      "_links": [
        "self": [
          "href": "http://test.host/go/admin/config_xml"
        ],
        "doc" : [
          "href": apiDocsUrl("#get-configuration")
        ]
      ],
      "type"  : "gocd"
    ]

    assertThatJson(actualJSON).isEqualTo(expectedJSON)
  }

  @Test
  void 'should render config xml origin with hal representation when origin is null'() {
    def actualJSON = toObjectString({ EntityConfigOriginRepresenter.toJSON(it, null) })

    def expectedJSON = [
      "_links": [
        "self": [
          "href": "http://test.host/go/admin/config_xml"
        ],
        "doc" : [
          "href": apiDocsUrl("#get-configuration")
        ]
      ],
      "type"  : "gocd"
    ]

    assertThatJson(actualJSON).isEqualTo(expectedJSON)
  }

  @Test
  void 'should render config repo origin with hal representation'() {
    def origin = new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.git("foo.git"), "json-plugon", "repo1"), "revision1");
    def actualJSON = toObjectString({ EntityConfigOriginRepresenter.toJSON(it, origin) })

    def expectedJSON = [
      "_links": [
        "doc" : ["href": apiDocsUrl("#config-repos")],
        "find": ["href": "http://test.host/go/api/admin/config_repos/:id"],
        "self": ["href": "http://test.host/go/api/admin/config_repos/repo1"]
      ],
      "id"    : "repo1",
      "type"  : "config_repo"
    ]

    assertThatJson(actualJSON).isEqualTo(expectedJSON)
  }


}
