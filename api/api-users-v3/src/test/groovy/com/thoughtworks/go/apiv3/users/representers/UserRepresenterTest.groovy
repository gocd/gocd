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
package com.thoughtworks.go.apiv3.users.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv3.users.model.UserToRepresent
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PluginRoleConfig
import com.thoughtworks.go.config.RoleConfig
import com.thoughtworks.go.config.RolesConfig
import com.thoughtworks.go.domain.User
import com.thoughtworks.go.helper.UsersMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class UserRepresenterTest {

  @Test
  void 'renders a user'() {
    def user = UsersMother.withName("Bob")
    def gocdRole = new RoleConfig()
    gocdRole.setName(new CaseInsensitiveString("gocdRole"))
    def pluginRole = new PluginRoleConfig("pluginRole", "auth-config-1")

    def json = toObject({
      UserRepresenter.toJSON(it, UserToRepresent.from(user, false, new RolesConfig(gocdRole, pluginRole)))
    })

    def expectedJson = [
      _links         : [
        self: [href: 'http://test.host/go/api/users/Bob'],
        find: [href: 'http://test.host/go/api/users/:login_name'],
        doc : [href: apiDocsUrl('#users')]
      ],
      login_name     : "Bob",
      display_name   : "Bob",
      enabled        : true,
      email_me       : true,
      is_admin       : false,
      email          : "Bob@no-reply.com",
      roles          : [
        [
          name: 'gocdRole',
          type: 'gocd'
        ],
        [
          name      : 'pluginRole',
          type      : 'plugin',
          attributes: [
            auth_config_id: 'auth-config-1'
          ]
        ]
      ],
      checkin_aliases: ['Bob', 'awesome']
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'should deserialize from JSON'() {
    def isLoginNameOptional = false
    def json = [
      login_name     : "Bob",
      display_name   : "Bob",
      enabled        : true,
      email_me       : true,
      email          : "Bob@no-reply.com",
      checkin_aliases: ['awesome', 'Bob']
    ]

    User expectedUser = UsersMother.withName("Bob")
    JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(json)

    assertThat(expectedUser).isEqualTo(UserRepresenter.fromJSON(jsonReader, isLoginNameOptional))
  }

  @Test
  void 'should create a user when provided just the login_name'() {
    def isLoginNameOptional = false

    def username = "John"
    def json = [
      login_name: username,
    ]

    User expectedUser = new User(username)
    expectedUser.setDisplayName(username)
    expectedUser.enable()

    JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(json)
    assertThat(expectedUser).isEqualTo(UserRepresenter.fromJSON(jsonReader, isLoginNameOptional))
  }

  @Test
  void 'should deserialize user when no login_name is provided'() {
    def isLoginNameOptional = true

    def emailAddress = 'my-email@no-reply.com'
    def json = [
      email   : emailAddress,
      email_me: true
    ]

    User expectedUser = new User()
    expectedUser.enable()
    expectedUser.setEmail(emailAddress)
    expectedUser.setEmailMe(true)
    expectedUser.setMatcher("")

    JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(json)
    assertThat(expectedUser, is(UserRepresenter.fromJSON(jsonReader, isLoginNameOptional)))
  }
}
