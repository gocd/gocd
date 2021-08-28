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
package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.mocks.StubTemplateEngine
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import spark.ModelAndView

@MockitoSettings(strictness = Strictness.LENIENT)
class ArtifactStoresControllerTest implements ControllerTrait<ArtifactStoresController>, SecurityServiceTrait {

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath())
      }
    }

    @Test
    void 'should return Model and View'() {
      loginAsAdmin()
      def expectedBody = new StubTemplateEngine().render(new ModelAndView([viewTitle: "Artifact Stores"], null))

      get(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }
  }


  @Override
  ArtifactStoresController createControllerInstance() {
    return new ArtifactStoresController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine)
  }
}
