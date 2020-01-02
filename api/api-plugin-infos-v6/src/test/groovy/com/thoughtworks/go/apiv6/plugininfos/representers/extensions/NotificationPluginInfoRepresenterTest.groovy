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
package com.thoughtworks.go.apiv6.plugininfos.representers.extensions


import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helpers.PluginInfoMother.createNotificationPluginInfo
import static com.thoughtworks.go.helpers.PluginInfoMother.createNotificationPluginInfoWithoutPluginSettings
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class NotificationPluginInfoRepresenterTest {
  @Test
  void 'should serialize notification extension info to JSON'() {


    def actualJson = toObjectString({
      new NotificationPluginInfoRepresenter().toJSON(it, createNotificationPluginInfo())
    })

    assertThatJson(actualJson).isEqualTo([
      type           : "notification",
      plugin_settings: [
        configurations: [
          [
            key     : "key1",
            metadata: [
              required: true,
              secure  : false
            ]
          ],
          [
            key     : "key2",
            metadata: [
              required: true,
              secure  : false
            ]
          ]
        ],
        view          : [template: "Template"]
      ]
    ])
  }

  @Test
  void 'should serialize notification extension info without plugin settings to JSON'() {
    def actualJson = toObjectString({
      new NotificationPluginInfoRepresenter().toJSON(it, createNotificationPluginInfoWithoutPluginSettings())
    })
    assertThatJson(actualJson).isEqualTo([
      type: "notification"
    ])
  }
}
