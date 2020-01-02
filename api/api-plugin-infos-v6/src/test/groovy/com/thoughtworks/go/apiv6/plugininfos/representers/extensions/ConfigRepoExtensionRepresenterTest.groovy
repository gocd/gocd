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

import com.thoughtworks.go.helpers.PluginInfoMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ConfigRepoExtensionRepresenterTest {
  @Test
  void 'should serialize config repo extension info to JSON'() {
    def actualJson = toObjectString({
      new ConfigRepoExtensionRepresenter().toJSON(it, PluginInfoMother.createConfigRepoPluginInfo())
    })
    def expectedJSON = [
      type           : "configrepo",
      plugin_settings: [
        configurations: [
          [
            key     : "key1",
            metadata: [required: true, secure: false]
          ],
          [
            key     : "key2",
            metadata: [required: true, secure: false]
          ]
        ],
        view          : [template: "Template"]
      ],
      capabilities   : [
        supports_pipeline_export: true,
        supports_parse_content  : true
      ]
    ]
    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }

  @Test
  void 'should serialize config repo extension info without plugin settings to JSON'() {
    def actualJson = toObjectString({
      new ConfigRepoExtensionRepresenter().toJSON(it, PluginInfoMother.createConfigRepoPluginInfoWithoutPluginSettings())
    })
    def expectedJSON = [
      type        : "configrepo",
      capabilities: [
        supports_pipeline_export: true,
        supports_parse_content  : true
      ]
    ]
    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
