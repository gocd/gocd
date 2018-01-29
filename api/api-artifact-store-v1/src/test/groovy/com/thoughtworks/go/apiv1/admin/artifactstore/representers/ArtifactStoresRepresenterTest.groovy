/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.admin.artifactstore.representers

import com.thoughtworks.go.config.ArtifactStore
import com.thoughtworks.go.config.ArtifactStores
import com.thoughtworks.go.spark.mocks.TestRequestContext
import org.junit.Test

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static org.assertj.core.api.Assertions.assertThat

class ArtifactStoresRepresenterTest {
  def s3ArtifactStore = [
    "_links"    : [
      "doc" : ["href": "https://api.gocd.org/#artifact-stores"],
      "find": ["href": "http://test.host/go/api/admin/artifact_stores/:storeId"],
      "self": ["href": "http://test.host/go/api/admin/artifact_stores/s3"]
    ],
    "id"        : "s3",
    "plugin_id" : "cd.go.s3",
    "properties": [["encrypted_value": "psVH+oJmIgirBOn/7YLrWQ==", "key": "AccessKey"]]
  ]


  def dockerArtifactStore = [
    "_links"    : [
      "doc" : ["href": "https://api.gocd.org/#artifact-stores"],
      "find": ["href": "http://test.host/go/api/admin/artifact_stores/:storeId"],
      "self": ["href": "http://test.host/go/api/admin/artifact_stores/docker"]
    ],
    "id"        : "docker",
    "plugin_id" : "cd.go.docker",
    "properties": [["key": "Username", "value": "admin"]]
  ]

  private final LinkedHashMap<String, Object> allArtifactStoresJSON = [
    "_links"   : [
      "doc" : ["href": "https://api.gocd.org/#artifact-stores"],
      "find": ["href": "http://test.host/go/api/admin/artifact_stores/:storeId"],
      "self": ["href": "http://test.host/go/api/admin/artifact_stores"]
    ],
    "_embedded": [
      "artifact_stores": [dockerArtifactStore, s3ArtifactStore]
    ]
  ]

  @Test
  void 'should generate json'() {
    def artifactStores = new ArtifactStores(
      new ArtifactStore("docker", "cd.go.docker", create("Username", false, "admin")),
      new ArtifactStore("s3", "cd.go.s3", create("AccessKey", true, "some-key"))
    )

    def map = ArtifactStoresRepresenter.toJSON(artifactStores, new TestRequestContext())
    assertThat(map).isEqualTo(allArtifactStoresJSON)
  }
}