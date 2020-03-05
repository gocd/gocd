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

import com.thoughtworks.go.CurrentGoCDVersion
import com.thoughtworks.go.server.service.ArtifactsDirHolder
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ServerInfoControllerTest implements ControllerTrait<ServerInfoController>, SecurityServiceTrait {
  @Mock
  ArtifactsDirHolder artifactsDirHolder

  @Mock
  PipelineConfigService pipelineConfigService

  @Override
  ServerInfoController createControllerInstance() {
    return new ServerInfoController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine, artifactsDirHolder, pipelineConfigService)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath())
      }
    }
  }

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Test
  void 'should set appropriate meta information on view model'() {
    def jvmVersion = System.getProperty("java.version")
    def osInfo = System.getProperty("os.name") + " " + System.getProperty("os.version")
    def pipelineCount = 200
    def artifactsDir = new File("/tmp/foo")
    when(artifactsDirHolder.getArtifactsDir()).thenReturn(artifactsDir)
    when(pipelineConfigService.totalPipelinesCount()).thenReturn(pipelineCount)

    def res = createControllerInstance().index(null, null)
    Map<String, Object> actual = ((Map<Object, Object>) res.model).get("meta")

    assertThat(actual).isEqualTo([
      "go_server_version"                   : CurrentGoCDVersion.getInstance().formatted(),
      "jvm_version"                         : jvmVersion,
      "os_information"                      : osInfo,
      "usable_space_in_artifacts_repository": artifactsDir.getUsableSpace(),
      "pipeline_count"                      : pipelineCount
    ])
  }
}

