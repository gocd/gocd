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
package com.thoughtworks.go.apiv1.packagerepository.representers

import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.packagerepository.PackageRepositories
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PackageRepositoriesRepresenterTest {

  def expectedJson = [
    "_links"   : [
      "self": [
        "href": "http://test.host/go/api/admin/repositories"
      ],
      "doc" : [
        "href": apiDocsUrl("#package-repositories")
      ]
    ],
    "_embedded": [
      "package_repositories": [
        [
          "_links"         : [
            "self": [
              "href": "http://test.host/go/api/admin/repositories/repo-id"
            ],
            "doc" : [
              "href": apiDocsUrl("#package-repositories")
            ],
            "find": [
              "href": "http://test.host/go/api/admin/repositories/:repo_id"
            ]
          ],
          "repo_id"        : "repo-id",
          "name"           : "repo-name",
          "plugin_metadata": [
            "id"     : "plugin-id",
            "version": "1.0.0"
          ],
          "configuration"  : [
            [
              "key"  : "key",
              "value": "value"
            ]
          ],
          "_embedded"      : [
            "packages": []
          ]
        ]
      ]
    ]
  ]

  @Test
  void 'should serialize package repositories'() {
    def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
    def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)
    def packageRepositories = new PackageRepositories(packageRepository)

    def actualJson = toObjectString({ PackageRepositoriesRepresenter.toJSON(it, packageRepositories) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
