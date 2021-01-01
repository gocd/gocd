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
package com.thoughtworks.go.apiv1.admin.encryption.representers

import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class EncryptedValueRepresenterTest {

  @Test
  void 'should serialize'() {
    def actualJson = toObjectString({ EncryptedValueRepresenter.toJSON(it, "foo") })

    LinkedHashMap<String, Object> expected = [
      _links         : [
        doc : [href: apiDocsUrl('#encryption')],
        self: [href: 'http://test.host/go/api/admin/encrypt']]
      ,
      encrypted_value: 'foo'
    ]

    assertThatJson(actualJson).isEqualTo(expected)
  }
}
