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
package com.thoughtworks.go.apiv11.shared.representers.configorigin

import com.thoughtworks.go.apiv11.admin.shared.representers.configorigin.ConfigRepoOriginRepresenter
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ConfigRepoOriginRepresenterTest {

  @Test
  void 'should render remote config origin'() {
    def gitMaterialConfig = git('https://github.com/config-repos/repo', 'master')
    def actualJson = toObjectString({
      ConfigRepoOriginRepresenter.toJSON(it, new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(gitMaterialConfig, 'json-plugon', 'repo1'), 'revision1'))
    })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  def expectedJson =
    [
      type  : 'config_repo',
      _links: [
        self: [
          href: 'http://test.host/go/api/admin/config_repos/repo1'
        ],
        doc : [
          href: apiDocsUrl('#config-repos')
        ],
        find: [
          href: 'http://test.host/go/api/admin/config_repos/:id'
        ]
      ],
      id    : 'repo1'
    ]
}
