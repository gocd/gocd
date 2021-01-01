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
package com.thoughtworks.go.apiv2.notificationfilter.representers

import com.thoughtworks.go.domain.NotificationFilter
import com.thoughtworks.go.domain.StageEvent
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class NotificationFiltersRepresenterTest {

  @Test
  void 'should serialize empty list as json'() {
    def json = toObjectString({
      NotificationFiltersRepresenter.toJSON(it, Collections.emptyList())
    })

    def expected = [
      "_links"   : [
        "self": ["href": "http://test.host/go/api/notification_filters"],
        "doc" : ["href": apiDocsUrl("#notification-filters")]
      ],
      "_embedded": [
        "filters": []
      ]
    ]

    assertThatJson(json).isEqualTo(expected)
  }

  @Test
  void 'should serialize to json'() {
    def filterOne = new NotificationFilter("up42", "stage_up42", StageEvent.All, true)
    filterOne.setId(1)
    def filterTwo = new NotificationFilter("up43", "stage_up43", StageEvent.Fails, false)
    filterTwo.setId(2)
    def filters = Arrays.asList(filterOne, filterTwo)

    def json = toObjectString({
      NotificationFiltersRepresenter.toJSON(it, filters)
    })

    def expected = [
      "_links"   : [
        "self": ["href": "http://test.host/go/api/notification_filters"],
        "doc" : ["href": apiDocsUrl("#notification-filters")]
      ],
      "_embedded": [
        "filters": [
          [
            "_links"       : [
              "self": ["href": "http://test.host/go/api/notification_filters/1"],
              "doc" : ["href": apiDocsUrl("#notification-filters")],
              "find": ["href": "http://test.host/go/api/notification_filters/:id"]
            ],
            "id"           : 1,
            "pipeline"     : "up42",
            "stage"        : "stage_up42",
            "event"        : "All",
            "match_commits": true
          ],
          [
            "_links"       : [
              "self": ["href": "http://test.host/go/api/notification_filters/2"],
              "doc" : ["href": apiDocsUrl("#notification-filters")],
              "find": ["href": "http://test.host/go/api/notification_filters/:id"]
            ],
            "id"           : 2,
            "pipeline"     : "up43",
            "stage"        : "stage_up43",
            "event"        : "Fails",
            "match_commits": false
          ]
        ]
      ]
    ]

    assertThatJson(json).isEqualTo(expected)
  }
}
