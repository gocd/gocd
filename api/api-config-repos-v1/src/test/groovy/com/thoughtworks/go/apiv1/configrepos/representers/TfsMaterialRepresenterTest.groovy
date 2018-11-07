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
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.util.command.UrlArgument
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

class TfsMaterialRepresenterTest {
  private static final UrlArgument REPO_URL = new UrlArgument("https://microsoft.com/chewbacca")
  private static final String DOMAIN = "the domain"
  private static final String USER = "user"
  private static final String PASSWORD = "it's secret!"
  private static final String PROJECT_PATH = "c:\\foo"
  private static final String ENCRYPTED_PASSWORD = "encrypted!"

  @Mock
  GoCipher cipher

  @BeforeEach
  void setup() { initMocks(this) }

  @Test
  void toJSON() {
    TfsMaterialConfig config = new TfsMaterialConfig(cipher, REPO_URL, null, DOMAIN, PROJECT_PATH)
    String json = toObjectString({ w -> TfsMaterialRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name        : null,
      url         : REPO_URL.toString(),
      project_path: PROJECT_PATH,
      domain      : DOMAIN,
      auto_update : true,
      username    : null
    ])
  }

  @Test
  void 'toJSON() with auth'() {
    when(cipher.encrypt(PASSWORD)).thenReturn(ENCRYPTED_PASSWORD)

    TfsMaterialConfig config = new TfsMaterialConfig(cipher, REPO_URL, USER, DOMAIN, PASSWORD, PROJECT_PATH)

    String json = toObjectString({ w -> TfsMaterialRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name              : null,
      url               : REPO_URL.toString(),
      project_path      : PROJECT_PATH,
      domain            : DOMAIN,
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
      name        : null,
      url         : REPO_URL.toString(),
      project_path: PROJECT_PATH,
      domain      : DOMAIN,
      auto_update : true,
      username    : USER,
      password    : PASSWORD
    ])

    TfsMaterialConfig expected = new TfsMaterialConfig(mock(GoCipher.class), REPO_URL, USER, DOMAIN, null, PROJECT_PATH)
    expected.setEncryptedPassword(ENCRYPTED_PASSWORD)

    assertEquals(expected, TfsMaterialRepresenter.fromJSON(json, mch))
  }
}