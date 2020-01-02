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
package com.thoughtworks.go.apiv2.elasticprofile.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.elastic.ElasticProfile
import com.thoughtworks.go.plugin.api.info.PluginDescriptor
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.HaltException

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assertions.assertThrows

@Nested
class ElasticProfileRepresenterTest {

  @Nested
  class WithClusterProfileId {
    @Test
    void shouldCreateObjectFromJson() {
      def elasticProfile = [
        id                : 'docker',
        cluster_profile_id: 'foo',
        properties        : [
          [
            "key"  : "DockerURI",
            "value": "http://foo"
          ]
        ]
      ]

      def expectedObject = new ElasticProfile('docker', 'foo', create('DockerURI', false, 'http://foo'))

      def jsonReader = GsonTransformer.instance.jsonReaderFrom(elasticProfile)
      def object = ElasticProfileRepresenter.fromJSON(jsonReader)

      assertThat(object).isEqualTo(expectedObject)
    }

    @Test
    void shouldAddErrorsToJson() {
      def elasticProfile = new ElasticProfile('docker', 'foo', create('DockerURI', false, 'http://foo'))
      elasticProfile.addError("pluginId", "Invalid Plugin Id")

      def expectedJson = [
        _links            : [
          self: [href: 'http://test.host/go/api/elastic/profiles/docker'],
          doc : [href: apiDocsUrl('#elastic-agent-profiles')],
          find: [href: 'http://test.host/go/api/elastic/profiles/:profile_id'],
        ],
        id                : 'docker',
        cluster_profile_id: 'foo',
        properties        : [
          [
            "key"  : "DockerURI",
            "value": "http://foo"
          ]
        ],
        errors            : [
          "plugin_id": ["Invalid Plugin Id"]
        ]
      ]

      def json = toObjectString({ ElasticProfileRepresenter.toJSON(it, elasticProfile) })

      assertThatJson(json).isEqualTo(expectedJson)
    }

    @Nested
    class WithoutClusterProfileId {
      @Test
      void shouldThrowHaltExceptionForNotSpecifyingClusterProfileId() {
        def elasticProfile = [
          id        : 'docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]
        ]

        def jsonReader = GsonTransformer.instance.jsonReaderFrom(elasticProfile)
        assertThrows(HaltException.class, { ElasticProfileRepresenter.fromJSON(jsonReader) })
      }
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
}
