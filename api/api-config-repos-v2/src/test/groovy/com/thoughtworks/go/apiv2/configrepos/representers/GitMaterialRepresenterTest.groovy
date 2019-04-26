/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.config.migration.UrlDenormalizerXSLTMigration121
import com.thoughtworks.go.domain.materials.MaterialConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class GitMaterialRepresenterTest {
  private static final String REPO_URL = "https://user:password@guthib.com/chewbacca"
  private static final String BRANCH = "wookie"

  @Test
  void toJSON() {
    GitMaterialConfig config = new GitMaterialConfig(REPO_URL, BRANCH)
    String json = toObjectString({ w -> new GitMaterialRepresenter().toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name              : null,
      url               : UrlDenormalizerXSLTMigration121.urlWithoutCredentials(REPO_URL),
      branch            : BRANCH,
      username          : config.getUserName(),
      encrypted_password: config.getPassword(),
      auto_update       : true
    ])
  }

  @Test
  void fromJSON() {
    JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
      name      : null,
      url       : REPO_URL,
      branch    : BRANCH,
      auto_upate: true
    ])

    MaterialConfig expected = new GitMaterialConfig(REPO_URL, BRANCH)
    assertEquals(expected, new GitMaterialRepresenter().fromJSON(json))
  }
}
