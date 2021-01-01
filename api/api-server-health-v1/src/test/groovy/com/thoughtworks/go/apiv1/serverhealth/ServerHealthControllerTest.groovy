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
package com.thoughtworks.go.apiv1.serverhealth

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.spark.AllowAllUsersSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.mockito.MockitoAnnotations.initMocks

class ServerHealthControllerTest implements ControllerTrait<ServerHealthController>, SecurityServiceTrait {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ServerHealthController createControllerInstance() {
    return new ServerHealthController()
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, AllowAllUsersSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath())
      }
    }

    @Test
    void 'should render JSON response'() {
      get(Routes.ServerHealth.BASE)

      assertThatResponse()
        .isOk()
        .hasJsonBody(["health": "OK"])
        .hasContentType(controller.mimeType)
    }
  }
}
