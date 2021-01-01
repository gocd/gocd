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


import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock

class LogoutPageControllerTest implements ControllerTrait<LogoutPageController>, SecurityServiceTrait {

  @Override
  LogoutPageController createControllerInstance() {
    return new LogoutPageController(templateEngine, mock(LoginLogoutHelper.class))
  }

  @Nested
  class PerformLogout {
    @Test
    void shouldInvalidateSessionAndCreateANewOne() {
      get("/auth/logout")

      assertThat(session).isNotSameAs(request.getSession(false))
      assertThat(Collections.list(request.getSession().getAttributeNames())).isEmpty()
    }
  }
}
