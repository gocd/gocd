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
package com.thoughtworks.go.apiv6.plugininfos

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv6.plugininfos.representers.PluginInfoRepresenter
import com.thoughtworks.go.apiv6.plugininfos.representers.PluginInfosRepresenter
import com.thoughtworks.go.plugin.access.ExtensionsRegistry
import com.thoughtworks.go.plugin.domain.common.BadPluginInfo
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo
import com.thoughtworks.go.plugin.domain.common.PluginInfo
import com.thoughtworks.go.plugin.infra.DefaultPluginManager
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.util.HaltApiMessages.notFoundMessage
import static com.thoughtworks.go.helpers.PluginInfoMother.createAuthorizationPluginInfo
import static com.thoughtworks.go.helpers.PluginInfoMother.createSCMPluginInfo
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class PluginInfosControllerV6Test implements SecurityServiceTrait, ControllerTrait<PluginInfosControllerV6> {
  @Mock
  private DefaultPluginInfoFinder pluginInfoFinder

  @Mock
  private EntityHashingService entityHashingService

  @Mock
  private DefaultPluginManager defaultPluginManager

  @Mock
  private ExtensionsRegistry extensionRegistry

  @BeforeEach
  void setup() {
    initMocks(this)
    Set extensions = ["authorization", "scm", "configrepo", "elastic-agent", "task", "package-repository", "notification", "analytics", "artifact"]
    when(extensionRegistry.allRegisteredExtensions()).thenReturn(extensions)
  }

  @Override
  PluginInfosControllerV6 createControllerInstance() {
    new PluginInfosControllerV6(new ApiAuthenticationHelper(securityService, goConfigService), pluginInfoFinder, entityHashingService, defaultPluginManager, extensionRegistry)
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
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
      void 'should return plugin info of specified id'() {
        def pluginInfo = new CombinedPluginInfo(createAuthorizationPluginInfo())

        when(pluginInfoFinder.pluginInfoFor('plugin_id')).thenReturn(pluginInfo)
        when(entityHashingService.md5ForEntity(pluginInfo)).thenReturn("md5")

        getWithApiHeader(controller.controllerPath('/plugin_id'))

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pluginInfo, PluginInfoRepresenter)
      }

      @Test
      void 'should return bad plugin info'() {
        def about = GoPluginDescriptor.About.builder()
          .name("authorization")
          .version("v1")
          .targetGoVersion("goVersion1")
          .description("go plugin")
          .vendor(new GoPluginDescriptor.Vendor("go", "goUrl"))
          .targetOperatingSystems(["os"])
          .build()

        def descriptor = GoPluginDescriptor.builder()
          .id("plugin_id")
          .version("1")
          .about(about)
          .pluginJarFileLocation("/home/pluginjar/")
          .isBundledPlugin(true)
          .build()

        descriptor.setBundleDescriptor(new GoPluginBundleDescriptor(descriptor))
        descriptor.markAsInvalid(new ArrayList<String>(), null)
        def pluginInfo = new CombinedPluginInfo(new BadPluginInfo(descriptor))

        when(pluginInfoFinder.pluginInfoFor('plugin_id')).thenReturn(null)
        when(defaultPluginManager.getPluginDescriptorFor('plugin_id')).thenReturn(descriptor)

        getWithApiHeader(controller.controllerPath('/plugin_id'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pluginInfo, PluginInfoRepresenter)
      }

      @Test
      void 'should return 404 if plugin with id does not exist'() {

        when(pluginInfoFinder.pluginInfoFor('plugin_id')).thenReturn(null)
        when(defaultPluginManager.getPluginDescriptorFor('plugin_id')).thenReturn(null)

        getWithApiHeader(controller.controllerPath('/plugin_id'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(notFoundMessage())
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 304 if plugin info is not modified'() {
        def about = GoPluginDescriptor.About.builder()
          .name("authorization")
          .version("v1")
          .targetGoVersion("goVersion1")
          .description("go plugin")
          .vendor(new GoPluginDescriptor.Vendor("go", "goUrl"))
          .targetOperatingSystems(["os"])
          .build()

        def descriptor = GoPluginDescriptor.builder()
          .id("plugin_id")
          .version("1")
          .about(about)
          .pluginJarFileLocation("/home/pluginjar/")
          .isBundledPlugin(true)
          .build()

        def pluginInfo = new CombinedPluginInfo(new PluginInfo(descriptor, "authorization", null, null))

        when(pluginInfoFinder.pluginInfoFor('plugin_id')).thenReturn(pluginInfo)
        when(entityHashingService.md5ForEntity(pluginInfo)).thenReturn('md5')

        getWithApiHeader(controller.controllerPath('/plugin_id'), ['if-none-match': '"md5"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

    }
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'index'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsAdmin {
      Collection<CombinedPluginInfo> pluginInfos

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        def scmCombinedPluginInfo = new CombinedPluginInfo(createSCMPluginInfo())
        def authorizationCombinedPluginInfo = new CombinedPluginInfo(createAuthorizationPluginInfo())
        pluginInfos = new LinkedList<CombinedPluginInfo>()
        pluginInfos.add(scmCombinedPluginInfo)
        pluginInfos.add(authorizationCombinedPluginInfo)
      }

      @Test
      void 'should return all plugin infos'() {
        when(pluginInfoFinder.allPluginInfos()).thenReturn(pluginInfos)
        when(entityHashingService.md5ForEntity(pluginInfos)).thenReturn("md5")

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pluginInfos, PluginInfosRepresenter)
      }

      @Test
      void 'should return only plugin infos with supported extension type'() {
        def about = GoPluginDescriptor.About.builder()
          .name("authorization")
          .version("v1")
          .targetGoVersion("goVersion1")
          .description("go plugin")
          .vendor(new GoPluginDescriptor.Vendor("go", "goUrl"))
          .targetOperatingSystems(["os"])
          .build()

        def descriptor = GoPluginDescriptor.builder()
          .id("plugin_id")
          .version("1")
          .about(about)
          .pluginJarFileLocation("/home/pluginjar/")
          .isBundledPlugin(true)
          .build()

        descriptor.setBundleDescriptor(new GoPluginBundleDescriptor(descriptor))
        descriptor.markAsInvalid(new ArrayList<String>(), null)
        def pluginInfoWithInvalidExtension = new CombinedPluginInfo(new PluginInfo(descriptor, "Invalid extension name", null, null))

        def pluginInfosWithInvalidPlugin = new CombinedPluginInfo(pluginInfos)
        pluginInfosWithInvalidPlugin.add(pluginInfoWithInvalidExtension)

        when(pluginInfoFinder.allPluginInfos()).thenReturn(pluginInfosWithInvalidPlugin)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pluginInfos, PluginInfosRepresenter)
      }


      @Test
      void 'should return all plugin infos with specified type'() {
        HashMap<String, String> queryParams = new HashMap<String, String>()
        queryParams.put("type", "authorization")

        getWithApiHeader(controller.controllerPath(queryParams))

        verify(pluginInfoFinder, times(1)).allPluginInfos("authorization")
      }

      @Test
      void 'should include bad plugins infos if `include_bad` parameter is passed via request'() {
        HashMap<String, String> queryParams = new HashMap<String, String>()
        queryParams.put("include_bad", "true")

        def about = GoPluginDescriptor.About.builder()
          .name("docker")
          .version("v1")
          .targetGoVersion("goVersion1")
          .description("go plugin")
          .vendor(new GoPluginDescriptor.Vendor("go", "goUrl"))
          .targetOperatingSystems(["os"])
          .build()

        def badPluginDescriptor = GoPluginDescriptor.builder()
          .id("authorization")
          .version("1")
          .about(about)
          .pluginJarFileLocation("/home/authorization/plugin_jar/")
          .isBundledPlugin(true)
          .build()

        badPluginDescriptor.setBundleDescriptor(new GoPluginBundleDescriptor(badPluginDescriptor))
        badPluginDescriptor.markAsInvalid(new ArrayList<String>(), null)

        def pluginDescriptors = new ArrayList<GoPluginDescriptor>()
        pluginDescriptors.add(badPluginDescriptor)

        when(defaultPluginManager.plugins()).thenReturn(pluginDescriptors)
        when(pluginInfoFinder.allPluginInfos()).thenReturn(pluginInfos)

        getWithApiHeader(controller.controllerPath(queryParams))

        def expectedPluginInfo = new ArrayList<CombinedPluginInfo>()
        expectedPluginInfo.addAll(pluginInfos)
        expectedPluginInfo.add(new BadPluginInfo(badPluginDescriptor))

        expectedPluginInfo.sort { a, b -> (a.descriptor.id() <=> b.descriptor.id()) }

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(expectedPluginInfo, PluginInfosRepresenter)
      }

      @Test
      void 'should return 422 for invalid plugin type'() {
        HashMap<String, String> queryParams = new HashMap<String, String>()
        def pluginType = "foobar"
        queryParams.put("type", pluginType)

        when(pluginInfoFinder.allPluginInfos(pluginType)).thenThrow(new InvalidPluginTypeException())
        getWithApiHeader(controller.controllerPath(queryParams))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. Invalid plugin type 'foobar'. It has to be one of 'authorization, scm, configrepo, elastic-agent, task, package-repository, notification, analytics, artifact'.")
      }

      @Test
      void 'should return 304 if plugin info is not modified'() {
        when(pluginInfoFinder.allPluginInfos()).thenReturn(pluginInfos)
        when(entityHashingService.md5ForEntity(pluginInfos)).thenReturn('md5')

        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"md5"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }
    }
  }

}
