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
package com.thoughtworks.go.apiv1.user.representers

import com.thoughtworks.go.domain.User
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class UserRepresenterTest {

  @Test
  void 'should serialize'() {

    def actualJson = toObjectString({
      UserRepresenter.toJSON(it, new User('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'] as String[], 'jdoe@example.com', true))
    })

    assertThatJson(actualJson).isEqualTo([
      _links         : [
        self        : [href: 'http://test.host/go/api/users/jdoe'],
        find        : [href: 'http://test.host/go/api/users/:login_name'],
        doc         : [href: apiDocsUrl('#users')],
        current_user: [href: 'http://test.host/go/api/current_user'],
      ],
      login_name     : 'jdoe',
      checkin_aliases: ['jdoe', 'jdoe@example.com'],
      display_name   : 'Jon Doe',
      email          : 'jdoe@example.com',
      enabled        : true,
      email_me       : true
    ])
  }
}
