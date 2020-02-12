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
package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.Pipeline
import com.thoughtworks.go.server.service.PipelineService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class CompareControllerTest implements ControllerTrait<CompareController>, SecurityServiceTrait {
  @Mock
  private PipelineService pipelineService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  CompareController createControllerInstance() {
    return new CompareController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine, pipelineService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {
      @BeforeEach
      void setUp() {
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(getPipelineName()))).thenReturn(true)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath("up42/3/with/4"))
      }

      @Override
      String getPipelineName() {
        return "up42"
      }
    }

    @Nested
    class AsAdminUser {
      @Test
      void 'should return view if counters are valid'() {
        Pipeline pipeline = mock(Pipeline.class);

        when(pipelineService.findPipelineByNameAndCounter(anyString(), anyInt())).thenReturn(pipeline)

        get(controller.controllerPath("up42/3/with/4"))

        assertThatResponse()
          .isOk()
      }

      @Test
      void 'should redirect if from counter is zero'() {
        get(controller.controllerPath("up42/0/with/4"))

        assertThatResponse()
          .redirectsTo("/go" + controller.controllerPath("up42/1/with/4"))
      }

      @Test
      void 'should redirect if to counter is zero'() {
        get(controller.controllerPath("up42/3/with/0"))

        assertThatResponse()
          .redirectsTo("/go" + controller.controllerPath("up42/3/with/1"))
      }

      @Test
      void 'should give 404 if from counter does not exist'() {
        Pipeline pipeline = mock(Pipeline.class);

        when(pipelineService.findPipelineByNameAndCounter(anyString(), eq(4))).thenReturn(pipeline)

        get(controller.controllerPath("up42/3/with/4"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Pipeline [up42/3] not found.")
      }

      @Test
      void 'should give 404 if to counter does not exist'() {
        Pipeline pipeline = mock(Pipeline.class);

        when(pipelineService.findPipelineByNameAndCounter(anyString(), eq(3))).thenReturn(pipeline)

        get(controller.controllerPath("up42/3/with/4"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Pipeline [up42/4] not found.")
      }
    }
  }
}
