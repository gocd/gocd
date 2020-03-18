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
package com.thoughtworks.go.apiv4.configrepos.representers

import com.thoughtworks.go.api.base.OutputWriter
import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException
import com.thoughtworks.go.config.materials.PackageMaterialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.mockito.Mockito.mock

class MaterialsRepresenterTest {
  private static final String REPO_URL = "https://github.com/chewbacca"
  private static final String BRANCH = "wookie"

  @Test
  void toJSON() {
    MaterialConfig config = git(REPO_URL, BRANCH)
    String json = toObjectString({ w -> MaterialsRepresenter.toJSON(w, config) })

    assertThatJson(json).isEqualTo([
      type      : "git",
      attributes: [
        name       : null,
        url        : REPO_URL,
        branch     : BRANCH,
        auto_update: true
      ]
    ])
  }

  @Test
  void 'toJSON() fails on unsupported material type'() {
    MaterialConfig config = new PackageMaterialConfig("foobar")
    Throwable error = assertThrows(IllegalArgumentException.class, {
      MaterialsRepresenter.toJSON(mock(OutputWriter.class), config)
    })

    assertEquals("Cannot serialize unsupported material type: PackageMaterialConfig", error.getMessage())
  }

  @Test
  void fromJSON() {
    JsonReader json = GsonTransformer.instance.jsonReaderFrom([
      type      : "git",
      attributes: [
        name       : null,
        url        : REPO_URL,
        branch     : BRANCH,
        auto_update: true
      ]
    ])

    MaterialConfig expected = git(REPO_URL, BRANCH)
    assertEquals(expected, MaterialsRepresenter.fromJSON(json))
  }

  @Test
  void 'fromJSON fails for unsupported type'() {
    Throwable error = assertThrows(UnprocessableEntityException.class, {
      JsonReader json = GsonTransformer.instance.jsonReaderFrom([
        type      : 'package',
        attributes: [:]
      ])
      MaterialsRepresenter.fromJSON(json)
    })
    assertEquals("Unsupported material type: package. It has to be one of 'git, hg, svn, p4 and tfs'.", error.getMessage())
  }
}
