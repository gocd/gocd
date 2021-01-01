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

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.ArtifactStore
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore
import com.thoughtworks.go.plugin.api.info.PluginDescriptor
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo
import com.thoughtworks.go.plugin.domain.common.Metadata
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class ArtifactStoreRepresenterTest {

  @Test
  void shouldCreateObjectFromJson() {
    def artifactStore = [
      id        : 'docker',
      plugin_id : 'cd.go.artifact.docker',
      properties: [
        [
          "key"  : "RegistryURL",
          "value": "http://foo"
        ]
      ]
    ]
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(artifactStore)
    def expectedObject = new ArtifactStore('docker', 'cd.go.artifact.docker',
      ConfigurationPropertyMother.create('RegistryURL', false, 'http://foo'))

    def object = ArtifactStoreRepresenter.fromJSON(jsonReader)

    assertThat(object).isEqualTo(expectedObject)
  }

  @Test
  void shouldAddErrorsToJson() {
    def artifactStore = new ArtifactStore('docker', 'cd.go.artifact.docker',
      ConfigurationPropertyMother.create('RegistryURL', false, 'http://foo'))
    artifactStore.addError("pluginId", "Invalid Plugin Id")
    def expectedJson = [
      _links    : [
        self: [href: 'http://test.host/go/api/admin/artifact_stores/docker'],
        doc : [href: apiDocsUrl('#artifact-store')],
        find: [href: 'http://test.host/go/api/admin/artifact_stores/:id'],
      ],
      id        : 'docker',
      plugin_id : 'cd.go.artifact.docker',
      properties: [
        [
          "key"  : "RegistryURL",
          "value": "http://foo"
        ]
      ],
      errors    : [
        "plugin_id": ["Invalid Plugin Id"]
      ]
    ]

    def json = toObjectString({ ArtifactStoreRepresenter.toJSON(it, artifactStore) })

    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void shouldEncryptSecureValues(){
    def artifactStore = [
      id        : 'docker',
      plugin_id : 'cd.go.artifact.docker',
      properties: [
        [
          "key"  : "Password",
          "value": "passw0rd1"
        ]
      ]
    ]
    def artifactMetadataStore = ArtifactMetadataStore.instance()
    PluggableInstanceSettings pluggableInstanceSettings = new PluggableInstanceSettings(Arrays.asList(
      new PluginConfiguration("Password", new Metadata(true, true))))
    artifactMetadataStore.setPluginInfo(new ArtifactPluginInfo(pluginDescriptor(), pluggableInstanceSettings, null, null, null, null))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(artifactStore)

    def object = ArtifactStoreRepresenter.fromJSON(jsonReader)

    assertThat(object.getProperty("Password").isSecure()).isTrue()
  }

  private static PluginDescriptor pluginDescriptor() {
    return new PluginDescriptor() {
      @Override
      String id() {
        return "cd.go.artifact.docker"
      }

      @Override
      String version() {
        return null
      }

      @Override
      PluginDescriptor.About about() {
        return null
      }
    }
  }
}
