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

package com.thoughtworks.go.apiv1.elasticprofileoperation

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.domain.ElasticProfileUsage
import com.thoughtworks.go.server.service.ElasticProfileService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import groovy.json.JsonBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ElasticProfileOperationControllerV1Test implements SecurityServiceTrait, ControllerTrait<ElasticProfileOperationControllerV1> {

  @Mock
  private ElasticProfileService elasticProfileService

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Override
  ElasticProfileOperationControllerV1 createControllerInstance() {
    new ElasticProfileOperationControllerV1(elasticProfileService, new ApiAuthenticationHelper(securityService, goConfigService))
  }


  @Nested
  class Usages {
    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "usages"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("/docker/usages"))
      }
    }

    @Nested
    class AsAGroupAdmin {

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should list jobs associated with a profile id'() {
        def elasticProfileUsages = Arrays.asList(
          new ElasticProfileUsage("LinuxPR", "build", "compile", "linux-pr"),
          new ElasticProfileUsage("LinuxPR", "build", "test", "linux-pr"),

          new ElasticProfileUsage("WindowsPR", "clean", "clean-dirs"),
          new ElasticProfileUsage("WindowsPR", "clean", "clean-artifacts")
        )

        when(elasticProfileService.getUsageInformation("docker")).thenReturn(elasticProfileUsages)

        getWithApiHeader(controller.controllerPath("/docker/usages"))

        def expectedResponse = [
          [pipeline_name: "LinuxPR", stage_name: "build", job_name: "compile", template_name: "linux-pr"],
          [pipeline_name: "LinuxPR", stage_name: "build", job_name: "test", template_name: "linux-pr"],

          [pipeline_name: "WindowsPR", stage_name: "clean", job_name: "clean-dirs"],
          [pipeline_name: "WindowsPR", stage_name: "clean", job_name: "clean-artifacts"]
        ]

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJson(new JsonBuilder(expectedResponse).toString())
      }

      @Test
      void 'should return 404 when profile with id does not exist'() {
        when(elasticProfileService.getUsageInformation("docker")).thenThrow(new RecordNotFoundException("docker"))

        getWithApiHeader(controller.controllerPath("/docker/usages"))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Either the resource you requested was not found, or you are not authorized to perform this action.")
      }
    }
  }
}
