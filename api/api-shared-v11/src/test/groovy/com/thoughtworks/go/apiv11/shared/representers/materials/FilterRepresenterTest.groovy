/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv11.shared.representers.materials

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv11.admin.shared.representers.materials.FilterRepresenter
import com.thoughtworks.go.config.materials.Filter
import com.thoughtworks.go.config.materials.IgnoredFiles
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals


class FilterRepresenterTest {

  @Test
  void 'should serialize'() {
    def filter = new Filter(new IgnoredFiles('**/*.html'), new IgnoredFiles('**/foobar/'))
    def actualJson = toObjectString({ FilterRepresenter.toJSON(it, filter) })
    assertThatJson(actualJson).isEqualTo(filterHash)
  }

  @Test
  void 'should deserialize'() {
    def expectedFilter = new Filter(new IgnoredFiles('**/*.html'), new IgnoredFiles('**/foobar/'))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(filterHash)
    def filter = FilterRepresenter.fromJSON(jsonReader)

    assertEquals(expectedFilter, filter)
  }

  @Test
  void 'should serialize to nil when ignored is empty'() {
    def filter = new Filter()
    def actualJson = toObjectString({ FilterRepresenter.toJSON(it, filter) })

    assertThatJson(actualJson).isEqualTo([:])
  }

  @Test
  void 'should deserialize to nil when no ignored files'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(emptyFilterHash)
    def deserializedFilter = FilterRepresenter.fromJSON(jsonReader)

    assertEquals(deserializedFilter, new Filter());
  }

  def filterHash =
    [
      ignore: ["**/*.html", "**/foobar/"]
    ]

  def emptyFilterHash =
    [
      ignore: []
    ]
}
