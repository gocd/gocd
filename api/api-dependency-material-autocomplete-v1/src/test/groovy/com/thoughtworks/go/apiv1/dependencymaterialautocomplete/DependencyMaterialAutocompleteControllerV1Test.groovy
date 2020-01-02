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
package com.thoughtworks.go.apiv1.dependencymaterialautocomplete

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.dependencymaterialautocomplete.representers.SuggestionsRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.StageConfig
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class DependencyMaterialAutocompleteControllerV1Test implements SecurityServiceTrait, ControllerTrait<DependencyMaterialAutocompleteControllerV1> {

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Override
  DependencyMaterialAutocompleteControllerV1 createControllerInstance() {
    return new DependencyMaterialAutocompleteControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), goConfigService)
  }

  @Nested
  class Security implements SecurityTestTrait, GroupAdminUserSecurity {

    @Override
    String getControllerMethodUnderTest() {
      return "suggest"
    }

    @Override
    void makeHttpCall() {
      getWithApiHeader(controller.controllerBasePath())
    }
  }

  @Nested
  class Index {

    @Test
    void 'shows pipeline + stage suggestions'() {
      List<PipelineConfig> suggestions = [
        pipeline("pipe-a", stage("stage-a1"), stage("stage-a2")),
        pipeline("pipe-b", stage("stage-b1")),
      ]
      when(goConfigService.getAllLocalPipelineConfigs()).thenReturn(suggestions)

      getWithApiHeader(controller.controllerPath([fingerprint: 'foo', pipeline_name: 'some-pipeline', search_text: 'abc']))

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasBodyWithJsonArray(suggestions, SuggestionsRepresenter.class)
    }

    private CaseInsensitiveString ident(String name) {
      return new CaseInsensitiveString(name)
    }

    private PipelineConfig pipeline(String name, StageConfig... stages) {
      return new PipelineConfig(ident(name), null, stages)
    }

    private StageConfig stage(String name) {
      return new StageConfig(ident(name), null);
    }
  }
}
