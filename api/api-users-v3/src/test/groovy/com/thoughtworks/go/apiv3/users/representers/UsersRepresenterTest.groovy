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

import com.thoughtworks.go.apiv3.users.UsersMother
import com.thoughtworks.go.apiv3.users.model.UserToRepresent
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.RoleConfig
import com.thoughtworks.go.config.RolesConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class UsersRepresenterTest {

  @Test
  void 'render a list of users'() {
    def gocdRole = new RoleConfig()
    gocdRole.setName(new CaseInsensitiveString("gocdRole"))

    def users = Arrays.asList(UserToRepresent.from(UsersMother.withName("Bob"), true, new RolesConfig(gocdRole)))
    def json = toObject({ UsersRepresenter.toJSON(it, users) })

    def expectedJson = [
      _links   : [
        self: [href: 'http://test.host/go/api/users'],
        doc : [href: apiDocsUrl('#users')]
      ],
      _embedded: [
        users: [
          [
            login_name     : "Bob",
            display_name   : "Bob",
            enabled        : true,
            email_me       : true,
            is_admin       : true,
            email          : "Bob@no-reply.com",
            roles          : [
              [
                name: 'gocdRole',
                type: 'gocd',
              ]
            ],
            checkin_aliases: ['Bob', 'awesome']
          ]
        ]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }
}
