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
package com.thoughtworks.go.apiv2.notificationfilter.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException
import com.thoughtworks.go.domain.NotificationFilter
import com.thoughtworks.go.domain.StageEvent
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatCode

class NotificationFilterRepresenterTest {

  @Test
  void 'should serialize to json'() {
    def filter = new NotificationFilter("up42", "unit-test", StageEvent.Breaks, true)
    filter.setId(1)

    def json = toObjectString({
      NotificationFilterRepresenter.toJSON(it, filter)
    })

    def expected = [
      "_links"       : [
        "self": ["href": "http://test.host/go/api/notification_filters/1"],
        "doc" : ["href": apiDocsUrl("#notification-filters")],
        "find": ["href": "http://test.host/go/api/notification_filters/:id"]
      ],
      "id"           : 1,
      "pipeline"     : "up42",
      "stage"        : "unit-test",
      "event"        : "Breaks",
      "match_commits": true
    ]

    assertThatJson(json).isEqualTo(expected)
  }

  @Test
  void 'should deserialize json from request'() {
    def reader = GsonTransformer.getInstance().jsonReaderFrom([
      "id"           : 100,
      "pipeline"     : "up42",
      "stage"        : "unit-test",
      "event"        : "Breaks",
      "match_commits": true
    ])

    def filter = NotificationFilterRepresenter.fromJSON(reader)

    assertThat(filter.getId()).isEqualTo(100)
    assertThat(filter.getPipelineName()).isEqualTo("up42")
    assertThat(filter.getStageName()).isEqualTo("unit-test")
    assertThat(filter.getEvent()).isEqualTo(StageEvent.Breaks)
    assertThat(filter.isMyCheckin()).isEqualTo(true)
  }

  @Test
  void 'should throw unprocessable entity when event with given name does not exist'() {
    def reader = GsonTransformer.getInstance().jsonReaderFrom([
      "id"           : 100,
      "pipeline"     : "up42",
      "stage"        : "unit-test",
      "event"        : "UnknownEventName",
      "match_commits": true
    ])

    assertThatCode({ NotificationFilterRepresenter.fromJSON(reader) })
      .isInstanceOf(UnprocessableEntityException)
      .hasMessage("Invalid event 'UnknownEventName'. It has to be one of [Fails, Passes, Breaks, Fixed, Cancelled, All].")
  }

  @Test
  void 'should deserialize even if match_commits is not present'() {
    def reader = GsonTransformer.getInstance().jsonReaderFrom([
      "id"      : 100,
      "pipeline": "up42",
      "stage"   : "unit-test",
      "event"   : "Breaks"
    ])

    def filter = NotificationFilterRepresenter.fromJSON(reader)

    assertThat(filter.getId()).isEqualTo(100)
    assertThat(filter.getPipelineName()).isEqualTo("up42")
    assertThat(filter.getStageName()).isEqualTo("unit-test")
    assertThat(filter.getEvent()).isEqualTo(StageEvent.Breaks)
    assertThat(filter.isMyCheckin()).isEqualTo(false)
  }
}
