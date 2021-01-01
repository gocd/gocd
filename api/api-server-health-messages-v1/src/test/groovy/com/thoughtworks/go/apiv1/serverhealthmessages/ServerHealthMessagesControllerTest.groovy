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
package com.thoughtworks.go.apiv1.serverhealthmessages

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.serverhealthmessages.representers.ServerHealthMessagesRepresenter
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.serverhealth.ServerHealthService
import com.thoughtworks.go.serverhealth.ServerHealthState
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString

class ServerHealthMessagesControllerTest implements SecurityServiceTrait, ControllerTrait<ServerHealthMessagesController> {

  ServerHealthService serverHealthService = new ServerHealthService()

  @Override
  ServerHealthMessagesController createControllerInstance() {
    return new ServerHealthMessagesController(serverHealthService, new ApiAuthenticationHelper(securityService, goConfigService))
  }

  @Nested
  class Show {
    @Nested
    class Security implements NormalUserSecurity, SecurityTestTrait {

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(Routes.ServerHealthMessages.BASE)
      }
    }

    @Nested
    class AsNormalUser {
      @Test
      void 'should render server health messages'() {
        def state = ServerHealthState.error("not enough disk space, halting scheduling", "There is not enough disk space on the artifact filesystem", HealthStateType.artifactsDiskFull())
        serverHealthService.update(state)

        getWithApiHeader(Routes.ServerHealthMessages.BASE)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonArray([state], ServerHealthMessagesRepresenter)
      }

      @Test
      void 'should render 304 if content matches'() {
        def state = ServerHealthState.error("not enough disk space, halting scheduling", "There is not enough disk space on the artifact filesystem", HealthStateType.artifactsDiskFull())
        serverHealthService.update(state)

        def etag = '"' + controller.etagFor(toArrayString({
          ServerHealthMessagesRepresenter.toJSON(it, [state])
        })) + '"'

        getWithApiHeader(Routes.ServerHealthMessages.BASE, ['if-none-match': etag])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
          .hasNoBody()
      }
    }
  }
}
