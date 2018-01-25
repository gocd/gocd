/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.spark.mocks.TestRequestContext
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class UserSummaryRepresenterTest {

  @Test
  void 'should serialize'() {
    Map actualJson = UserSummaryRepresenter.toJSON("jdoe", new TestRequestContext())

    assertThat(actualJson).isEqualTo([
      _links    : [
        self        : [href: 'http://test.host/api/users/jdoe'],
        find        : [href: 'http://test.host/api/users/:login_name'],
        doc         : [href: 'https://api.gocd.org/#users'],
        current_user: [href: 'http://test.host/api/current_user'],
      ],
      login_name: 'jdoe'
    ])
  }

}
