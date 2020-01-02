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
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.util.command.UrlArgument
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.tfs
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class TfsMaterialRepresenterTest {
  private static final UrlArgument REPO_URL = new UrlArgument("https://microsoft.com/chewbacca")
  private static final String DOMAIN = "the domain"
  private static final String USER = "user"
  private static final String PASSWORD = "it's secret!"
  private static final String PROJECT_PATH = "c:\\foo"

  @Test
  void toJSON() {
    TfsMaterialConfig config = tfs(new GoCipher(), REPO_URL, null, DOMAIN, PROJECT_PATH)
    String json = toObjectString({ w -> new TfsMaterialRepresenter().toJSON(w, config) })

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
    def goCipher = new GoCipher()
    TfsMaterialConfig config = tfs(goCipher, REPO_URL, USER, DOMAIN, PASSWORD, PROJECT_PATH)

    String json = toObjectString({ w -> new TfsMaterialRepresenter().toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name              : null,
      url               : REPO_URL.toString(),
      project_path      : PROJECT_PATH,
      domain            : DOMAIN,
      auto_update       : true,
      username          : USER,
      encrypted_password: goCipher.encrypt(PASSWORD)
    ])
  }

  @Test
  void fromJSON() {
    JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
      name        : null,
      url         : REPO_URL.toString(),
      project_path: PROJECT_PATH,
      domain      : DOMAIN,
      auto_update : true,
      username    : USER,
      password    : PASSWORD
    ])

    def goCipher = new GoCipher()
    TfsMaterialConfig expected = tfs(goCipher, REPO_URL, USER, DOMAIN, PASSWORD, PROJECT_PATH)

    def actual = new TfsMaterialRepresenter().fromJSON(json)
    assertEquals(expected, actual)
    assertTrue(goCipher.passwordEquals(expected.encryptedPassword, actual.encryptedPassword))
  }
}
