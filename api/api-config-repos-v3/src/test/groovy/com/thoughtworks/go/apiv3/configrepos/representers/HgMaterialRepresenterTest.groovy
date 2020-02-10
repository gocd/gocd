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
package com.thoughtworks.go.apiv3.configrepos.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.hg
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class HgMaterialRepresenterTest {
  private static final String REPO_URL = "http://mydomain.com/myproject"

  @Nested
  class ToJSON {
    @Test
    void shouldSerializeObjectToJson() {
      HgMaterialConfig config = hg(REPO_URL, null)
      config.setUserName("bob")
      config.setPassword("password")
      config.setBranchAttribute("feature")
      String json = toObjectString({ w -> new HgMaterialRepresenter().toJSON(w, config) })

      assertThatJson(json).isEqualTo([
        name              : null,
        url               : REPO_URL,
        username          : "bob",
        encrypted_password: new GoCipher().encrypt("password"),
        auto_update       : true,
        branch            : "feature"
      ])
    }
  }

  @Nested
  class FromJson {
    @Test
    void shouldDeserializeJsonToObject() {
      JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
        name      : "Test",
        url       : REPO_URL,
        auto_upate: true,
        username  : "bob",
        password  : "some-pass",
        branch    : "feature"
      ])

      def materialConfig = new HgMaterialRepresenter().fromJSON(json)
      assertThat(materialConfig.getName()).isEqualTo(new CaseInsensitiveString("Test"))
      assertThat(materialConfig.getUrl()).isEqualTo(REPO_URL)
      assertThat(materialConfig.getAutoUpdate()).isTrue()
      assertThat(materialConfig.getUserName()).isEqualTo("bob")
      assertThat(materialConfig.getPassword()).isEqualTo("some-pass")
      assertThat(materialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("some-pass"))
      assertThat(materialConfig.getBranchAttribute()).isEqualTo("feature")
    }
  }
}
