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

package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested

import static org.mockito.Mockito.when

class CompareControllerTest implements ControllerTrait<CompareController>, SecurityServiceTrait {
  @Override
  CompareController createControllerInstance() {
    return new CompareController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {
      @BeforeEach
      void setUp() {
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(getPipelineName()))).thenReturn(true)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath("up42/3/with/4"))
      }

      @Override
      String getPipelineName() {
        return "up42"
      }
    }
  }
}
