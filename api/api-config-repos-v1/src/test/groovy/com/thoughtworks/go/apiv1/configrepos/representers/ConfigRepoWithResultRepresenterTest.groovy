/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.configrepos.representers

import com.thoughtworks.go.apiv1.configrepos.ConfigRepoWithResult
import com.thoughtworks.go.config.PartialConfigParseResult
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.spark.Routes
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ConfigRepoWithResultRepresenterTest {
  private static final String TEST_PLUGIN_ID = "test.configrepo.plugin"
  private static final String TEST_REPO_URL = "https://fakeurl.com"

  @Test
  void toJSON() {
    String id = "foo"
    String json = toObjectString({ w ->
      ConfigRepoWithResultRepresenter.toJSON(w, repo(id))
    })

    String self = "http://test.host/go${Routes.ConfigRepos.id(id)}"
    String find = "http://test.host/go${Routes.ConfigRepos.find()}"

    assertThatJson(json).isEqualTo([
      _links       : [
        self: [href: self],
        doc : [href: Routes.ConfigRepos.DOC],
        find: [href: find],
      ],

      id           : id,
      plugin_id    : TEST_PLUGIN_ID,
      material     : [
        type      : "hg",
        attributes: [
          name       : null,
          url        : TEST_REPO_URL,
          auto_update: true
        ]
      ],
      configuration: [
        [key: "foo", value: "bar"],
        [key: "baz", value: "quu"]
      ],

      last_parse   : [
        revision: "${id}-123".toString(),
        success : false,
        error   : "boom!"
      ]
    ])
  }

  static ConfigRepoWithResult repo(String id) {
    Configuration c = new Configuration()
    c.addNewConfigurationWithValue("foo", "bar", false)
    c.addNewConfigurationWithValue("baz", "quu", false)

    HgMaterialConfig materialConfig = new HgMaterialConfig(TEST_REPO_URL, "")
    ConfigRepoConfig repo = new ConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)
    repo.setConfiguration(c)

    return new ConfigRepoWithResult(repo, new PartialConfigParseResult("${id}-123", new RuntimeException("boom!")))
  }
}