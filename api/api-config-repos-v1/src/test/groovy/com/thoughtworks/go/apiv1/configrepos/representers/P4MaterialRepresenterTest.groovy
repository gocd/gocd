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
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class P4MaterialRepresenterTest {
  private static final String REPO_URL = "https://peeforce.com/chewbacca"
  private static final String VIEW = "myView"
  private static final String PASSWORD = "foo"
  private static final String ENCRYPTED_PASSWORD = "secret-foo"

  @Test
  void toJSON() {
    P4MaterialConfig config = new P4MaterialConfig(REPO_URL, VIEW)
    String json = toObjectString({ w -> P4MaterialRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name       : null,
      port       : REPO_URL,
      view       : VIEW,
      use_tickets: false,
      auto_update: true,
      username   : null
    ])
  }

  @Test
  void 'toJSON() with auth'() {
    P4MaterialConfig config = new P4MaterialConfig(REPO_URL, VIEW)
    config.setEncryptedPassword(ENCRYPTED_PASSWORD)
    config.setUserName("user")

    String json = toObjectString({ w -> P4MaterialRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name              : null,
      port              : REPO_URL,
      view              : VIEW,
      use_tickets       : false,
      auto_update       : true,
      username          : "user",
      encrypted_password: ENCRYPTED_PASSWORD
    ])
  }

  @Test
  void fromJSON() {
    PasswordDeserializer pd = mock(PasswordDeserializer.class)
    when(pd.deserialize(eq(PASSWORD), eq(null as String), any() as AbstractMaterialConfig)).thenReturn(ENCRYPTED_PASSWORD)
    MaterialConfigHelper mch = new MaterialConfigHelper(pd)

    JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
      name       : null,
      port       : REPO_URL,
      view       : VIEW,
      use_tickets: false,
      auto_update: true,
      username   : "user",
      password   : PASSWORD
    ])

    MaterialConfig materialConfig = P4MaterialRepresenter.fromJSON(json, mch)
    P4MaterialConfig expected = new P4MaterialConfig(REPO_URL, VIEW)
    expected.setEncryptedPassword(ENCRYPTED_PASSWORD)
    expected.setUserName("user")

    assertEquals(expected, materialConfig)
  }
}