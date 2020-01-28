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
package com.thoughtworks.go.apiv2.configrepos.representers

import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import static com.thoughtworks.go.helper.MaterialConfigsMother.hg
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.spark.Routes
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ConfigReposConfigRepresenterV2Test {

  private static final String ID_1 = "foo"
  private static final String ID_2 = "bar"
  private static final String TEST_PLUGIN_ID = "test.plugin"
  private static final String TEST_REPO_URL = "https://test.com"


  @Test
  void toJSON() {
    String actual = toObjectString({ w -> ConfigReposConfigRepresenterV2.toJSON(w, new ConfigReposConfig(repo(ID_1), repo(ID_2))) })

    assertThatJson(actual).isEqualTo(
      [
        _links   : [
          self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
        ],
        _embedded: [
          config_repos: [
            expectedRepoJson(ID_1),
            expectedRepoJson(ID_2)
          ]
        ]
      ]
    )
  }

  static Map expectedRepoJson(String id) {
    return [
      _links       : [
        self: [href: "http://test.host/go${Routes.ConfigRepos.id(id)}".toString()],
        doc : [href: Routes.ConfigRepos.DOC],
        find: [href: "http://test.host/go${Routes.ConfigRepos.find()}".toString()],
      ],

      id           : id,
      plugin_id    : TEST_PLUGIN_ID,
      material     : [
        type      : "hg",
        attributes: [
          name       : null,
          url        : "${TEST_REPO_URL}/$id".toString(),
          auto_update: true
        ]
      ],
      configuration: []
    ]
  }

  static ConfigRepoConfig repo(String id) {
    HgMaterialConfig materialConfig = hg("${TEST_REPO_URL}/$id", "")
    ConfigRepoConfig repo = ConfigRepoConfig.createConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)

    return repo
  }
}
