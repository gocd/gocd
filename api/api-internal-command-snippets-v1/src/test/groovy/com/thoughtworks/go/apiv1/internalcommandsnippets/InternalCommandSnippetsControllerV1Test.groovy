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
package com.thoughtworks.go.apiv1.internalcommandsnippets

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalcommandsnippets.representers.CommandSnippetsRepresenter
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.lookups.CommandRepositoryService
import com.thoughtworks.go.server.service.lookups.CommandSnippet
import com.thoughtworks.go.server.service.lookups.CommandSnippets
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalCommandSnippetsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalCommandSnippetsControllerV1> {
  @Mock
  CommandRepositoryService commandRepositoryService

  @Mock
  EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalCommandSnippetsControllerV1 createControllerInstance() {
    new InternalCommandSnippetsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), commandRepositoryService, entityHashingService)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'index'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }


      @Test
      void 'should return 400 if no prefix is provided'() {
        def prefix = ""
        getWithApiHeader(controller.controllerPath([prefix: prefix]))

        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Request is missing parameter `prefix`")
      }

      @Test
      void 'should return command snippets'() {
        def prefix = "curl"
        List<CommandSnippet> snippets = new ArrayList<>()
        when(commandRepositoryService.lookupCommand(prefix)).thenReturn(snippets)
        when(entityHashingService.hashForEntity(new CommandSnippets(snippets))).thenReturn("digest")
        getWithApiHeader(controller.controllerPath([prefix: prefix]))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasBodyWithJson(toObjectString({ CommandSnippetsRepresenter.toJSON(it, snippets, prefix) }))
      }

      @Test
      void 'should return not modified when ETag matches'() {
        def prefix = "curl"
        List<CommandSnippet> snippets = new ArrayList<>()
        when(commandRepositoryService.lookupCommand(prefix)).thenReturn(snippets)
        when(entityHashingService.hashForEntity(new CommandSnippets(snippets))).thenReturn("digest")
        getWithApiHeader(controller.controllerPath([prefix: prefix]), ['if-none-match': 'digest'])

        assertThatResponse()
          .isNotModified()
      }
    }
  }
}
