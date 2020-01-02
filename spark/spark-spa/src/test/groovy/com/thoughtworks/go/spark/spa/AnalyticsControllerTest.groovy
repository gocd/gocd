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

import com.google.gson.Gson
import com.thoughtworks.go.api.mocks.MockHttpServletResponseAssert
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsData
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.spark.*
import com.thoughtworks.go.spark.mocks.StubTemplateEngine
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock
import spark.ModelAndView
import spark.Request
import spark.Response

import static java.lang.String.format
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class AnalyticsControllerTest implements ControllerTrait<AnalyticsController>, SecurityServiceTrait {
  private static final Gson GSON = new Gson()

  AnalyticsExtension analyticsExtension = mock(AnalyticsExtension.class)
  PipelineConfigService pipelineConfigService = mock(PipelineConfigService.class)

  @Override
  AnalyticsController createControllerInstance() {
    return new AnalyticsController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine, systemEnvironment, analyticsExtension, pipelineConfigService)
  }

  @Nested
  class Index {
    @Nested
    class Load implements PipelinesHelperTrait {
      @Test
      void "should provide pipeline config data"() {
        PipelineConfigs group1 = pipelinesFor("pipe1", "pipe2")
        PipelineConfigs group2 = pipelinesFor("pipe3", "pipe4")
        def pipelineGroups = new PipelineGroups(group1, group2)
        when(pipelineConfigService.viewableGroupsFor(currentUsername())).thenReturn(pipelineGroups)

        get(controller.controllerPath())

        String expectedBody = new StubTemplateEngine().render(
          new ModelAndView([
            viewTitle: "Analytics",
            pipelines: GSON.toJson(["pipe1", "pipe2", "pipe3", "pipe4"])
          ], "analytics/index.ftlh")
        )

        assertThatResponse()
          .isOk()
          .hasBody(expectedBody)
      }

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerBasePath())
      }
    }

  }

  @Nested
  class ShowAnalytics {
    @Nested
    class Fetch {
      @Test
      void "should return analytics data"() {
        AnalyticsData expected = new AnalyticsData(GSON.toJson([1, 2, 3]), "/path/to/template")
        when(analyticsExtension.getAnalytics("pluginId", "pipeline", "metric", Collections.singletonMap("pipeline_name", getPipelineName()))).thenReturn(expected)
        get(controller.controllerPath("pluginId", "pipeline", "metric") + "?pipeline_name=" + getPipelineName())

        assertThatResponse().isOk().hasJsonBody(expected.toMap())
      }

      @Test
      void "should fetch analytics on a post request"() {
        AnalyticsData expected = new AnalyticsData(GSON.toJson([1, 2, 3]), "/path/to/template")

        when(analyticsExtension.getAnalytics("pluginId", "vsm", "metric", Collections.emptyMap())).thenReturn(expected)

        post(controller.controllerPath("pluginId", "vsm", "metric"), Collections.emptyMap())

        assertThatResponse().isOk().hasJsonBody(expected.toMap())
      }

      String getPipelineName() {
        return "testPipeline"
      }

      @BeforeEach
      void setUp() {
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(getPipelineName()))).thenReturn(true)
        when(pipelineConfigService.pipelineConfigNamed(getPipelineName())).thenReturn(mock(PipelineConfig.class))
        enableSecurity()
        loginAsAdmin()
      }
    }

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {
      @Test
      void "should allow agent analytics for normal users"() {
        enableSecurity()
        loginAsUser()

        get(controller.controllerPath("plugin/agent/metric"))
        assertRequestAllowed()
      }

      @Test
      void "should disallow agent analytics for normal users when admin-only toggle is enabled"() {
        when(systemEnvironment.enableAnalyticsOnlyForAdmins()).thenReturn(true)

        enableSecurity()
        loginAsUser()

        get(controller.controllerPath("plugin/agent/metric"))
        assertRequestForbidden()
      }

      @Test
      void "should allow agent analytics for admins when admin-only toggle is enabled"() {
        when(systemEnvironment.enableAnalyticsOnlyForAdmins()).thenReturn(true)

        enableSecurity()
        loginAsAdmin()

        get(controller.controllerPath("plugin/agent/metric"))
        assertRequestAllowed()
      }

      @Test
      void "should disallow pipeline view users when admin-only toggle is enabled"() {
        when(systemEnvironment.enableAnalyticsOnlyForAdmins()).thenReturn(true)

        enableSecurity()
        loginAsPipelineViewUser(pipelineName)

        makeHttpCall()
        assertRequestForbidden()
      }

      @Test
      void "should only allow admins when admin-only toggle is enabled"() {
        when(systemEnvironment.enableAnalyticsOnlyForAdmins()).thenReturn(true)

        enableSecurity()
        loginAsAdmin()

        makeHttpCall()
        assertRequestAllowed()
      }

      @Test
      void "should allow all users to view VSM analytics"() {
        when(systemEnvironment.enableAnalyticsOnlyForAdmins()).thenReturn(false)

        enableSecurity()
        loginAsUser()

        get(controller.controllerPath("plugin/vsm/metric"))
        assertRequestAllowed()
      }

      @Test
      void "should allow all users to view drilldown analytics"() {
        when(systemEnvironment.enableAnalyticsOnlyForAdmins()).thenReturn(false)

        enableSecurity()
        loginAsUser()

        get(controller.controllerPath("plugin/drilldown/metric"))
        assertRequestAllowed()
      }

      @Test
      void "should return 404 when pipeline does not exist"() {
        when(pipelineConfigService.pipelineConfigNamed(getPipelineName())).thenReturn(null)
        enableSecurity()
        loginAsPipelineViewUser(pipelineName)

        makeHttpCall()
        assertRequestMissing()
      }

      @Override
      def assertRequestAllowed() {
        verify(controller)."${controllerMethodUnderTest}"(any(), any())

        ((MockHttpServletResponseAssert) assertThatResponse())
          .hasStatus(99999)
          .hasContentType("application/json")
          .hasBody("{\"data\": \"rendered ${this.reachedControllerMessage}\"}".toString())
      }

      void assertRequestMissing() {
        verify(controller, never())."${controllerMethodUnderTest}"(any(), any())

        ((MockHttpServletResponseAssert) assertThatResponse())
          .hasContentType("text/html")
          .hasStatus(404)
          .hasBody(format("Cannot generate analytics. Pipeline with name: '%s' not found.", pipelineName))
      }

      @Override
      void stubControllerAction() {
        this.reachedControllerMessage = UUID.randomUUID().toString()
        doAnswer({ InvocationOnMock invocation ->
          Response res = invocation.arguments.last()
          res.status(99999)
          res.type("application/json")
          return "{\"data\": \"rendered ${this.reachedControllerMessage}\"}".toString()
        }).when(controller)."${controllerMethodUnderTest}"(any() as Request, any() as Response)

      }

      @Override
      String getControllerMethodUnderTest() {
        return "showAnalytics"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath("pluginId", "pipeline", "metric") + "?pipeline_name=" + getPipelineName())
      }

      @Override
      String getPipelineName() {
        return "testPipeline"
      }

      @BeforeEach
      void setUp() {
        stubControllerAction()
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(getPipelineName()))).thenReturn(true)
        when(pipelineConfigService.pipelineConfigNamed(getPipelineName())).thenReturn(mock(PipelineConfig.class))
      }
    }
  }

  @BeforeEach
  void setUp() {
    initMocks(this)
  }
}
