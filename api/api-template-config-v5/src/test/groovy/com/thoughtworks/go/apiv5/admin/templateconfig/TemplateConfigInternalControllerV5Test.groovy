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
package com.thoughtworks.go.apiv5.admin.templateconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv5.admin.templateconfig.representers.TemplatesInternalRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.TemplateConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.TemplateViewUserSecurity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class TemplateConfigInternalControllerV5Test implements SecurityServiceTrait, ControllerTrait<TemplateConfigInternalControllerV5> {

  private PipelineTemplateConfig template

  @BeforeEach
  void setUp() {
    initMocks(this)
    template = new PipelineTemplateConfig(new CaseInsensitiveString('some-template'), new StageConfig(new CaseInsensitiveString('stage'), new JobConfigs(new JobConfig(new CaseInsensitiveString('job')))))
  }

  @Mock
  private TemplateConfigService templateConfigService

  @Override
  TemplateConfigInternalControllerV5 createControllerInstance() {
    return new TemplateConfigInternalControllerV5(templateConfigService, new ApiAuthenticationHelper(securityService, goConfigService))
  }

  @Nested
  class listTemplates {

    @Nested
    class Security implements SecurityTestTrait, TemplateViewUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "listTemplates"
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

        def templates = new TemplatesConfig();

        when(templateConfigService.templateConfigsThatCanBeViewedBy(any(Username.class) as Username)).thenReturn(templates)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(templates, TemplatesInternalRepresenter)
      }
    }
  }
}
