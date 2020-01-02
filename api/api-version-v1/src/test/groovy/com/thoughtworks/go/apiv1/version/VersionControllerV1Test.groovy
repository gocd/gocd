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
package com.thoughtworks.go.apiv1.version

import com.thoughtworks.go.CurrentGoCDVersion
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.mockito.MockitoAnnotations.initMocks

class VersionControllerV1Test implements SecurityServiceTrait, ControllerTrait<VersionControllerV1> {

  private CurrentGoCDVersion currentGoCDVersion

  @Override
  VersionControllerV1 createControllerInstance() {
    return new VersionControllerV1()
  }

  @BeforeEach
  void setUp() {
    initMocks(this)
    currentGoCDVersion = new CurrentGoCDVersion()
  }

  @Nested
  class Show {

    @Test
    void 'should render the current goCD version'() {
      getWithApiHeader(controller.controllerPath())

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(currentGoCDVersion, VersionRepresenter)

    }

  }
}
