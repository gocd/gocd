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

package com.thoughtworks.go.apiv1.configrepooperations

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.config.GoRepoConfigDataSource
import com.thoughtworks.go.config.PartialConfigParseResult
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.domain.materials.Material
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.MaterialConfigConverter
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ConfigRepoOperationsControllerV1Test implements SecurityServiceTrait, ControllerTrait<ConfigRepoOperationsControllerV1> {
  @Mock
  GoRepoConfigDataSource dataSource
  @Mock
  ConfigRepoService service
  @Mock
  MaterialUpdateService materialUpdateService
  @Mock
  MaterialConfigConverter converter

  private static final ID = "repo1"

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ConfigRepoOperationsControllerV1 createControllerInstance() {
    return new ConfigRepoOperationsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), dataSource, service, materialUpdateService, converter)
  }
}
