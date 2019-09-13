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

import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.server.service.support.toggle.Toggles
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AgentsControllerTest implements ControllerTrait<AgentsController>, SecurityServiceTrait {
  @Mock
  private FeatureToggleService featureToggleService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  AgentsController createControllerInstance() {
    return new AgentsController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine, securityService, systemEnvironment, featureToggleService)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath())
      }
    }

    @Nested
    class AsValidUser {
      @Test
      void 'should add meta to the model if toggle is on'() {
        when(featureToggleService.isToggleOn(Toggles.SHOW_NEW_AGENTS_SPA)).thenReturn(true)

        get("/agents")

        assertThatResponse()
          .hasBodyContaining("{\"meta\":{\"data-should-show-analytics-icon\":true},\"viewTitle\":\"Agents\"}")
      }

      @Test
      void 'should not add meta but other values to the model if toggle is off'() {
        when(featureToggleService.isToggleOn(Toggles.SHOW_NEW_AGENTS_SPA)).thenReturn(false)

        get("/agents")

        println response.contentAsString

        assertThatResponse()
          .hasBodyContaining("{\"shouldShowAnalyticsIcon\":true,\"isUserAnAdmin\":false,\"viewTitle\":\"Agents\"}")
      }
    }
  }
}
