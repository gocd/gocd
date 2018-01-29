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

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.ArtifactStore
import com.thoughtworks.go.spark.mocks.TestRequestContext
import org.junit.Test

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static org.assertj.core.api.Assertions.assertThat

class ArtifactStoreRepresenterTest {
  def dockerArtifactStore = [
    "_links"    : [
      "doc" : ["href": "https://api.gocd.org/#artifact-stores"],
      "find": ["href": "http://test.host/go/api/admin/artifact_stores/:storeId"],
      "self": ["href": "http://test.host/go/api/admin/artifact_stores/docker"]
    ], "id"     : "docker", "plugin_id": "cd.go.docker",
    "properties": [["key": "Username", "value": "admin"]]
  ]

  @Test
  void 'should serialize to json'() {
    def artifactStore = new ArtifactStore("docker", "cd.go.docker", create("Username", false, "admin"))

    def map = ArtifactStoreRepresenter.toJSON(artifactStore, new TestRequestContext())

    assertThat(map).isEqualTo(dockerArtifactStore)
  }

  @Test
  void 'should deserialize json to artifact stores'() {
    def artifactStore = new ArtifactStore("docker", "cd.go.docker", create("Username", false, "admin"))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(dockerArtifactStore)
    def deserializedArtifactStore = ArtifactStoreRepresenter.fromJSON(jsonReader)
    assertThat(deserializedArtifactStore).isEqualTo(artifactStore)
  }
}