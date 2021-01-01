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
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested

import static com.thoughtworks.go.spark.Routes.PipelineConfig.SPA_AS_CODE
import static org.mockito.MockitoAnnotations.initMocks

class PipelinesAsCodeControllerTest implements ControllerTrait<PipelinesAsCodeController>, SecurityServiceTrait {

  @Nested
  class AsCode {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "asCode"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath(SPA_AS_CODE))
      }
    }
  }

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PipelinesAsCodeController createControllerInstance() {
    return new PipelinesAsCodeController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine)
  }
}
