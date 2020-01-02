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
package com.thoughtworks.go.apiv1.usersearch.representers

import com.thoughtworks.go.domain.User
import com.thoughtworks.go.presentation.UserSearchModel
import com.thoughtworks.go.presentation.UserSourceType
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class UserSearchRepresenterTest {
  @Test
  void 'should serialize'() {
    def user = new User("bob", "Bob", "bob@example.com")
    def actualJson = toObjectString({ UserSearchRepresenter.toJSON(it, new UserSearchModel(user, UserSourceType.PLUGIN)) })

    Map<String, Object> expectedJson = [
      _links      : [
        self        : [href: 'http://test.host/go/api/users/bob'],
        find        : [href: 'http://test.host/go/api/users/:login_name'],
        doc         : [href: apiDocsUrl('#users')],
        current_user: [href: 'http://test.host/go/api/current_user'],
      ],
      login_name  : 'bob',
      display_name: 'Bob',
      email       : 'bob@example.com',
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
