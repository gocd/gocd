/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.mockito.MockitoAnnotations.initMocks

class RolesControllerDelegateTest implements ControllerTrait<RolesControllerDelegate>, SecurityServiceTrait {

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait {

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void "should disallow anonymous users, with security enabled"() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      void 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath())
      }
    }
  }

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  RolesControllerDelegate createControllerInstance() {
    return new RolesControllerDelegate(authenticationHelper, templateEngine)
  }
}
