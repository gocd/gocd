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

package com.thoughtworks.go.apiv1.serverdrainmode.representers

import com.thoughtworks.go.server.domain.ServerDrainMode
import com.thoughtworks.go.util.TimeProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

import java.sql.Timestamp

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.MockitoAnnotations.initMocks

class DrainModeSettingsRepresenterTest {
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

    def actualJson = toObjectString({ DrainModeSettingsRepresenter.toJSON(it, drainMode) })

    def expectedJson = [
      _links     : [
        self: [href: 'http://test.host/go/api/admin/drain_mode/settings'],
        doc : [href: 'https://api.gocd.org/current/#drain-mode-settings']
      ],
      "_embedded": [
        drain     : drainMode.isDrainMode(),
        updated_by: drainMode.updatedBy(),
        updated_on: jsonDate(drainMode.updatedOn())
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
