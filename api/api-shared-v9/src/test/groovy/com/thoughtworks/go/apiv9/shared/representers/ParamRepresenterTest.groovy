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
package com.thoughtworks.go.apiv9.shared.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv9.admin.shared.representers.ParamRepresenter
import com.thoughtworks.go.config.ParamConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class ParamRepresenterTest {

  @Test
  void  "should represent a param"() {
    def actualJson = toObjectString({ ParamRepresenter.toJSON(it, new ParamConfig("command", "echo")) })

    assertThatJson(actualJson).isEqualTo(paramHash)
  }

  @Test
  void "should deserialize" () {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(paramHash)
    def paramConfig = ParamRepresenter.fromJSON(jsonReader)

    assertEquals(new ParamConfig('command', 'echo'), paramConfig)
  }

  def paramHash =
  [
    name:         "command",
    value:       "echo"
  ]
}
