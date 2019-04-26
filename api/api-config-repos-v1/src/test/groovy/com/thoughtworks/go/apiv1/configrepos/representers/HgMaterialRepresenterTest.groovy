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

package com.thoughtworks.go.apiv1.configrepos.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class HgMaterialRepresenterTest {
  private static final String REPO_URL = "http://username:password@mydomain.com/myproject"

  @Test
  void toJSON() {
    HgMaterialConfig config = new HgMaterialConfig(REPO_URL, null)
    String json = toObjectString({ w -> HgMaterialRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name       : null,
      url        : REPO_URL.replace('password', '******'),
      auto_update: true
    ])
  }

  @Nested
  class fromJSON {
    @Test
    void shouldConvertToMaterialObject() {
      JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
        name      : null,
        url       : REPO_URL,
        auto_upate: true
      ])

      MaterialConfig expected = new HgMaterialConfig(REPO_URL, null)
      assertEquals(expected, HgMaterialRepresenter.fromJSON(json))
    }

    @Test
    void shouldHandleNullMaterialUrl() {
      JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
        name      : null,
        url       : null,
        auto_upate: true
      ])

      MaterialConfig expected = new HgMaterialConfig()
      assertEquals(expected, HgMaterialRepresenter.fromJSON(json))
    }

    @Test
    void shouldHandleIfUrlFieldIsMissingInJson() {
      JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
        name      : null,
        auto_upate: true
      ])

      MaterialConfig expected = new HgMaterialConfig()
      assertEquals(expected, HgMaterialRepresenter.fromJSON(json))
    }
  }
}
