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
import com.thoughtworks.go.apiv9.admin.shared.representers.TimerRepresenter
import com.thoughtworks.go.config.TimerConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class TimerRepresenterTest {

  @Test
  void "should represent a timer"() {
    def timer = new TimerConfig("0 0 7 ? * MON", false)
    timer.validateTree(null)
    def actualJson = toObjectString({ TimerRepresenter.toJSON(it, timer) })

    assertThatJson(actualJson).isEqualTo(timerhash)
  }

  @Test
  void "should represent validation errors"() {
    def timer = new TimerConfig("SOME JUNK TIMER SPEC", false)
    timer.validateTree(null)
    def actualJson = toObjectString({ TimerRepresenter.toJSON(it, timer) })

    assertThatJson(actualJson).isEqualTo(timerHashWithError)
  }


  @Test
  void "should deserialize"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(timerhash)
    def deserializedTimer = TimerRepresenter.fromJSON(jsonReader)
    def expected = new TimerConfig("0 0 7 ? * MON", false)
    assertEquals(expected, deserializedTimer)
  }

  def timerhash =
    [
      spec           : "0 0 7 ? * MON",
      only_on_changes: false
    ]

  def timerHashWithError =
    [
      spec           : "SOME JUNK TIMER SPEC",
      only_on_changes: false,
      errors         : [
        spec: ["Invalid cron syntax: Illegal characters for this position: 'SOM'"]
      ]
    ]
}
