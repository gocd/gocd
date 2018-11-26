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

package com.thoughtworks.go.apiv1.serverdrainmode.representer

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv1.serverdrainmode.representers.DrainModeRepresenter
import com.thoughtworks.go.server.domain.ServerDrainMode
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.util.TimeProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

import java.sql.Timestamp

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class DrainModeRepresenterTest {
  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  TimeProvider timeProvider

  @Test
  void "should represent data sharing settings"() {
    def drainMode = new ServerDrainMode()

    drainMode.setDrainMode(true)
    drainMode.updatedBy("Bob")
    drainMode.updatedOn(new Timestamp(new Date().getTime()))

    def actualJson = toObjectString({ DrainModeRepresenter.toJSON(it, drainMode) })

    def expectedJson = [
      _links     : [
        self: [href: 'http://test.host/go/api/drain_mode/settings'],
        doc : [href: 'https://api.gocd.org/current/#server-drain-mode']
      ],
      "_embedded": [
        drain     : drainMode.isDrainMode(),
        updated_by: drainMode.updatedBy(),
        updated_on: jsonDate(drainMode.updatedOn())
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }


  @Test
  void "should deserialize server drain mode object"() {
    def json = [
      drain: true
    ]

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(json)
    def time = 10000000l
    when(timeProvider.currentTimeMillis()).thenReturn(time)
    def deserializedSettings = DrainModeRepresenter.fromJSON(jsonReader,
      new Username("user"), timeProvider,
      new ServerDrainMode(false, "me", new Date()))
    assertEquals(deserializedSettings.isDrainMode(), true)
    assertEquals(deserializedSettings.updatedBy(), "user")
    assertEquals(deserializedSettings.updatedOn(), new Timestamp(time))
  }

  @Test
  void "should set drain flag from the object from server if the user does not pass it along during deserialization of server drain mode"() {
    def json = [a: ""]

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(json)
    def time = 10000000l
    when(timeProvider.currentTimeMillis()).thenReturn(time)
    def deserializedSettings = DrainModeRepresenter.fromJSON(jsonReader,
      new Username("user"), timeProvider,
      new ServerDrainMode(false, "me", new Date()))
    assertEquals(deserializedSettings.isDrainMode(), false)
    assertEquals(deserializedSettings.updatedBy(), "user")
    assertEquals(deserializedSettings.updatedOn(), new Timestamp(time))
  }
}
