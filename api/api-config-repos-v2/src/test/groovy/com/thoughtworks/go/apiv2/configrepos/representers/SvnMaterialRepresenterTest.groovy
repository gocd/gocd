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
package com.thoughtworks.go.apiv2.configrepos.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig
import static com.thoughtworks.go.helper.MaterialConfigsMother.svn
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.mockito.MockitoAnnotations.initMocks

class SvnMaterialRepresenterTest {
  private static final String REPO_URL = "svn+ssh://username:password@10.106.191.164/home/svn/shproject"
  private static final String USER = "user"
  private static final String PASSWORD = "it's secret!"

  @BeforeEach
  void setup() { initMocks(this) }

  @Test
  void toJSON() {
    SvnMaterialConfig config = svn(REPO_URL, true)
    String json = toObjectString({ w -> new SvnMaterialRepresenter().toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name           : null,
      url            : REPO_URL.replace('password', '******'),
      check_externals: true,
      auto_update    : true,
      username       : null
    ])
  }

  @Test
  void 'toJSON() with auth'() {
    def cipher = new GoCipher()
    SvnMaterialConfig config = svn(REPO_URL, USER, PASSWORD, false, cipher)

    String json = toObjectString({ w -> new SvnMaterialRepresenter().toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name              : null,
      url               : REPO_URL.replace('password', '******'),
      check_externals   : false,
      auto_update       : true,
      username          : USER,
      encrypted_password: cipher.encrypt(PASSWORD)
    ])
  }

  @Test
  void fromJSON() {
    JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
      name           : null,
      url            : REPO_URL,
      check_externals: false,
      auto_update    : true,
      username       : USER,
      password       : PASSWORD
    ])

    MaterialConfig materialConfig = new SvnMaterialRepresenter().fromJSON(json)
    def goCipher = new GoCipher()
    SvnMaterialConfig expected = svn(REPO_URL, USER, PASSWORD, false, goCipher)

    assertEquals(expected, materialConfig)
    assertTrue(goCipher.passwordEquals(expected.encryptedPassword, materialConfig.encryptedPassword))
  }
}
