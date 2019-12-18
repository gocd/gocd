/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv6.admin.templateconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv6.admin.templateconfig.representers.TemplateConfigRepresenter
import com.thoughtworks.go.apiv6.admin.templateconfig.representers.TemplatesConfigRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.TemplateConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class TemplateConfigControllerV6Test implements SecurityServiceTrait, ControllerTrait<TemplateConfigControllerV6> {

  private PipelineTemplateConfig template

  @BeforeEach
  void setUp() {
    initMocks(this)
    template = new PipelineTemplateConfig(new CaseInsensitiveString('some-template'), new StageConfig(new CaseInsensitiveString('stage'), new JobConfigs(new JobConfig(new CaseInsensitiveString('job')))))
  }

  @Mock
  private TemplateConfigService templateConfigService

  @Mock
  private EntityHashingService entityHashingService


  @Override
  TemplateConfigControllerV6 createControllerInstance() {
    return new TemplateConfigControllerV6(templateConfigService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, AnyAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsAdmin {

      @Test
      void 'should list all templates'() {
        enableSecurity()
        loginAsAdmin()

        def templates = new TemplateToPipelines(new CaseInsensitiveString("template"), true, true)
        templates.add(new PipelineEditabilityInfo(new CaseInsensitiveString("pipeline1"), true, true))
        templates.add(new PipelineEditabilityInfo(new CaseInsensitiveString("pipeline2"), false, true))

        when(templateConfigService.getTemplatesList(any(Username.class) as Username)).thenReturn([templates])

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject([templates], TemplatesConfigRepresenter)
      }
    }
  }

  @Nested
  class Show {

    @Nested
    class Security implements SecurityTestTrait, TemplateViewUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/t1'))
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
      void 'should render the template of specified name'() {
        when(entityHashingService.md5ForEntity(any(PipelineTemplateConfig) as PipelineTemplateConfig)).thenReturn('md5')
        when(templateConfigService.loadForView('template', result)).thenReturn(template)

        getWithApiHeader(controller.controllerPath("/template"))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(template, TemplateConfigRepresenter)

      }

      @Test
      void "should return 304 for show pipeline config if etag sent in request is fresh"() {

        when(entityHashingService.md5ForEntity(any(PipelineTemplateConfig) as PipelineTemplateConfig)).thenReturn('md5_for_template_config')
        when(templateConfigService.loadForView("template", result)).thenReturn(template)

        getWithApiHeader(controller.controllerPath('/template'), ['if-none-match': '"md5_for_template_config"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 404 if the template does not exist'() {
        when(templateConfigService.loadForView('non-existent-template', result)).thenReturn(null)

        getWithApiHeader(controller.controllerPath("/non-existent-template"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("non-existent-template"))
          .hasContentType(controller.mimeType)

      }
    }
  }

  @Nested
  class Destroy {

    private HttpLocalizedOperationResult result

    @Nested
    class Security implements SecurityTestTrait, TemplateAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath('/t1'))
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should raise an error if template is not found'() {
        when(templateConfigService.loadForView("foo", result)).thenReturn(null)

        deleteWithApiHeader(controller.controllerPath("/foo"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("foo"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render the success message on deleting a template'() {
        when(templateConfigService.loadForView("some-template", result)).thenReturn(template)
        doAnswer({ InvocationOnMock invocation ->
          result = invocation.arguments.last()
          result.setMessage("The template 'some-template' was deleted successfully.")
        }).when(templateConfigService).deleteTemplateConfig(any(Username.class) as Username, eq(template), eq(result))

        deleteWithApiHeader(controller.controllerPath("/some-template"))

        assertThatResponse()
          .isOk()
          .hasJsonMessage("The template 'some-template' was deleted successfully.")
      }

      @Test
      void 'should render the validation errors on failure to delete'() {
        when(templateConfigService.loadForView("some-template", result)).thenReturn(template)

        doAnswer({ InvocationOnMock invocation ->
          result = invocation.arguments.last()
          result.unprocessableEntity("Save failed. Validation failed.")
        }).when(templateConfigService).deleteTemplateConfig(any(Username.class) as Username, eq(template), eq(result))

        deleteWithApiHeader(controller.controllerPath("/some-template"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Save failed. Validation failed.")
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [])
      }
    }

    @Nested
    class AsAdmin {

      private HttpLocalizedOperationResult result;

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should deserialize template from given parameters'() {
        doNothing().when(templateConfigService).createTemplateConfig(any(Username.class) as Username, any(PipelineTemplateConfig.class) as PipelineTemplateConfig, eq(result) )

        postWithApiHeader(controller.controllerPath(), templateHash)

        assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(template, TemplateConfigRepresenter)
      }

      @Test
      void 'should fail to save if there are validation errors'() {
        doAnswer({ InvocationOnMock invocation ->
          result = invocation.arguments.last()
          result.unprocessableEntity("Save failed.")
        }).when(templateConfigService).createTemplateConfig(any(Username.class) as Username, eq(template), eq(result))

        postWithApiHeader(controller.controllerPath(), templateHash)

        assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Save failed.")
      }
    }
  }

  @Nested
  class Update {

    @Nested
    class Security implements SecurityTestTrait, TemplateAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath('/foo'), [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-md5',
          'content-type': 'application/json'
        ], toObjectString({ TemplateConfigRepresenter.toJSON(it, template) }))
      }
    }

    @Nested
    class AsAdmin {

      private HttpLocalizedOperationResult result;

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should deserialize template from given parameters'() {
        when(templateConfigService.loadForView("some-template", result)).thenReturn(template)
        when(entityHashingService.md5ForEntity(template)).thenReturn("md5")

        doNothing().when(templateConfigService).updateTemplateConfig(any(Username.class) as Username, any(PipelineTemplateConfig.class) as PipelineTemplateConfig, eq(result), eq("md5"))

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'md5',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/some-template"), headers, templateHash)

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(template, TemplateConfigRepresenter)
      }

      @Test
      void 'should not allow rename of template name'() {
        when(templateConfigService.loadForView('some-template', result)).thenReturn(template)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'md5',
          'content-type': 'application/json'
        ]


        putWithApiHeader(controller.controllerPath("/some-template"), headers, [
          name: 'some-other-template',
          stages: [
            [
              name: 'stage',
              jobs: [
                [
                  name: 'job'
                ]
              ]
            ]
          ]
        ])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Renaming of templates is not supported by this API.")
      }


      @Test
      void 'should fail update if etag does not match' () {
        when(templateConfigService.loadForView("some-template", result)).thenReturn(template)

        when(entityHashingService.md5ForEntity(any(PipelineTemplateConfig.class) as PipelineTemplateConfig)).thenReturn("another-etag")

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'md5',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/some-template"), headers, templateHash)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for template 'some-template'. Please update your copy of the config with the changes and try again.")
      }

      @Test
      void 'should not update existing material if validations fail' () {
        when(templateConfigService.loadForView("some-template", result)).thenReturn(template)

        when(entityHashingService.md5ForEntity(any(PipelineTemplateConfig.class) as PipelineTemplateConfig)).thenReturn("md5")

        doAnswer({ InvocationOnMock invocation ->
          result = invocation.arguments[2]
          result.unprocessableEntity("some error")
        }).when(templateConfigService).updateTemplateConfig(any(Username.class) as Username, eq(template), eq(result), eq("md5"))

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'md5',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/some-template"), headers, templateHash)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("some error")
      }
    }
  }

  def templateHash =
  [
    name: 'some-template',
    stages: [
      [
        name: 'stage',
        jobs: [
          [
            name: 'job'
          ]
        ]
      ]
    ]
  ]

}
