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

package com.thoughtworks.go.apiv1.feedsapi

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.server.service.FeedService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito

import static org.mockito.MockitoAnnotations.initMocks

class FeedsApiControllerV1Test implements SecurityServiceTrait, ControllerTrait<FeedsApiControllerV1> {
  @Mock
  private FeedService feedService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  FeedsApiControllerV1 createControllerInstance() {
    new FeedsApiControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), feedService)
  }

  @Nested
  class PipelinesXML {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "pipelinesXML"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(Routes.FeedsAPI.PIPELINES_XML))
      }
    }

    @Test
    void 'should call feed service to get pipelines xml'() {
      getWithApiHeader(controller.controllerPath(Routes.FeedsAPI.PIPELINES_XML))

      Mockito.verify(feedService).pipelinesXml(currentUsername(), "http://test.host/go")
      Mockito.verifyNoMoreInteractions(feedService)
    }
  }

  @Nested
  class PipelineXML {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "pipelineXML"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(Routes.FeedsAPI.PIPELINE_XML))
      }
    }

    @Test
    void 'should call feed service to get pipeline xml with pipeline name and id'() {
      getWithApiHeader(controller.controllerPath("pipelines", "up42", "101.xml"))

      Mockito.verify(feedService).pipelineXml(currentUsername(), "up42", 101L, "http://test.host/go")
      Mockito.verifyNoMoreInteractions(feedService)
    }
  }

  @Nested
  class StagesXML {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "stagesXML"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(Routes.FeedsAPI.STAGES_XML).replaceAll(":pipeline_name", "up42"))
      }
    }

    @Test
    void 'should call feed service to get stages xml'() {
      getWithApiHeader(controller.controllerPath(Routes.FeedsAPI.STAGES_XML).replaceAll(":pipeline_name", "up42"))

      Mockito.verify(feedService).stagesXml(currentUsername(), "up42", null, "http://test.host/go")
      Mockito.verifyNoMoreInteractions(feedService)
    }

    @Test
    void 'should call feed service to get stages xml before id'() {
      getWithApiHeader(controller.controllerPath(Routes.FeedsAPI.STAGES_XML.replaceAll(":pipeline_name", "up42") + "?before=100"))

      Mockito.verify(feedService).stagesXml(currentUsername(), "up42", 100L, "http://test.host/go")
      Mockito.verifyNoMoreInteractions(feedService)
    }
  }

  @Nested
  class StageXML {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "stageXML"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(Routes.FeedsAPI.STAGE_XML).replaceAll(":stage_id", "10"))
      }
    }

    @Test
    void 'should call feed service to get stage xml'() {
      getWithApiHeader(controller.controllerPath(Routes.FeedsAPI.STAGE_XML.replaceAll(":stage_id", "1.xml")))

      Mockito.verify(feedService).stageXml(1L, "http://test.host/go")
      Mockito.verifyNoMoreInteractions(feedService)
    }
  }
}
