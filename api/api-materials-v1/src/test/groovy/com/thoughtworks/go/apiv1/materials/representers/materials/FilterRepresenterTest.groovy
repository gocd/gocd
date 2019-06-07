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

package com.thoughtworks.go.apiv1.materials.representers.materials


import com.thoughtworks.go.config.materials.Filter
import com.thoughtworks.go.config.materials.IgnoredFiles
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class FilterRepresenterTest {

  @Test
  void 'should serialize'() {
    def filter = new Filter(new IgnoredFiles('**/*.html'), new IgnoredFiles('**/foobar/'))
    def actualJson = toObjectString(FilterRepresenter.toJSON(filter))
    assertThatJson(actualJson).isEqualTo(filterHash)
  }

  @Test
  void 'should serialize to nil when ignored is empty'() {
    def filter = new Filter()
    def actualJson = toObjectString(FilterRepresenter.toJSON(filter))

    assertThatJson(actualJson).isEqualTo([:])
  }

  def filterHash = [
    ignore: ["**/*.html", "**/foobar/"]
  ]
}
