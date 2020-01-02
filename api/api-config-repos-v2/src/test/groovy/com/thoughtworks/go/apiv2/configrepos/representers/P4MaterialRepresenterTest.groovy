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
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.p4
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class P4MaterialRepresenterTest {
  private static final String REPO_URL = "https://peeforce.com/chewbacca"
  private static final String VIEW = "myView"
  private static final String PASSWORD = "foo"

  @Test
  void toJSON() {
    P4MaterialConfig config = p4(REPO_URL, VIEW)
    String json = toObjectString({ w -> new P4MaterialRepresenter().toJSON(w, config) })

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
    P4MaterialConfig config = p4(REPO_URL, VIEW)
    config.setUserName("user")
    config.setPassword(PASSWORD)

    String json = toObjectString({ w -> new P4MaterialRepresenter().toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name              : null,
      port              : REPO_URL,
      view              : VIEW,
      use_tickets       : false,
      auto_update       : true,
      username          : "user",
      encrypted_password: new GoCipher().encrypt(PASSWORD)
    ])
  }

  @Test
  void fromJSON() {
    JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
      name       : null,
      port       : REPO_URL,
      view       : VIEW,
      use_tickets: false,
      auto_update: true,
      username   : "user",
      password   : PASSWORD
    ])

    def goCipher = new GoCipher()
    MaterialConfig materialConfig = new P4MaterialRepresenter().fromJSON(json)
    P4MaterialConfig expected = p4(REPO_URL, VIEW)
    expected.setUserName("user")
    expected.setEncryptedPassword(goCipher.encrypt(PASSWORD))

    assertEquals(expected, materialConfig)
    assertTrue(goCipher.passwordEquals(expected.encryptedPassword, materialConfig.encryptedPassword))
  }
}
