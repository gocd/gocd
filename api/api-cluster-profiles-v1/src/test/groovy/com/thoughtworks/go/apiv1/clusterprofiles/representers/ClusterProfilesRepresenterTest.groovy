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
package com.thoughtworks.go.apiv1.clusterprofiles.representers

import com.thoughtworks.go.config.elastic.ClusterProfile
import com.thoughtworks.go.config.elastic.ClusterProfiles
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ClusterProfilesRepresenterTest {
  def expectedJson = [
    _links   : [
      self: [href: 'http://test.host/go/api/admin/elastic/cluster_profiles'],
      doc : [href: apiDocsUrl('#cluster-profiles')],
    ],
    _embedded: [
      cluster_profiles: [
        [
          _links      : [
            self: [href: 'http://test.host/go/api/admin/elastic/cluster_profiles/docker'],
            doc : [href: apiDocsUrl('#cluster-profiles')],
            find: [href: 'http://test.host/go/api/admin/elastic/cluster_profiles/:cluster_id'],
          ],
          id          : 'docker',
          plugin_id   : 'cd.go.docker',
          "properties": [
            [
              "key"  : "docker-uri",
              "value": "unix:///var/run/docker"
            ]
          ],
        ],
        [
          _links      : [
            self: [href: 'http://test.host/go/api/admin/elastic/cluster_profiles/ecs'],
            doc : [href: apiDocsUrl('#cluster-profiles')],
            find: [href: 'http://test.host/go/api/admin/elastic/cluster_profiles/:cluster_id'],
          ],
          id          : 'ecs',
          plugin_id   : 'cd.go.ecs',
          "properties": [
            [
              "key"            : "ACCESS_KEY",
              "encrypted_value": new GoCipher().encrypt('encrypted-key')
            ]
          ],
        ]
      ]
    ]
  ]

  @Test
  void 'should serialize cluster profiles to json'() {
    def clusterProfiles = new ClusterProfiles(
      new ClusterProfile("docker", "cd.go.docker", create("docker-uri", false, "unix:///var/run/docker")),
      new ClusterProfile("ecs", "cd.go.ecs", create("ACCESS_KEY", true, "encrypted-key"))
    )

    def actualJson = toObjectString({ ClusterProfilesRepresenter.toJSON(it, clusterProfiles) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
