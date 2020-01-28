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

import com.thoughtworks.go.apiv2.configrepos.ConfigRepoWithResult
import com.thoughtworks.go.config.PartialConfigParseResult
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import static com.thoughtworks.go.helper.MaterialConfigsMother.hg
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.materials.Modification
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.spark.Routes
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ConfigRepoWithResultRepresenterTest {
  private static final String TEST_PLUGIN_ID = "test.configrepo.plugin"
  private static final String TEST_REPO_URL = "https://fakeurl.com"

  @Test
  void toJSON() {
    String id = "foo"
    ConfigRepoWithResult result = repo(id)

    String json = toObjectString({ w ->
      ConfigRepoWithResultRepresenter.toJSON(w, result, true)
    })

    String self = "http://test.host/go${Routes.ConfigRepos.id(id)}"
    String find = "http://test.host/go${Routes.ConfigRepos.find()}"

    assertThatJson(json).isEqualTo([
      _links                     : [
        self: [href: self],
        doc : [href: Routes.ConfigRepos.DOC],
        find: [href: find],
      ],

      id                         : id,
      plugin_id                  : TEST_PLUGIN_ID,
      material                   : [
        type      : "hg",
        attributes: [
          name       : null,
          url        : TEST_REPO_URL,
          auto_update: true
        ]
      ],
      can_administer: true,
      configuration              : [
        [key: "foo", value: "bar"],
        [key: "baz", value: "quu"]
      ],
      material_update_in_progress: false,
      parse_info                 : [
        error                     : "Boom!",
        good_modification         : null,
        latest_parsed_modification: [
          "username"     : "lgao",
          "email_address": "foo@bar.com",
          "revision"     : "foo-123",
          "comment"      : "Fixing the not checked in files",
          "modified_time": jsonDate(result.result().latestParsedModification.modifiedTime)
        ]
      ]
    ])
  }

  static ConfigRepoWithResult repo(String id) {
    Modification modification = ModificationsMother.oneModifiedFile("${id}-123")
    Exception exception = new Exception("Boom!")

    PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseFailed(modification, exception)

    Configuration c = new Configuration()
    c.addNewConfigurationWithValue("foo", "bar", false)
    c.addNewConfigurationWithValue("baz", "quu", false)

    HgMaterialConfig materialConfig = hg(TEST_REPO_URL, "")
    ConfigRepoConfig repo = ConfigRepoConfig.createConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)
    repo.setConfiguration(c)

    return new ConfigRepoWithResult(repo, expectedParseResult, false)
  }
}
