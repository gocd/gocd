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

package com.thoughtworks.go.apiv1.templateauthorization

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.templateauthorization.representers.AuthorizationRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.TemplateConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.api.base.JsonUtils.toObjectWithoutLinks
import static com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

@MockitoSettings(strictness = Strictness.LENIENT)
class TemplateAuthorizationControllerV1Test implements SecurityServiceTrait, ControllerTrait<TemplateAuthorizationControllerV1> {
  @Mock
  EntityHashingService entityHashingService;

  @Mock
  TemplateConfigService templateConfigService;


  @Override
  TemplateAuthorizationControllerV1 createControllerInstance() {
    new TemplateAuthorizationControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, templateConfigService)
  }

  @Nested
  class show {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("/t1/authorization"))
      }
    }

    @Nested
    class AsAdmin {
      private HttpLocalizedOperationResult result

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should render template authorization as JSON'() {
        def templateName = "template-name"
        def templateConfig = createTemplate(templateName)
        def viewConfig = new ViewConfig(new AdminRole(new CaseInsensitiveString("role_view")), new AdminUser("view"))
        def adminsConfig = new AdminsConfig(new AdminRole(new CaseInsensitiveString("role_admin")), new AdminUser("admin"))
        def authorization = new Authorization(viewConfig, null, adminsConfig)

        templateConfig.setAuthorization(authorization)
        when(entityHashingService.hashForEntity(any(PipelineTemplateConfig) as PipelineTemplateConfig)).thenReturn('digest')
        when(templateConfigService.loadForView(templateName, result)).thenReturn(templateConfig)

        getWithApiHeader(controller.controllerPath("/${templateName}/authorization"))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasBodyWithJsonObject(AuthorizationRepresenter, templateConfig.getAuthorization())
      }

      @Test
      void "should return 304 if etag sent in request is fresh"() {
        when(entityHashingService.hashForEntity(any(PipelineTemplateConfig) as PipelineTemplateConfig)).thenReturn('digest')
        when(templateConfigService.loadForView("t1", result)).thenReturn(createTemplate("t1"))

        getWithApiHeader(controller.controllerPath('/t1/authorization'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 404 if the template does not exist'() {
        when(templateConfigService.loadForView('non-existent-template', result)).thenReturn(null)

        getWithApiHeader(controller.controllerPath("/non-existent-template/authorization"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("non-existent-template"))
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class update {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      Authorization authorization

      @BeforeEach
      void setUp() {
        def viewConfig = new ViewConfig(new AdminRole(new CaseInsensitiveString("role_view")), new AdminUser("view"))
        def adminsConfig = new AdminsConfig(new AdminRole(new CaseInsensitiveString("role_admin")), new AdminUser("admin"))
        authorization = new Authorization(viewConfig, null, adminsConfig)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath('/t1/authorization'), [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-digest',
          'content-type': 'application/json'
        ], toObjectString({ AuthorizationRepresenter.toJSON(it, authorization) }))
      }
    }

    @Nested
    class AsAdmin {
      private HttpLocalizedOperationResult result

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should update authorization for a given template'() {
        def templateName = "template-name"
        def templateConfig = createTemplate(templateName)
        def templateConfigAfterUpdate = createTemplate(templateName)

        def viewConfig = new ViewConfig(new AdminRole(new CaseInsensitiveString("role_view")), new AdminUser("view"))
        def adminsConfig = new AdminsConfig(new AdminRole(new CaseInsensitiveString("role_admin")), new AdminUser("admin"))
        def authorizationRequest = new Authorization(viewConfig, new OperationConfig(), adminsConfig)
        templateConfigAfterUpdate.setAuthorization(authorizationRequest)

        when(entityHashingService.hashForEntity(any(PipelineTemplateConfig) as PipelineTemplateConfig)).thenReturn('digest').thenReturn('digest_after_update')
        when(templateConfigService.loadForView(templateName, result)).thenReturn(templateConfig).thenReturn(templateConfigAfterUpdate)
        doNothing().when(templateConfigService).updateTemplateAuthConfig(any(Username.class) as Username, eq(templateConfig),
          any(), eq(result), eq("digest"))

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/${templateName}/authorization"), headers, toObjectString({
          AuthorizationRepresenter.toJSON(it, authorizationRequest)
        }))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest_after_update"')
          .hasBodyWithJsonObject(AuthorizationRepresenter, authorizationRequest)
      }

      @Test
      void 'should fail update if etag does not match' () {
        def templateName = "template-name"
        def templateConfig = createTemplate(templateName)

        when(templateConfigService.loadForView(templateName, result)).thenReturn(templateConfig)
        when(entityHashingService.hashForEntity(any(PipelineTemplateConfig.class) as PipelineTemplateConfig)).thenReturn("another-etag")

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/${templateName}/authorization"), headers, authorizationHash)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for template 'template-name'. Please update your copy of the config with the changes and try again.")
      }

      @Test
      void 'should return 404 if the template does not exist'() {
        def templateName = "non-existent-template"

        when(templateConfigService.loadForView(templateName, result)).thenReturn(null)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/${templateName}/authorization"), headers, authorizationHash)

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("non-existent-template"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 422 with errors serialized if validation fails'() {
        def templateName = "template-name"
        def templateConfig = createTemplate(templateName)

        def viewConfig = new ViewConfig(new AdminRole(new CaseInsensitiveString("role_view")), new AdminUser("view"))
        def adminsConfig = new AdminsConfig(new AdminRole(new CaseInsensitiveString("role_admin")), new AdminUser("admin"))
        def authorizationRequest = new Authorization(viewConfig, null, adminsConfig)

        when(entityHashingService.hashForEntity(any(PipelineTemplateConfig) as PipelineTemplateConfig)).thenReturn('digest').thenReturn('digest')
        when(templateConfigService.loadForView(templateName, result)).thenReturn(templateConfig)
        doAnswer({ InvocationOnMock invocation ->
          authorizationRequest.addError("name", "Role \"role_admin\" does not exist.")

          HttpLocalizedOperationResult result = invocation.arguments[3]
          result.unprocessableEntity("Validation Errors.")
        }).when(templateConfigService).updateTemplateAuthConfig(any(Username.class) as Username, eq(templateConfig),
          any(), eq(result), eq("digest"))


        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/${templateName}/authorization"), headers, toObjectString({
          AuthorizationRepresenter.toJSON(it, authorizationRequest)
        }))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Validation Errors.")
          .hasJsonAttribute("data", toObjectWithoutLinks({ AuthorizationRepresenter.toJSON(it, authorizationRequest) }))
      }

      def authorizationHash =
      [
        admin: [
          roles: ['foo']
        ],
        view: [
          users: ['jez']
        ]
      ]
    }
  }
}
