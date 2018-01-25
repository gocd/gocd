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

package com.thoughtworks.go.apiv1.admin.encryption.representers

import com.thoughtworks.go.spark.mocks.TestRequestContext
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class EncryptedValueRepresenterTest {

  @Test
  void 'should serialize'() {
    def actual = EncryptedValueRepresenter.toJSON("foo", new TestRequestContext())

    LinkedHashMap<String, Object> expected = [
      _links         : [
        doc : [href: 'https://api.gocd.org/#encryption'],
        self: [href: 'http://test.host/go/api/admin/encrypt']]
      ,
      encrypted_value: 'foo'
    ]

    assertThatJson(actual).isEqualTo(expected)
  }
}
