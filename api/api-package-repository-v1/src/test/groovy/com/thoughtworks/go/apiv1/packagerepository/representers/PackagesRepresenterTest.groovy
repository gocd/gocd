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

import com.thoughtworks.go.domain.packagerepository.PackageDefinition
import com.thoughtworks.go.domain.packagerepository.Packages
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PackagesRepresenterTest {

  def expectedJson = [
    [
      _links: [
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
      name  : "package-name-1",
      id    : "package-id-1"
    ]
  ]

  @Test
  void 'should serialize packages'() {
    def packageDefinition = new PackageDefinition('package-id-1', 'package-name-1', null)
    def packages = new Packages(packageDefinition)
    def actualJson = toArrayString({ PackagesRepresenter.toJSON(it, packages) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
