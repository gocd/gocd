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

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.config.PluginConfiguration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.packagerepository.PackageDefinition
import com.thoughtworks.go.domain.packagerepository.PackageRepository
import com.thoughtworks.go.domain.packagerepository.Packages
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv1.packagerepository.representers.PackageRepositoryRepresenter.fromJSON
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class PackageRepositoryRepresenterTest {

  def expectedJson = [
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
      "packages": [
        [
          "_links": [
            "doc" : [
              "href": apiDocsUrl("#packages")
            ],
            "find": [
              "href": "http://test.host/go/api/admin/packages/:package_id"
            ],
            "self": [
              "href": "http://test.host/go/api/admin/packages/pkg-id"
            ]
          ],
          "id"    : "pkg-id",
          "name"  : "pkg-name"
        ]
      ]
    ]
  ]

  @Test
  void 'should serialize package repository'() {
    def pluginConfiguration = new PluginConfiguration('plugin-id', '1.0.0')
    def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
    def packageRepository = new PackageRepository('repo-id', 'repo-name', pluginConfiguration, configuration)
    def packages = new Packages(new PackageDefinition('pkg-id', 'pkg-name', null))
    packageRepository.setPackages(packages)

    def actualJson = toObjectString({ PackageRepositoryRepresenter.toJSON(it, packageRepository) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should deserialize package repository'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(expectedJson)

    def pluginConfiguration = new PluginConfiguration('plugin-id', '1.0.0')
    def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
    def packageRepository = new PackageRepository('repo-id', 'repo-name', pluginConfiguration, configuration)
    def packages = new Packages(new PackageDefinition('pkg-id', 'pkg-name', null))
    packageRepository.setPackages(packages)

    def actualPkgRepo = fromJSON(jsonReader)

    assertEquals(actualPkgRepo.name, packageRepository.name)
    assertEquals(actualPkgRepo.repoId, packageRepository.repoId)
    assertEquals(actualPkgRepo.pluginConfiguration, packageRepository.pluginConfiguration)
    assertEquals(actualPkgRepo.configuration, packageRepository.configuration)
  }

  @Test
  void 'should serialize errors as well'() {
    def pluginConfiguration = new PluginConfiguration('plugin-id', '1.0.0')
    def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
    def packageRepository = new PackageRepository('repo-id', 'repo-name', pluginConfiguration, configuration)
    packageRepository.addError('field', 'some error message')
    packageRepository.addConfigurationErrorFor('config-key', 'some config error msg')

    def actualJson = toObjectString({ PackageRepositoryRepresenter.toJSON(it, packageRepository) })

    def expectedJSONWithErrors = [
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
      ],
      "errors"         : [
        "field": [
          "some error message"
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJSONWithErrors)
  }
}
