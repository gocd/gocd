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

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.spark.Routes
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class ConfigRepoConfigRepresenterV2Test {
  private static final String TEST_PLUGIN_ID = 'test.plugin'
  private static final String TEST_REPO_URL = 'https://something.com'
  private static final String ID = "repo-1"

  @Test
  void toJSON() {
    String json = toObjectString({ w -> ConfigRepoConfigRepresenterV2.toJSON(w, repo(ID)) })

    String self = "http://test.host/go${Routes.ConfigRepos.id(ID)}"
    String find = "http://test.host/go${Routes.ConfigRepos.find()}"

    assertThatJson(json).isEqualTo([
      _links       : [
        self: [href: self],
        doc : [href: Routes.ConfigRepos.DOC],
        find: [href: find],
      ],
      id           : ID,
      plugin_id    : TEST_PLUGIN_ID,
      material     : [
        type      : "git",
        attributes: [
          name       : null,
          url        : TEST_REPO_URL,
          branch     : "master",
          auto_update: true
        ]
      ],
      configuration: [
        [key: "foo", value: "bar"],
        [key: "baz", value: "quu"]
      ]
    ])
  }

  @Test
  void "toJSON should serialize errors"() {
    ConfigRepoConfig configRepo = repo(ID)
    configRepo.addError("id", "Duplicate Id.")
    configRepo.addError("material", "You have defined multiple configuration repositories with the same repository.")
    configRepo.getRepo().addError("autoUpdate", "Cannot be false.")
    String json = toObjectString({ w ->
      ConfigRepoConfigRepresenterV2.toJSON(w, configRepo)
    })

    String self = "http://test.host/go${Routes.ConfigRepos.id(ID)}"
    String find = "http://test.host/go${Routes.ConfigRepos.find()}"

    assertThatJson(json).isEqualTo([
      _links       : [
        self: [href: self],
        doc : [href: Routes.ConfigRepos.DOC],
        find: [href: find],
      ],
      id           : ID,
      plugin_id    : TEST_PLUGIN_ID,
      errors       : ["id"      : ["Duplicate Id."],
                      "material": ["You have defined multiple configuration repositories with the same repository."]],
      material     : [
        type      : "git",
        attributes: [
          errors     : ["auto_update": ["Cannot be false."]],
          name       : null,
          url        : TEST_REPO_URL,
          branch     : "master",
          auto_update: true
        ]
      ],
      configuration: [
        [key: "foo", value: "bar"],
        [key: "baz", value: "quu"]
      ]
    ])
  }

  @Test
  void fromJSON() {
    JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
      id           : ID,
      plugin_id    : TEST_PLUGIN_ID,
      material     : [
        type      : "git",
        attributes: [
          name       : null,
          url        : TEST_REPO_URL,
          branch     : "master",
          auto_update: true
        ]
      ],
      configuration: [
        [key: "foo", value: "bar"],
        [key: "baz", value: "quu"]
      ]
    ])

    ConfigRepoConfig expected = repo(ID)
    ConfigRepoConfig actual = ConfigRepoConfigRepresenterV2.fromJSON(json)

    assertEquals(expected, actual)
  }

  static ConfigRepoConfig repo(String id) {
    Configuration c = new Configuration()
    c.addNewConfigurationWithValue("foo", "bar", false)
    c.addNewConfigurationWithValue("baz", "quu", false)

    GitMaterialConfig materialConfig = git(TEST_REPO_URL)
    ConfigRepoConfig repo = new ConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)
    repo.setConfiguration(c)

    return repo
  }
}

