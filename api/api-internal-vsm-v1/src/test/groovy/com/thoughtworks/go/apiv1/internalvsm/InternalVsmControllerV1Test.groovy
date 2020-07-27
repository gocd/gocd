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

package com.thoughtworks.go.apiv1.internalvsm

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalvsm.representers.VSMRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel
import com.thoughtworks.go.server.service.ValueStreamMapService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import java.util.stream.Stream

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalVsmControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalVsmControllerV1> {
  @Mock
  private ValueStreamMapService valueStreamMapService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalVsmControllerV1 createControllerInstance() {
    new InternalVsmControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), valueStreamMapService)
  }

  @Nested
  class Index {

    @BeforeEach
    void setUp() {
      when(goConfigService.hasPipelineNamed(any(CaseInsensitiveString.class))).thenReturn(true)
    }

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {
      @Override
      String getPipelineName() {
        return "up42"
      }

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader("/api/internal/value_stream_map/pipelines/up42/1")
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return 200 when asked for vsm for a pipeline instance'() {
        def vsm = new ValueStreamMapPresentationModel(null, null, [])
        when(valueStreamMapService.getValueStreamMap(any(CaseInsensitiveString.class), anyInt(), any(Username.class), any())).thenReturn(vsm)

        getWithApiHeader("/api/internal/value_stream_map/pipelines/up42/1")

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(VSMRepresenter.class, vsm)
      }

      @Test
      void 'should return error if the user does not have view access'() {
        when(valueStreamMapService.getValueStreamMap(any(CaseInsensitiveString.class), anyInt(), any(Username.class), any()))
          .then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.forbidden("user does not have access", HealthStateType.forbidden())
        })

        getWithApiHeader("/api/internal/value_stream_map/pipelines/some-pipeline/1")

        assertThatResponse()
          .isForbidden()
          .hasJsonMessage("user does not have access")
      }

      @ParameterizedTest
      @MethodSource("pipelineCounters")
      void 'should return 400 if the counter is incorrect'(String input) {
        getWithApiHeader("/api/internal/value_stream_map/pipelines/up42/" + input)

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'pipeline_counter' must be a number greater than 0.")
      }

      static Stream<Arguments> pipelineCounters() {
        return Stream.of(
          Arguments.of("-10"),
          Arguments.of("abc")
        )
      }
    }
  }

  @Nested
  class MaterialVsm {

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "materialsVsm"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader("/api/internal/value_stream_map/materials/fingerprint/revision")
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return 200 when asked for vsm for a material revision'() {
        def vsm = new ValueStreamMapPresentationModel(null, null, [])
        when(valueStreamMapService.getValueStreamMap(anyString(), anyString(), any(Username.class), any())).thenReturn(vsm)

        getWithApiHeader("/api/internal/value_stream_map/materials/fingerprint/rev")

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(VSMRepresenter.class, vsm)
      }

      @Test
      void 'should return error if the user does not have view access'() {
        when(valueStreamMapService.getValueStreamMap(anyString(), anyString(), any(Username.class), any()))
          .then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.forbidden("user does not have access", HealthStateType.forbidden())
        })

        getWithApiHeader("/api/internal/value_stream_map/materials/fingerprint/rev")

        assertThatResponse()
          .isForbidden()
          .hasJsonMessage("user does not have access")
      }
    }
  }
}
