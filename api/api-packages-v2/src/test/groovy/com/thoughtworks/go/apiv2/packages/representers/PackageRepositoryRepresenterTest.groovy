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
package com.thoughtworks.go.apiv2.packages.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.domain.packagerepository.PackageRepository
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.*

class PackageRepositoryRepresenterTest {

  def expectedJson = [
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

  @Test
  void 'should deserialize package repository'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(expectedJson)
    def packageRepository = PackageRepositoryRepresenter.fromJSON(jsonReader)

    assertEquals('package-repo-id-1', packageRepository.id)
    assertEquals('package-repo-name-1', packageRepository.name)
  }

  @Test
  void 'should serialize package repository'() {
    def packageRepository = new PackageRepository('package-repo-id-1', 'package-repo-name-1', null, null)
    def actualJson = toObjectString({ PackageRepositoryRepresenter.toJSON(it, packageRepository) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
