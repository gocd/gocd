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
package com.thoughtworks.go.apiv1.artifactstoreconfig.representers

import com.thoughtworks.go.config.ArtifactStore
import com.thoughtworks.go.config.ArtifactStores
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ArtifactStoresRepresenterTest {

  private final LinkedHashMap<Object, Object> expectedJson = [
    _links   : [
      self: [href: 'http://test.host/go/api/admin/artifact_stores'],
      doc : [href: apiDocsUrl('#artifact-store')],
      find: [href: 'http://test.host/go/api/admin/artifact_stores/:id'],
    ],
    _embedded: [
      artifact_stores: [
        [
          _links      : [
            self: [href: 'http://test.host/go/api/admin/artifact_stores/docker'],
            doc : [href: apiDocsUrl('#artifact-store')],
            find: [href: 'http://test.host/go/api/admin/artifact_stores/:id'],
          ],
          id          : 'docker',
          plugin_id   : 'cd.go.artifact.docker',
          "properties": [
            [
              "key"  : "RegistryURL",
              "value": "http://foo"
            ]
          ],
        ]
      ]
    ]
  ]

  @Test
  void shouldGenerateJSON() {
    def stores = new ArtifactStores(new ArtifactStore("docker", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo")))
    def actualJson = toObjectString({ ArtifactStoresRepresenter.toJSON(it, stores) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
