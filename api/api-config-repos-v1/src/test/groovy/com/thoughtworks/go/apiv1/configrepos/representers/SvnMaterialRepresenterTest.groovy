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
import com.thoughtworks.go.config.materials.AbstractMaterialConfig
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class SvnMaterialRepresenterTest {
  private static final String REPO_URL = "https://dontusesvn.com/chewbacca"
  private static final String USER = "user"
  private static final String PASSWORD = "it's secret!"
  private static final String ENCRYPTED_PASSWORD = "encrypted!"

  @Mock
  GoCipher cipher

  @BeforeEach
  void setup() { initMocks(this) }

  @Test
  void toJSON() {
    SvnMaterialConfig config = new SvnMaterialConfig(REPO_URL, true)
    String json = toObjectString({ w -> SvnMaterialRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name           : null,
      url            : REPO_URL,
      check_externals: true,
      auto_update    : true,
      username       : null
    ])
  }

  @Test
  void 'toJSON() with auth'() {
    when(cipher.encrypt(PASSWORD)).thenReturn(ENCRYPTED_PASSWORD)
    SvnMaterialConfig config = new SvnMaterialConfig(REPO_URL, USER, PASSWORD, false, cipher)

    String json = toObjectString({ w -> SvnMaterialRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name              : null,
      url               : REPO_URL,
      check_externals   : false,
      auto_update       : true,
      username          : USER,
      encrypted_password: ENCRYPTED_PASSWORD
    ])
  }

  @Test
  void fromJSON() {
    PasswordDeserializer pd = mock(PasswordDeserializer.class)
    when(pd.deserialize(eq(PASSWORD), eq(null as String), any() as AbstractMaterialConfig)).thenReturn(ENCRYPTED_PASSWORD)
    MaterialConfigHelper mch = new MaterialConfigHelper(pd)

    JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
      name           : null,
      url            : REPO_URL,
      check_externals: false,
      auto_update    : true,
      username       : USER,
      password       : PASSWORD
    ])

    MaterialConfig materialConfig = SvnMaterialRepresenter.fromJSON(json, mch)
    SvnMaterialConfig expected = new SvnMaterialConfig(REPO_URL, USER, null, false, cipher)
    expected.setEncryptedPassword(ENCRYPTED_PASSWORD)

    assertEquals(expected, materialConfig)
  }
}