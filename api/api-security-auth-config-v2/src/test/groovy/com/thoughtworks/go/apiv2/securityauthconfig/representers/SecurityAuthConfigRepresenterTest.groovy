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
package com.thoughtworks.go.apiv2.securityauthconfig.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.SecurityAuthConfig
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.spark.Routes
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static org.assertj.core.api.Java6Assertions.assertThat
import static org.assertj.core.api.Java6Assertions.assertThatCode

class SecurityAuthConfigRepresenterTest {
  @Test
  void "should convert from json to auth config"() {
    def id = 'file'
    def pluginId = "cd.go.authorization.file"
    def property = ConfigurationPropertyMother.create("Path", "/var/config/pass.prop")

    def jsonReader = GsonTransformer.getInstance().jsonReaderFrom(authConfigJson(id, pluginId, true))

    def authConfig = SecurityAuthConfigRepresenter.fromJSON(jsonReader)

    def expected = new SecurityAuthConfig(id, pluginId, property)
    expected.setAllowOnlyKnownUsersToLogin(true)

    assertThat(authConfig).isEqualTo(expected)
  }

  @Test
  void "should not bomb when id is missing"() {
    def id = null
    def pluginId = "cd.go.authorization.file"

    def jsonReader = GsonTransformer.getInstance().jsonReaderFrom(authConfigJson(id, pluginId))

    assertThatCode({ SecurityAuthConfigRepresenter.fromJSON(jsonReader) })
      .doesNotThrowAnyException()
  }

  @Test
  void "should convert to json from auth config"() {
    def id = 'file'
    def pluginId = "cd.go.authorization.file"
    def property = ConfigurationPropertyMother.create("Path", "/var/config/pass.prop")
    def authConfig = new SecurityAuthConfig(id, pluginId, property)

    def json = toObject({ SecurityAuthConfigRepresenter.toJSON(it, authConfig) })

    assertThat(json)
      .isEqualTo(authConfigJson(id, pluginId))
  }

  private static LinkedHashMap<String, Object> authConfigJson(String id, String pluginId, boolean allowOnlyKnownUsersToLogin = false) {
    [
      "_links"                         : [
        "self": [
          "href": "http://test.host/go/api/admin/security/auth_configs/$id".toString()
        ],
        "doc" : [
          "href": Routes.SecurityAuthConfigAPI.DOC
        ],
        "find": [
          "href": "http://test.host/go/api/admin/security/auth_configs/:id"
        ]
      ],
      "id"                             : id,
      "plugin_id"                      : pluginId,
      "allow_only_known_users_to_login": allowOnlyKnownUsersToLogin,
      "properties"                     : [
        [
          "key"  : "Path",
          "value": "/var/config/pass.prop"
        ]
      ]
    ]
  }

}
