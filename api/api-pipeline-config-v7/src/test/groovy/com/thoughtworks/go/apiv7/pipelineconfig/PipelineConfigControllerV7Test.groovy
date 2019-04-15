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

package com.thoughtworks.go.apiv7.pipelineconfig

import com.thoughtworks.go.apiv7.admin.pipelineconfig.PipelineConfigControllerV7
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PipelineConfigControllerV7Test implements SecurityServiceTrait, ControllerTrait<PipelineConfigControllerV7> {

  @Override
  PipelineConfigControllerV7 createControllerInstance() {
    new PipelineConfigControllerV7()
  }

  @Nested
  class Index {

    @Test
    void 'test a request'() {
    }

  }
}
