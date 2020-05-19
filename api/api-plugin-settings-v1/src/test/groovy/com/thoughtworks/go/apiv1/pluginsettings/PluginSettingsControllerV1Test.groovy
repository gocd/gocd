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
package com.thoughtworks.go.apiv1.pluginsettings

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.pluginsettings.representers.PluginSettingsRepresenter
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
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.PluginService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class PluginSettingsControllerV1Test implements SecurityServiceTrait, ControllerTrait<PluginSettingsControllerV1> {
  @Mock
  PluginService pluginService

  @Mock
  EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PluginSettingsControllerV1 createControllerInstance() {
    new PluginSettingsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), pluginService, entityHashingService)
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'show'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("/plugin_id"))
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return plugin settings of specified plugin id'() {
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings("plugin_id")).thenReturn(pluginSettings)
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn("digest")

        getWithApiHeader(controller.controllerPath('/plugin_id'))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pluginSettings, PluginSettingsRepresenter)
      }

      @Test
      void 'should return 404 if plugin with id does not exist'() {
        when(pluginService.getPluginSettings("plugin_id")).thenReturn(null)
        getWithApiHeader(controller.controllerPath('/plugin_id'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Plugin settings with id 'plugin_id' was not found!")
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 304 if plugin info is not modified'() {
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings("plugin_id")).thenReturn(pluginSettings)
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn("digest")

        getWithApiHeader(controller.controllerPath('/plugin_id'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), '{}')
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should create plugin settings from given json payload'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(true)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(pluginInfo())
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn('some-digest')
        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"some-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pluginSettings, PluginSettingsRepresenter)
      }

      @Test
      void 'should not create plugin settings from given json payload when plugin is not loaded'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(false)
        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. The plugin with id 'json.elastic.plugin' is not loaded.")
      }

      @Test
      void 'should not create plugin settings from given json payload when plugin does not support plugin settings'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(true)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(null)
        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. The plugin with id 'json.elastic.plugin' does not support plugin-settings.")
      }

      @Test
      void 'should not create plugin settings from given json payload in case of validation errors'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(true)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(pluginInfo())
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn('some-digest')
        doAnswer({ InvocationOnMock invocation ->
          def result = (HttpLocalizedOperationResult) invocation.arguments.last()
          result.unprocessableEntity("Boom!")
          return null
        }).when(pluginService).createPluginSettings(any(), any(), any())

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Boom!")
      }
    }
  }

  @Nested
  class Update {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath("/plugin_id"), '{}')
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should update plugin settings from given json payload'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings(pluginSettings.pluginId)).thenReturn(pluginSettings)
        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(true)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(pluginInfo())
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn('some-digest')

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'some-digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/$pluginSettings.pluginId"), headers, jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"some-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pluginSettings, PluginSettingsRepresenter)
      }

      @Test
      void 'should not update plugin settings from given json payload when plugin settings are not configured'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings(pluginSettings.pluginId)).thenReturn(null)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'some-digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/$pluginSettings.pluginId"), headers, jsonPayload)

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Plugin settings with id 'json.elastic.plugin' was not found!")
      }

      @Test
      void 'should not update plugin settings from given json payload when plugin is not loaded'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings(pluginSettings.pluginId)).thenReturn(pluginSettings)
        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(false)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(pluginInfo())
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn('some-digest')

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'some-digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/$pluginSettings.pluginId"), headers, jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. The plugin with id 'json.elastic.plugin' is not loaded.")
      }

      @Test
      void 'should not update plugin settings from given json payload when plugin does not support plugin settings'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings(pluginSettings.pluginId)).thenReturn(pluginSettings)
        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(true)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(null)
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn('some-digest')

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'some-digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/$pluginSettings.pluginId"), headers, jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. The plugin with id 'json.elastic.plugin' does not support plugin-settings.")
      }

      @Test
      void 'should not update plugin settings from given json payload when etag is not provided'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings(pluginSettings.pluginId)).thenReturn(pluginSettings)
        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(true)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(pluginInfo())
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn('some-digest')

        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/$pluginSettings.pluginId"), headers, jsonPayload)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
      }

      @Test
      void 'should not update plugin settings from given json payload when etag has changed'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings(pluginSettings.pluginId)).thenReturn(pluginSettings)
        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(true)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(pluginInfo())
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn('some-digest')

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'some-fancy-digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/$pluginSettings.pluginId"), headers, jsonPayload)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
      }

      @Test
      void 'should not update plugin settings from given json payload in case of validation errors'() {
        def jsonPayload = pluginSettingsHash
        def pluginSettings = pluginSettings()

        when(pluginService.getPluginSettings(pluginSettings.pluginId)).thenReturn(pluginSettings)
        when(pluginService.isPluginLoaded(pluginSettings.pluginId)).thenReturn(true)
        when(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginSettings.pluginId)).thenReturn(pluginInfo())
        when(entityHashingService.hashForEntity(pluginSettings)).thenReturn('some-digest')
        doAnswer({ InvocationOnMock invocation ->
          def result = (HttpLocalizedOperationResult) invocation.arguments[2]
          result.unprocessableEntity("Boom!")
          return null
        }).when(pluginService).updatePluginSettings(any(), any(), any(), any())

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'some-digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/$pluginSettings.pluginId"), headers, jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Boom!")
      }
    }
  }

  private PluginSettings pluginSettings() {
    ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>()
    configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1")))
    configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("k2"), new EncryptedConfigurationValue(new GoCipher().encrypt("v2"))))

    PluginSettings pluginSettings = new PluginSettings("json.elastic.plugin")
    pluginSettings.addConfigurations(pluginInfo(), configurationProperties)
    return pluginSettings
  }

  private PluginInfo pluginInfo() {
    ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>()
    pluginConfigurations.add(new PluginConfiguration("k1", new Metadata(true, false)))
    pluginConfigurations.add(new PluginConfiguration("k2", new Metadata(true, true)))
    return new ConfigRepoPluginInfo(null, null, new PluggableInstanceSettings(pluginConfigurations), new Capabilities())
  }

  private def pluginSettingsHash = [
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
