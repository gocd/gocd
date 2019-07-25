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
package com.thoughtworks.go.apiv1.configrepos.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class GitMaterialRepresenterTest {
  private static final String REPO_URL = "https://user:password@guthib.com/chewbacca"
  private static final String BRANCH = "wookie"

  @Test
  void toJSON() {
    GitMaterialConfig config = git(REPO_URL, BRANCH)
    String json = toObjectString({ w -> GitMaterialRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      name       : null,
      url        : REPO_URL,
      branch     : BRANCH,
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
        branch    : BRANCH,
        auto_upate: true
      ])

      MaterialConfig expected = git(REPO_URL, BRANCH)
      assertEquals(expected, GitMaterialRepresenter.fromJSON(json))
    }

    @Test
    void shouldHandleNullUrl() {
      JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
        name      : null,
        url       : null,
        branch    : BRANCH,
        auto_upate: true
      ])

      MaterialConfig expected = git(null, BRANCH)
      assertEquals(expected, GitMaterialRepresenter.fromJSON(json))
    }

    @Test
    void shouldHandleNullMissingUrlField() {
      JsonReader json = GsonTransformer.getInstance().jsonReaderFrom([
        name      : null,
        branch    : BRANCH,
        auto_upate: true
      ])

      MaterialConfig expected = git(null, BRANCH)
      assertEquals(expected, GitMaterialRepresenter.fromJSON(json))
    }
  }
}
