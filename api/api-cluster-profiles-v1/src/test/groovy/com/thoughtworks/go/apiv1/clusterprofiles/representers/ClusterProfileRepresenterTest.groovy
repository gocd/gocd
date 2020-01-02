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
package com.thoughtworks.go.apiv1.clusterprofiles.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.elastic.ClusterProfile
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore
import com.thoughtworks.go.plugin.api.info.PluginDescriptor
import com.thoughtworks.go.plugin.domain.common.Metadata
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ClusterProfileRepresenterTest {
  @Test
  void shouldCreateObjectFromJson() {
    def clusterProfile = [
      id        : 'docker',
      plugin_id : 'cd.go.docker',
      properties: [
        [
          "key"  : "DockerURI",
          "value": "http://foo"
        ]
      ]
    ]

    def expectedObject = new ClusterProfile('docker', 'cd.go.docker', create('DockerURI', false, 'http://foo'))

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(clusterProfile)
    def object = ClusterProfileRepresenter.fromJSON(jsonReader)

    Assertions.assertThat(object).isEqualTo(expectedObject)
  }

  @Test
  void shouldAddErrorsToJson() {
    def clusterProfile = new ClusterProfile('docker', 'cd.go.docker', create('DockerURI', false, 'http://foo'))
    clusterProfile.addError("pluginId", "Invalid Plugin Id")

    def expectedJson = [
      _links    : [
        self: [href: 'http://test.host/go/api/admin/elastic/cluster_profiles/docker'],
        doc : [href: apiDocsUrl('#cluster-profiles')],
        find: [href: 'http://test.host/go/api/admin/elastic/cluster_profiles/:cluster_id'],
      ],
      id        : 'docker',
      plugin_id : 'cd.go.docker',
      properties: [
        [
          "key"  : "DockerURI",
          "value": "http://foo"
        ]
      ],
      errors    : [
        "plugin_id": ["Invalid Plugin Id"]
      ]
    ]

    def json = toObjectString({ ClusterProfileRepresenter.toJSON(it, clusterProfile) })

    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void shouldEncryptSecureValues() {
    def clusterProfile = [
      id        : 'docker',
      plugin_id : 'cd.go.docker',
      properties: [
        [
          "key"  : "Password",
          "value": "passw0rd1"
        ]
      ]
    ]

    def elasticAgentMetadataStore = ElasticAgentMetadataStore.instance()
    PluggableInstanceSettings pluggableInstanceSettings = new PluggableInstanceSettings(Arrays.asList(
      new PluginConfiguration("Password", new Metadata(true, true))))
    elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(pluginDescriptor(), pluggableInstanceSettings, pluggableInstanceSettings, null, null, null))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(clusterProfile)

    def object = ClusterProfileRepresenter.fromJSON(jsonReader)

    Assertions.assertThat(object.getProperty("Password").isSecure()).isTrue()
  }

  private static PluginDescriptor pluginDescriptor() {
    return new PluginDescriptor() {
      @Override
      String id() {
        return "cd.go.docker"
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
