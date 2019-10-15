/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.packages.representers

import com.thoughtworks.go.api.base.OutputWriter
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.config.ConfigurationKey
import com.thoughtworks.go.domain.config.ConfigurationProperty
import com.thoughtworks.go.domain.config.ConfigurationValue
import com.thoughtworks.go.domain.packagerepository.PackageDefinition
import com.thoughtworks.go.domain.packagerepository.PackageRepository
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.*

class PackageDefinitionRepresenterTest {

  def expectedJson = [
    _links       : [
      self: [
        href: "http://test.host/go/api/admin/packages/package-id-1"
      ],
      doc : [
        href: apiDocsUrl("#packages")
      ],
      find: [
        href: "http://test.host/go/api/admin/packages/:package_id"
      ]
    ],
    name         : "package-1",
    id           : "package-id-1",
    auto_update  : true,
    configuration: [
      [
        "key"  : "PACKAGE_NAME",
        "value": "foo"
      ]
    ],
    package_repo : [
      _links: [
        self: [
          href: "http://test.host/go/api/admin/repositories/package-repo-id-1"
        ],
        doc : [
          href: apiDocsUrl("#package-repositories")
        ],
        find: [
          href: "http://test.host/go/api/admin/repositories/:repo_id"
        ]
      ],
      id    : "package-repo-id-1",
      name  : "package-repo-name-1"
    ]
  ]

  @Test
  void 'should serialize package definition'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(expectedJson)
    def packageDefinition = PackageDefinitionRepresenter.fromJSON(jsonReader)

    def configurationProperties = new Configuration(new ConfigurationProperty(new ConfigurationKey('PACKAGE_NAME'), new ConfigurationValue('foo')))
    def expectedPackageDefinition = new PackageDefinition('package-id-1', 'package-1', configurationProperties)
    expectedPackageDefinition.setRepository(new PackageRepository('package-repo-id-1', 'package-repo-name-1', null, null))

    assertEquals(expectedPackageDefinition.name, packageDefinition.name)
    assertEquals(expectedPackageDefinition.id, packageDefinition.id)
    assertEquals(expectedPackageDefinition.configuration, packageDefinition.configuration)
    assertEquals(expectedPackageDefinition.repository.id, packageDefinition.repository.id)
    assertEquals(expectedPackageDefinition.repository.name, packageDefinition.repository.name)
  }

  @Test
  void 'should deserialize package definition'() {
    def configurationProperties = new Configuration(new ConfigurationProperty(new ConfigurationKey('PACKAGE_NAME'), new ConfigurationValue('foo')))
    def packageDefinition = new PackageDefinition('package-id-1', 'package-1', configurationProperties)
    packageDefinition.setRepository(new PackageRepository('package-repo-id-1', 'package-repo-name-1', null, null))

    def actualJson = toObjectString({ PackageDefinitionRepresenter.toJSON(it, packageDefinition) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}