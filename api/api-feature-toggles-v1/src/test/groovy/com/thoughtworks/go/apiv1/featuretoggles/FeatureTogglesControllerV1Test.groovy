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
package com.thoughtworks.go.apiv1.featuretoggles

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.featuretoggles.representers.FeatureTogglesRepresenter
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class FeatureTogglesControllerV1Test implements SecurityServiceTrait, ControllerTrait<FeatureTogglesControllerV1> {

  @Mock
  FeatureToggleService featureToggleService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  FeatureTogglesControllerV1 createControllerInstance() {
    new FeatureTogglesControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), featureToggleService)
  }

  @Nested
  class Index {
    @BeforeEach
    void setUp() {
      loginAsAdmin()
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @Test
    void 'should return feature toggles'() {
      def featureToggles = new FeatureToggles(new FeatureToggle("key", "description", true))

      when(featureToggleService.allToggles()).thenReturn(featureToggles)

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse().hasBodyWithJsonArray(FeatureTogglesRepresenter, featureToggles)
    }
  }


  @Nested
  class Update {
    @BeforeEach
    void setUp() {
      loginAsAdmin()
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath("/foo"), [:])
      }
    }

    @Test
    void 'should allow turning on a feature toggle'() {
      putWithApiHeader(controller.controllerPath('/key'), [:], [toggle_value: 'on'])

      verify(featureToggleService).changeValueOfToggle('key', true)
      assertThatResponse()
        .isOk()
        .hasJsonMessage("success")
    }

    @Test
    void 'should allow turning off a feature toggle'() {
      putWithApiHeader(controller.controllerPath('/key'), [:], [toggle_value: 'off'])

      verify(featureToggleService).changeValueOfToggle('key', false)
      assertThatResponse()
        .isOk()
        .hasJsonMessage("success")
    }

    @Test
    void 'should allow turning on a feature toggle with case insensitivity'() {
      putWithApiHeader(controller.controllerPath('/key'), [:], [toggle_value: 'On'])

      verify(featureToggleService).changeValueOfToggle('key', true)
      assertThatResponse()
        .isOk()
        .hasJsonMessage("success")
    }

    @Test
    void 'should allow turning off a feature toggle with case insensitivity'() {
      putWithApiHeader(controller.controllerPath('/key'), [:], [toggle_value: 'OFF'])

      verify(featureToggleService).changeValueOfToggle('key', false)
      assertThatResponse()
        .isOk()
        .hasJsonMessage("success")
    }

    @Test
    void 'should fail to update when no toggle value is specified'() {
      putWithApiHeader(controller.controllerPath('/key'), [:], [:])

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Json `{}` does not contain property 'toggle_value'")
    }

    @Test
    void 'should fail to update when invalid toggle value is specified'() {
      putWithApiHeader(controller.controllerPath('/key'), [:], [toggle_value: 'boom'])

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Your request could not be processed. Value of property \\\"toggle_value\\\" is invalid. Valid values are: \\\"on\\\" and \\\"off\\\".")
    }

    @Test
    void 'should render any errors occured during updating feature toggle value'() {
      when(featureToggleService.changeValueOfToggle('key', false)).thenThrow(new RecordNotFoundException("Boom!"))

      putWithApiHeader(controller.controllerPath('/key'), [:], [toggle_value: 'OFF'])

      assertThatResponse()
        .isNotFound()
        .hasJsonMessage("Boom!")
    }
  }
}
