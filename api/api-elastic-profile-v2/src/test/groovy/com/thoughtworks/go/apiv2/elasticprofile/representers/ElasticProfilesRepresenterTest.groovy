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
package com.thoughtworks.go.apiv2.elasticprofile.representers

import com.thoughtworks.go.config.elastic.ElasticProfile
import com.thoughtworks.go.config.elastic.ElasticProfiles
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ElasticProfilesRepresenterTest {
  def expectedJson = [
    _links   : [
      self: [href: 'http://test.host/go/api/elastic/profiles'],
      doc : [href: apiDocsUrl('#elastic-agent-profiles')],
      find: [href: 'http://test.host/go/api/elastic/profiles/:profile_id'],
    ],
    _embedded: [
      profiles: [
        [
          _links            : [
            self: [href: 'http://test.host/go/api/elastic/profiles/docker'],
            doc : [href: apiDocsUrl('#elastic-agent-profiles')],
            find: [href: 'http://test.host/go/api/elastic/profiles/:profile_id'],
          ],
          id                : 'docker',
          cluster_profile_id: 'foo',
          "properties"      : [
            [
              "key"  : "docker-uri",
              "value": "unix:///var/run/docker"
            ]
          ],
        ],
        [
          _links            : [
            self: [href: 'http://test.host/go/api/elastic/profiles/ecs'],
            doc : [href: apiDocsUrl('#elastic-agent-profiles')],
            find: [href: 'http://test.host/go/api/elastic/profiles/:profile_id'],
          ],
          id                : 'ecs',
          cluster_profile_id: "bar",
          "properties"      : [
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
  void 'should serialize elastic profiles to json'() {
    def elasticProfiles = new ElasticProfiles(
      new ElasticProfile("docker", 'foo', create("docker-uri", false, "unix:///var/run/docker")),
      new ElasticProfile("ecs", "bar", create("ACCESS_KEY", true, "encrypted-key"))
    )

    def actualJson = toObjectString({ ElasticProfilesRepresenter.toJSON(it, elasticProfiles) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
