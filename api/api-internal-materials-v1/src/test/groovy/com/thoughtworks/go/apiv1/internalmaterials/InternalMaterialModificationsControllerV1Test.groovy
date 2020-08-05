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

package com.thoughtworks.go.apiv1.internalmaterials

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalmaterials.representers.ModificationsRepresenter
import com.thoughtworks.go.domain.PipelineRunIdInfo
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.domain.materials.Modifications
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.server.service.MaterialConfigService
import com.thoughtworks.go.server.service.MaterialService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.server.service.result.OperationResult
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
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
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class InternalMaterialModificationsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalMaterialModificationsControllerV1> {
  @Mock
  private MaterialConfigService materialConfigService
  @Mock
  private MaterialService materialService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalMaterialModificationsControllerV1 createControllerInstance() {
    return new InternalMaterialModificationsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), materialConfigService, materialService)
  }

  @Nested
  class MaterialModifications {

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "modifications"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @Nested
    class AsAuthorizedUser {
      def info = new PipelineRunIdInfo(10, 1)

      @BeforeEach
      void setUp() {
        loginAsUser()
        when(materialService.getLatestAndOldestModification(any(MaterialConfig.class), anyString())).thenReturn(info)
      }

      @Test
      void 'should return 200 with modifications'() {
        def git = MaterialConfigsMother.git("http://example.com")
        def modifications = new Modifications(ModificationsMother.withModifiedFileWhoseNameLengthIsOneK())

        when(materialConfigService.getMaterialConfig(anyString(), anyString(), any(OperationResult.class))).thenReturn(git)
        when(materialService.getModificationsFor(any(MaterialConfig.class), anyLong(), anyLong(), anyInt())).thenReturn(modifications)

        getWithApiHeader("/api/internal/materials/abc123/modifications")

        verify(materialService, never()).findMatchingModifications(any(MaterialConfig.class), anyString(), anyLong(), anyLong(), anyInt())
        verify(materialService).getModificationsFor(eq(git), eq(0L), eq(0L), eq(10))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(ModificationsRepresenter.class, modifications, info, git.getFingerprint())
      }

      @Test
      void 'should return 404 value if material does not exist'() {
        when(materialConfigService.getMaterialConfig(anyString(), anyString(), any(OperationResult.class))).then({
          InvocationOnMock invocation ->
            HttpOperationResult result = (HttpOperationResult) invocation.arguments.last()
            result.notFound("Material not found", "Some message", HealthStateType.notFound())
        })

        getWithApiHeader("/api/internal/materials/abc123/modifications")

        verifyNoInteractions(materialService)

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Material not found { Some message }")
      }

      @Test
      void 'should render history after the specified cursor'() {
        def git = MaterialConfigsMother.git("http://example.com")
        def modifications = new Modifications(ModificationsMother.withModifiedFileWhoseNameLengthIsOneK())

        when(materialConfigService.getMaterialConfig(anyString(), anyString(), any(OperationResult.class))).thenReturn(git)
        when(materialService.getModificationsFor(any(MaterialConfig.class), anyLong(), anyLong(), anyInt())).thenReturn(modifications)

        getWithApiHeader("/api/internal/materials/abc123/modifications?after=3")

        verify(materialService).getModificationsFor(eq(git), eq(3L), eq(0L), eq(10))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(ModificationsRepresenter.class, modifications, info, git.getFingerprint())
      }

      @Test
      void 'should render history before the specified cursor'() {
        def git = MaterialConfigsMother.git("http://example.com")
        def modifications = new Modifications(ModificationsMother.withModifiedFileWhoseNameLengthIsOneK())

        when(materialConfigService.getMaterialConfig(anyString(), anyString(), any(OperationResult.class))).thenReturn(git)
        when(materialService.getModificationsFor(any(MaterialConfig.class), anyLong(), anyLong(), anyInt())).thenReturn(modifications)

        getWithApiHeader("/api/internal/materials/abc123/modifications?before=3")

        verify(materialService).getModificationsFor(eq(git), eq(0L), eq(3L), eq(10))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(ModificationsRepresenter.class, modifications, info, git.getFingerprint())
      }

      @Test
      void 'should throw if the after cursor is specified as a invalid integer'() {
        getWithApiHeader("/api/internal/materials/abc123/modifications?after=abc")

        verifyNoInteractions(materialConfigService)
        verifyNoInteractions(materialService)

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter 'after', if specified, must be a positive integer.")
      }

      @Test
      void 'should throw if the before cursor is specified as a invalid integer'() {
        getWithApiHeader("/api/internal/materials/abc123/modifications?before=abc")

        verifyNoInteractions(materialConfigService)
        verifyNoInteractions(materialService)

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter 'before', if specified, must be a positive integer.")
      }

      @ParameterizedTest
      @MethodSource("pageSizes")
      void 'should throw error if page_size is not between 10 and 100'(String input) {
        getWithApiHeader("/api/internal/materials/abc123/modifications?page_size=" + input)

        verifyNoInteractions(materialConfigService)
        verifyNoInteractions(materialService)
        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter 'page_size', if specified must be a number between 10 and 100.")
      }

      @Test
      void 'should return matching modifications if pattern is supplied'() {
        def git = MaterialConfigsMother.git("http://example.com")
        def modifications = new Modifications(ModificationsMother.withModifiedFileWhoseNameLengthIsOneK())

        when(materialConfigService.getMaterialConfig(anyString(), anyString(), any(OperationResult.class))).thenReturn(git)
        when(materialService.findMatchingModifications(any(MaterialConfig.class), anyString(), anyLong(), anyLong(), anyInt())).thenReturn(modifications)

        getWithApiHeader("/api/internal/materials/abc123/modifications?pattern=hello")

        verify(materialService).findMatchingModifications(eq(git), eq("hello"), eq(0L), eq(0L), eq(10))
        verify(materialService, never()).getModificationsFor(eq(git), eq(3L), eq(0L), eq(10))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(ModificationsRepresenter.class, modifications, info, git.getFingerprint())
      }

      static Stream<Arguments> pageSizes() {
        return Stream.of(
          Arguments.of("7"),
          Arguments.of("107"),
          Arguments.of("-10"),
          Arguments.of("abc")
        )
      }
    }
  }
}
