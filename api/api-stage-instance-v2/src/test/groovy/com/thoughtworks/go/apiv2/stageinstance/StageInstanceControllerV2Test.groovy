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

package com.thoughtworks.go.apiv2.stageinstance

import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach

import static org.mockito.MockitoAnnotations.initMocks

class StageInstanceControllerV2Test implements SecurityServiceTrait, ControllerTrait<StageInstanceControllerV2> {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  StageInstanceControllerV2 createControllerInstance() {
    new StageInstanceControllerV2(new ApiAuthenticationHelper(securityService, goConfigService))
  }
}
