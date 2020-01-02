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
package com.thoughtworks.go.apiv1.packages.representers

import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.PackageDefinition
import com.thoughtworks.go.domain.packagerepository.PackageRepository
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PackageDefinitionsRepresenterTest {

  def expectedJson = [
    _links   : [
      self: [
        href: "http://test.host/go/api/admin/packages"
      ],
      doc : [
        href: apiDocsUrl("#packages")
      ]
    ],
    _embedded: [
      "packages": [
        [
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
        ],
        [
          _links       : [
            self: [
              href: "http://test.host/go/api/admin/packages/package-id-2"
            ],
            doc : [
              href: apiDocsUrl("#packages")
            ],
            find: [
              href: "http://test.host/go/api/admin/packages/:package_id"
            ]
          ],
          name         : "package-2",
          id           : "package-id-2",
          auto_update  : false,
          configuration: [
            [
              "key"  : "PACKAGE_NAME",
              "value": "bar"
            ]
          ],
          package_repo : [
            _links: [
              self: [
                href: "http://test.host/go/api/admin/repositories/package-repo-id-2"
              ],
              doc : [
                href: apiDocsUrl("#package-repositories")
              ],
              find: [
                href: "http://test.host/go/api/admin/repositories/:repo_id"
              ]
            ],
            id    : "package-repo-id-2",
            name  : "package-repo-name-2"
          ]
        ]
      ]
    ]
  ]

  @Test
  void 'should generate JSON'() {
    def config1 = new Configuration()
    config1.addNewConfigurationWithValue("PACKAGE_NAME", "foo", false)
    def package1 = new PackageDefinition("package-id-1", "package-1", config1)
    package1.repository = new PackageRepository("package-repo-id-1", "package-repo-name-1", null, null)

    def config2 = new Configuration()
    config2.addNewConfigurationWithValue("PACKAGE_NAME", "bar", false)
    def package2 = new PackageDefinition("package-id-2", "package-2", config2)
    package2.autoUpdate = false
    package2.repository = new PackageRepository("package-repo-id-2", "package-repo-name-2", null, null)

    def packages = [package1, package2]

    def actualJson = toObjectString({ PackageDefinitionsRepresenter.toJSON(it, packages) })
    assertThatJson(actualJson).isEqualTo(this.expectedJson)
  }
}
