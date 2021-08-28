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
package com.thoughtworks.go.apiv2.adminsconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.adminsconfig.representers.AdminsConfigRepresenter
import com.thoughtworks.go.apiv2.adminsconfig.representers.BulkUpdateFailureResultRepresenter
import com.thoughtworks.go.config.AdminRole
import com.thoughtworks.go.config.AdminUser
import com.thoughtworks.go.config.AdminsConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.server.service.AdminsConfigService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.BulkUpdateAdminsResult
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class AdminControllerV2Test implements ControllerTrait<AdminControllerV2>, SecurityServiceTrait {
  @Mock
  private AdminsConfigService adminsConfigService
  @Mock
  private EntityHashingService entityHashingService

  @Override
  AdminControllerV2 createControllerInstance() {
    return new AdminControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, adminsConfigService)
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @BeforeEach
      void setUp() {
        AdminsConfig config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        when(adminsConfigService.systemAdmins()).thenReturn(config)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsAdmin {
      HttpLocalizedOperationResult result

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        this.result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should render the security admins config'() {
        AdminsConfig config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        when(entityHashingService.hashForEntity(config)).thenReturn('digest')
        when(adminsConfigService.systemAdmins()).thenReturn(config)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(config, AdminsConfigRepresenter)
      }

      @Test
      void 'should render 304 if etag matches'() {
        def config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        when(entityHashingService.hashForEntity(config)).thenReturn('digest')
        when(adminsConfigService.systemAdmins()).thenReturn(config)
        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render 200 if etag does not match'() {
        def config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin-new")))
        when(entityHashingService.hashForEntity(config)).thenReturn('digest')
        when(adminsConfigService.systemAdmins()).thenReturn(config)
        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(config, AdminsConfigRepresenter)
      }
    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      AdminsConfig config

      @BeforeEach
      void setUp() {
        config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        when(adminsConfigService.systemAdmins()).thenReturn(config)
        when(entityHashingService.hashForEntity(config)).thenReturn('cached-digest')
      }

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        sendRequest('put', controller.controllerPath(), [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-digest',
          'content-type': 'application/json'
        ], toObjectString({ AdminsConfigRepresenter.toJSON(it, this.config) }))
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
      void 'should update the system admins'() {
        AdminsConfig configInServer = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        AdminsConfig configFromRequest = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")),
          new AdminUser(new CaseInsensitiveString("new_admin")))

        when(adminsConfigService.systemAdmins()).thenReturn(configInServer)
        when(entityHashingService.hashForEntity(configInServer)).thenReturn("cached-digest")

        putWithApiHeader(controller.controllerPath(), ['if-match': 'cached-digest'], toObjectString({
          AdminsConfigRepresenter.toJSON(it, configFromRequest)
        }))

        verify(adminsConfigService).update(any(), eq(configFromRequest), eq("cached-digest"), any(HttpLocalizedOperationResult.class))
        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(configFromRequest, AdminsConfigRepresenter)
      }

      @Test
      void 'should return a response with errors if update fails'() {
        AdminsConfig configInServer = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        AdminsConfig configFromRequest = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")),
          new AdminUser(new CaseInsensitiveString("new_admin")))


        when(adminsConfigService.systemAdmins()).thenReturn(configInServer)
        when(entityHashingService.hashForEntity(configInServer)).thenReturn("cached-digest")
        when(adminsConfigService.update(any(), eq(configFromRequest), eq("cached-digest"), any(HttpLocalizedOperationResult.class))).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        putWithApiHeader(controller.controllerPath(), ['if-match': 'cached-digest'], toObjectString({
          AdminsConfigRepresenter.toJSON(it, configFromRequest)
        }))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("validation failed")
      }

      @Test
      void 'should not update a stale system admins request'() {
        AdminsConfig systemAdminsRequest = new AdminsConfig(new AdminRole(new CaseInsensitiveString("admin")))
        AdminsConfig systemAdminsInServer = new AdminsConfig(new AdminRole(new CaseInsensitiveString("role1")))

        when(adminsConfigService.systemAdmins()).thenReturn(systemAdminsInServer)
        when(entityHashingService.hashForEntity(systemAdminsInServer)).thenReturn('cached-digest')

        putWithApiHeader(controller.controllerPath(), ['if-match': 'some-string'], toObjectString({
          AdminsConfigRepresenter.toJSON(it, systemAdminsRequest)
        }))

        verify(adminsConfigService, Mockito.never()).update(any(), any(), any(), any())
        assertThatResponse().isPreconditionFailed()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
      }
    }
  }

  @Nested
  class Patch {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      AdminsConfig config

      @BeforeEach
      void setUp() {
        config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        when(adminsConfigService.systemAdmins()).thenReturn(config)
        when(entityHashingService.hashForEntity(config)).thenReturn('cached-digest')
      }

      @Override
      String getControllerMethodUnderTest() {
        return "bulkUpdate"
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath(), [])
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
      void 'should patch the system admins'() {
        AdminsConfig expectedConfig = new AdminsConfig(new AdminUser("jez"), new AdminUser("tez"),
          new AdminRole("super"), new AdminRole("duper"))

        when(adminsConfigService.systemAdmins()).thenReturn(expectedConfig)
        when(adminsConfigService.bulkUpdate(any(), eq(["jez", "tez"]), eq(["admin"]), eq(["super", "duper"]), eq(["wonky", "donkey"]), eq("cached-digest")))
          .thenReturn(new BulkUpdateAdminsResult())
        when(entityHashingService.hashForEntity(expectedConfig)).thenReturn("cached-digest")

        patchWithApiHeader(controller.controllerPath(), [
          operations: [
            users: [
              add   : ["jez", "tez"],
              remove: ["admin"]
            ],
            roles: [
              add   : ["super", "duper"],
              remove: ["wonky", "donkey"]
            ]
          ]
        ])

        verify(adminsConfigService).bulkUpdate(any(), eq(["jez", "tez"]), eq(["admin"]), eq(["super", "duper"]), eq(["wonky", "donkey"]), eq("cached-digest"))
        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(expectedConfig, AdminsConfigRepresenter)
      }

      @Test
      void 'should return a response with errors if patch fails'() {
        AdminsConfig configInServer = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))

        def result = new BulkUpdateAdminsResult()
        result.nonExistentRoles = [new CaseInsensitiveString("jez"), new CaseInsensitiveString("tez")]
        result.unprocessableEntity("validation failed")
        when(adminsConfigService.systemAdmins()).thenReturn(configInServer)
        when(entityHashingService.hashForEntity(configInServer)).thenReturn("cached-digest")
        when(adminsConfigService.bulkUpdate(any(), eq(["jez", "tez"]), eq([]), eq([]), eq([]), eq("cached-digest")))
          .thenReturn(result)

        patchWithApiHeader(controller.controllerPath(), [
          operations: [
            users: [
              add: ["jez", "tez"],
            ]
          ]
        ])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(result, BulkUpdateFailureResultRepresenter.class)
      }
    }
  }
}
