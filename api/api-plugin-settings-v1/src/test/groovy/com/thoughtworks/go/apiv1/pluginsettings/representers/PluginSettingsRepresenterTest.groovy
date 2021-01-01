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
package com.thoughtworks.go.apiv1.pluginsettings.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.domain.config.ConfigurationKey
import com.thoughtworks.go.domain.config.ConfigurationProperty
import com.thoughtworks.go.domain.config.ConfigurationValue
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue
import com.thoughtworks.go.plugin.domain.common.Metadata
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration
import com.thoughtworks.go.plugin.domain.common.PluginInfo
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities
import com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.server.domain.PluginSettings
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class PluginSettingsRepresenterTest {

  @Test
  void "should represent a plugin settings object"() {
    def actualJson = toObjectString({ PluginSettingsRepresenter.toJSON(it, pluginSettings()) })

    assertThatJson(actualJson).isEqualTo(pluginSettingsHash)
  }

  @Test
  void "should deserialize from json"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(pluginSettingsHash)
    def paramConfig = PluginSettingsRepresenter.fromJSON(pluginInfo(), jsonReader)

    assertEquals(pluginSettings(), paramConfig)
  }

  PluginSettings pluginSettings() {
    ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>()
    configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1")))
    configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("k2"), new EncryptedConfigurationValue(new GoCipher().encrypt("v2"))))

    PluginSettings pluginSettings = new PluginSettings("json.elastic.plugin")
    pluginSettings.addConfigurations(pluginInfo(), configurationProperties)
    return pluginSettings
  }

  PluginInfo pluginInfo() {
    ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>()
    pluginConfigurations.add(new PluginConfiguration("k1", new Metadata(true, false)))
    pluginConfigurations.add(new PluginConfiguration("k2", new Metadata(true, true)))
    return new ConfigRepoPluginInfo(null, null, new PluggableInstanceSettings(pluginConfigurations), new Capabilities())
  }

  def pluginSettingsHash = [
    _links       : [
      doc : [
        href: apiDocsUrl("#plugin-settings")
      ],
      find: [
        href: "http://test.host/go/api/admin/plugin_settings/:plugin_id"
      ],
      self: [
        href: "http://test.host/go/api/admin/plugin_settings/json.elastic.plugin"
      ]
    ],
    plugin_id    : "json.elastic.plugin",
    configuration: [
      [
        key  : "k1",
        value: pluginSettings().getPluginSettingsProperties().get(0).value
      ],
      [
        key            : "k2",
        encrypted_value: pluginSettings().getPluginSettingsProperties().get(1).encryptedValue
      ]
    ]
  ]

}
